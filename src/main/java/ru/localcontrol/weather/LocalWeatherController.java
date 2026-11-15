
package ru.localcontrol.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import ru.localcontrol.LocalControlClient;
import ru.localcontrol.config.ConfigManager;

import java.lang.reflect.Method;

public class LocalWeatherController {

    private static final int TRANSITION_TICKS = 40;

    private final ConfigManager.ModConfig config;

    private float currentRain = 0.0F;
    private float currentThunder = 0.0F;

    private float startRain = 0.0F;
    private float startThunder = 0.0F;

    private float targetRain = 0.0F;
    private float targetThunder = 0.0F;

    private float serverRain = 0.0F;
    private float serverThunder = 0.0F;

    private int transitionTick = 0;

    private boolean initialized = false;
    private boolean transitioning = false;
    private boolean returningToServer = false;

    private boolean applyingOverride = false;

    private boolean serverRainReceived = false;
    private boolean serverThunderReceived = false;

    public LocalWeatherController(ConfigManager.ModConfig config) {
        this.config = config;
    }

    public void syncFromConfig() {
        config.rainIntensity = Math.max(
                0.0F,
                Math.min(1.0F, config.rainIntensity)
        );

        config.weatherVisualDuration = Math.max(
                1.0F,
                config.weatherVisualDuration
        );

        if (config.weatherPreset == null) {
            config.weatherPreset = "CLEAR";
        }

        config.weatherPreset = config.weatherPreset.toUpperCase();
    }

    /**
     * Сохраняет настоящее серверное значение дождя.
     */
    public void recordServerRain(float value) {
        if (applyingOverride) {
            return;
        }

        serverRain = clamp(value);
        serverRainReceived = true;
    }

    /**
     * Сохраняет настоящее серверное значение грозы.
     */
    public void recordServerThunder(float value) {
        if (applyingOverride) {
            return;
        }

        serverThunder = clamp(value);
        serverThunderReceived = true;
    }

    public boolean isApplyingOverride() {
        return applyingOverride;
    }

    public boolean isLocalWeatherEnabled() {
        return config.localWeatherEnabled;
    }

    public void tick(ClientLevel world) {
        if (world == null || !config.localWeatherEnabled) {
            return;
        }

        syncFromConfig();

        if (!initialized) {
            currentRain = clamp(world.getRainLevel(1.0F));
            currentThunder = clamp(world.getThunderLevel(1.0F));

            if (!serverRainReceived) {
                serverRain = currentRain;
            }

            if (!serverThunderReceived) {
                serverThunder = currentThunder;
            }

            initialized = true;

            startRain = currentRain;
            startThunder = currentThunder;

            if (!returningToServer) {
                setLocalTarget();
            }
        }

        if (returningToServer) {
            tickReturnToServer(world);
            return;
        }

        float desiredRain;
        float desiredThunder;

        switch (config.weatherPreset) {
            case "CLEAR" -> {
                desiredRain = 0.0F;
                desiredThunder = 0.0F;
            }

            case "RAIN" -> {
                desiredRain = config.rainIntensity;
                desiredThunder = 0.0F;
            }

            case "THUNDER" -> {
                desiredRain = 1.0F;
                desiredThunder = config.rainIntensity;
            }

            default -> {
                desiredRain = 0.0F;
                desiredThunder = 0.0F;
            }
        }

        if (Float.compare(desiredRain, targetRain) != 0
                || Float.compare(desiredThunder, targetThunder) != 0) {

            startRain = currentRain;
            startThunder = currentThunder;

            targetRain = desiredRain;
            targetThunder = desiredThunder;

            transitionTick = 0;
            transitioning = true;
        }

        if (transitioning) {
            transitionTick++;

            float progress = Math.min(
                    1.0F,
                    (float) transitionTick / TRANSITION_TICKS
            );

            float eased = smoothStep(progress);

            currentRain = interpolate(
                    startRain,
                    targetRain,
                    eased
            );

            currentThunder = interpolate(
                    startThunder,
                    targetThunder,
                    eased
            );

            if (transitionTick >= TRANSITION_TICKS) {
                currentRain = targetRain;
                currentThunder = targetThunder;

                transitioning = false;
                transitionTick = 0;
            }
        }

        applyWeather(world, currentRain, currentThunder);
    }

