
package ru.localcontrol.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private final ModConfig config = new ModConfig();

    private final Path configPath =
            QuiltLoader.getConfigDir().resolve("localcontrol.json");

    public ModConfig getConfig() {
        return config;
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                resetToDefaults();
                return;
            }

            String json = Files.readString(configPath);
            ModConfig loaded = GSON.fromJson(json, ModConfig.class);

            if (loaded == null) {
                throw new IllegalStateException("Config was empty");
            }

            config.apply(loaded);
            config.validate();

        } catch (Exception exception) {
            System.err.println("[LocalControl] Failed to load config:");
            exception.printStackTrace();

            config.resetToDefaults();
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());

            Files.writeString(
                    configPath,
                    GSON.toJson(config)
            );

        } catch (IOException exception) {
            System.err.println("[LocalControl] Failed to save config:");
            exception.printStackTrace();
        }
    }

    public void resetToDefaults() {
        config.resetToDefaults();
        save();
    }

    public static class ModConfig {

        // TIME

        public boolean localTimeEnabled = false;
        public boolean timeLocked = true;
        public long timeOfDay = 18000L;

        // WEATHER

        public boolean localWeatherEnabled = false;
        public String weatherPreset = "CLEAR";
        public float rainIntensity = 1.0F;
        public float weatherVisualDuration = 12000.0F;
        public boolean weatherVisualOverride = true;

        // EXTRA

        public boolean extraBrightnessEnabled = false;
        public boolean cinematicFogEnabled = false;
        public boolean atmosphereOverride = false;

        public void validate() {
            timeOfDay = Math.floorMod(timeOfDay, 24000L);

            if (!Float.isFinite(rainIntensity)) {
                rainIntensity = 1.0F;
            }

            rainIntensity = Math.max(
                    0.0F,
                    Math.min(1.0F, rainIntensity)
            );

            if (!Float.isFinite(weatherVisualDuration)) {
                weatherVisualDuration = 12000.0F;
            }

            weatherVisualDuration = Math.max(
                    1.0F,
                    Math.min(1000000.0F, weatherVisualDuration)
            );

            if (weatherPreset == null) {
                weatherPreset = "CLEAR";
            }

            weatherPreset = weatherPreset.toUpperCase();

            if (!weatherPreset.equals("CLEAR")
                    && !weatherPreset.equals("RAIN")
                    && !weatherPreset.equals("THUNDER")) {
                weatherPreset = "CLEAR";
            }
        }

        public void resetToDefaults() {
            localTimeEnabled = false;
            timeLocked = true;
            timeOfDay = 18000L;

            localWeatherEnabled = false;
            weatherPreset = "CLEAR";
            rainIntensity = 1.0F;
            weatherVisualDuration = 12000.0F;
            weatherVisualOverride = true;

            extraBrightnessEnabled = false;
            cinematicFogEnabled = false;
            atmosphereOverride = false;
        }

        public void apply(ModConfig other) {
            localTimeEnabled = other.localTimeEnabled;
            timeLocked = other.timeLocked;
            timeOfDay = other.timeOfDay;

            localWeatherEnabled = other.localWeatherEnabled;
            weatherPreset = other.weatherPreset;
            rainIntensity = other.rainIntensity;
            weatherVisualDuration = other.weatherVisualDuration;
            weatherVisualOverride = other.weatherVisualOverride;

            extraBrightnessEnabled = other.extraBrightnessEnabled;
            cinematicFogEnabled = other.cinematicFogEnabled;
            atmosphereOverride = other.atmosphereOverride;
        }
    }
}
