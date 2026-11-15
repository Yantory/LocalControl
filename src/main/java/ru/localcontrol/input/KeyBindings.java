
package ru.localcontrol.input;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import ru.localcontrol.LocalControlClient;

public final class KeyBindings {

    private KeyBindings() {
    }

    public static KeyMapping OPEN_MENU;
    public static KeyMapping TOGGLE_TIME;
    public static KeyMapping TOGGLE_WEATHER;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(
                            LocalControlClient.MOD_ID,
                            "main"
                    )
            );

    public static void register() {

        OPEN_MENU = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.localcontrol.open_menu",
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(),
                        CATEGORY
                )
        );

        TOGGLE_TIME = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.localcontrol.toggle_time",
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(),
                        CATEGORY
                )
        );

        TOGGLE_WEATHER = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.localcontrol.toggle_weather",
                        InputConstants.Type.KEYSYM,
                        InputConstants.UNKNOWN.getValue(),
                        CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (OPEN_MENU.consumeClick()) {
                if (LocalControlClient.INSTANCE != null) {
                    LocalControlClient.INSTANCE.openMenu();
                }
            }

            while (TOGGLE_TIME.consumeClick()) {
                if (LocalControlClient.INSTANCE != null) {
                    var mod = LocalControlClient.INSTANCE;
                    var config = mod.configManager().getConfig();

                    config.localTimeEnabled = !config.localTimeEnabled;

                    if (!config.localTimeEnabled) {
                        mod.timeController().disableLocalTime();
                    }

                    mod.configManager().save();
                }
            }

            while (TOGGLE_WEATHER.consumeClick()) {
                if (LocalControlClient.INSTANCE != null) {
                    var mod = LocalControlClient.INSTANCE;
                    var config = mod.configManager().getConfig();

                    config.localWeatherEnabled =
                            !config.localWeatherEnabled;

                    mod.configManager().save();
                }
            }
        });
    }
}
