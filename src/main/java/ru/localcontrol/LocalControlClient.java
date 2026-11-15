
package ru.localcontrol;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import ru.localcontrol.config.ConfigManager;
import ru.localcontrol.gui.LocalControlScreen;
import ru.localcontrol.input.KeyBindings;
import ru.localcontrol.time.LocalTimeController;
import ru.localcontrol.weather.LocalWeatherController;

public class LocalControlClient implements ClientModInitializer {

    public static final String MOD_ID = "localcontrol";
    public static LocalControlClient INSTANCE;

    private final ConfigManager configManager = new ConfigManager();

    private final LocalTimeController timeController =
            new LocalTimeController(configManager.getConfig());

    private final LocalWeatherController weatherController =
            new LocalWeatherController(configManager.getConfig());

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        configManager.load();

        timeController.syncFromConfig();
        weatherController.syncFromConfig();

        KeyBindings.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            configManager.load();

            timeController.syncFromConfig();
            weatherController.syncFromConfig();
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public void onClientTick(Minecraft client) {
        if (client == null || client.level == null) {
            return;
        }

        if (configManager.getConfig().localTimeEnabled) {
            timeController.tick(client.level);
        }

        if (configManager.getConfig().localWeatherEnabled) {
            weatherController.tick(client.level);
        }
    }

    public void openMenu() {
        Minecraft client = Minecraft.getInstance();

        if (client.screen instanceof LocalControlScreen screen) {
            screen.closeAnimated();
            return;
        }

        client.setScreen(new LocalControlScreen());
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public LocalTimeController timeController() {
        return timeController;
    }

    public LocalWeatherController weatherController() {
        return weatherController;
    }
}