    private void setLocalTarget() {
        switch (config.weatherPreset) {
            case "RAIN" -> {
                targetRain = config.rainIntensity;
                targetThunder = 0.0F;
            }

            case "THUNDER" -> {
                targetRain = 1.0F;
                targetThunder = config.rainIntensity;
            }

            default -> {
                targetRain = 0.0F;
                targetThunder = 0.0F;
            }
        }

        transitioning = true;
        transitionTick = 0;
    }

    private void tickReturnToServer(ClientLevel world) {
        if (serverRainReceived) {
            targetRain = serverRain;
        }

        if (serverThunderReceived) {
            targetThunder = serverThunder;
        }

        transitionTick++;

        float progress = Math.min(
                1.0F,
                (float) transitionTick / TRANSITION_TICKS
        );

        float eased = smoothStep(progress);

        currentRain = interpolate(
                startRain,
                targetRain,
                eased
        );

        currentThunder = interpolate(
                startThunder,
                targetThunder,
                eased
        );

        applyWeather(world, currentRain, currentThunder);

        if (transitionTick >= TRANSITION_TICKS) {
            finishReturnToServer(world);
        }
    }

    /**
     * Начинает плавное возвращение к серверной погоде.
     */
    public void disableLocalWeather(ClientLevel world) {
        if (returningToServer || !config.localWeatherEnabled) {
            return;
        }

        if (world != null && !initialized) {
            currentRain = clamp(world.getRainLevel(1.0F));
            currentThunder = clamp(world.getThunderLevel(1.0F));

            if (!serverRainReceived) {
                serverRain = currentRain;
            }

            if (!serverThunderReceived) {
                serverThunder = currentThunder;
            }

            initialized = true;
        }

        startRain = currentRain;
        startThunder = currentThunder;

        targetRain = serverRainReceived
                ? serverRain
                : currentRain;

        targetThunder = serverThunderReceived
                ? serverThunder
                : currentThunder;

        transitionTick = 0;
        transitioning = true;
        returningToServer = true;

        // Контроллер продолжает работать до окончания перехода.
        config.localWeatherEnabled = true;
    }

    /**
     * Используется после сброса настроек.
     */
    public void continueReturnAfterReset() {
        if (returningToServer && transitioning) {
            config.localWeatherEnabled = true;
        }
    }

    private void finishReturnToServer(ClientLevel world) {
        currentRain = serverRainReceived
                ? serverRain
                : targetRain;

        currentThunder = serverThunderReceived
                ? serverThunder
                : targetThunder;

        applyWeather(world, currentRain, currentThunder);

        transitioning = false;
        returningToServer = false;
        initialized = false;
        transitionTick = 0;

        config.localWeatherEnabled = false;

        saveConfig();
    }

    private void applyWeather(
            ClientLevel world,
            float rain,
            float thunder
    ) {
        applyingOverride = true;

        try {
            invoke(world, "setRainLevel", clamp(rain));
            invoke(world, "setThunderLevel", clamp(thunder));
        } finally {
            applyingOverride = false;
        }
    }

    public void setPreset(String preset) {
        if (preset == null || preset.isBlank()) {
            preset = "CLEAR";
        }

        config.weatherPreset = preset.toUpperCase();

        if (config.weatherPreset.equals("RAIN")
                && config.rainIntensity <= 0.0F) {
            config.rainIntensity = 1.0F;
        }

        config.localWeatherEnabled = true;

        returningToServer = false;
        initialized = false;
        transitioning = false;
        transitionTick = 0;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public boolean isReturningToServer() {
        return returningToServer;
    }

    private void saveConfig() {
        if (LocalControlClient.INSTANCE != null) {
            LocalControlClient.INSTANCE.configManager().save();
        }
    }

    private static float interpolate(
            float start,
            float end,
            float progress
    ) {
        return start + (end - start) * progress;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }

        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static boolean invoke(
            Object target,
            String methodName,
            Object... args
    ) {
        if (target == null) {
            return false;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] types = method.getParameterTypes();

            if (types.length != args.length) {
                continue;
            }

            boolean compatible = true;

            for (int i = 0; i < types.length; i++) {
                if (types[i] == float.class
                        || types[i] == Float.class) {

                    if (!(args[i] instanceof Number)) {
                        compatible = false;
                        break;
                    }

                } else if (args[i] == null
                        || !types[i].isAssignableFrom(args[i].getClass())) {

                    compatible = false;
                    break;
                }
            }

            if (!compatible) {
                continue;
            }

            try {
                method.invoke(target, args);
                return true;
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
                return false;
            }
        }

        System.err.println(
                "[LocalControl] Method not found: " + methodName
        );

        return false;
    }
}
