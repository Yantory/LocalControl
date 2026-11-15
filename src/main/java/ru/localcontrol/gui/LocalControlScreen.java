
package ru.localcontrol.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import ru.localcontrol.LocalControlClient;
import ru.localcontrol.config.ConfigManager;
import ru.localcontrol.time.LocalTimeController;

import java.util.ArrayList;
import java.util.List;

public class LocalControlScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 390;
    private static final int ANIMATION_TICKS = 10;

    private static final int COLOR_PANEL = 0xA8151D28;
    private static final int COLOR_BORDER = 0xFF34465A;
    private static final int COLOR_ACCENT = 0xFF1689D5;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SECONDARY = 0xFF9BAFC3;

    private final LocalControlClient mod;
    private final ConfigManager.ModConfig config;

    private final List<WidgetPosition> movingWidgets =
            new ArrayList<>();

    private EditBox timeInput;

    private Button timeToggleButton;
    private Button weatherToggleButton;

    private float animationProgress = 0.0F;
    private boolean closing = false;
    private boolean updatingTimeInput = false;

    private int panelX;
    private int panelY;

    public LocalControlScreen() {
        super(Component.literal("Local Control"));

        mod = LocalControlClient.INSTANCE;
        config = mod.configManager().getConfig();
    }

    @Override
    protected void init() {
        super.init();

        movingWidgets.clear();

        panelY = Math.max(4, (height - PANEL_HEIGHT) / 2);

        animationProgress = 0.0F;
        closing = false;

        int x = openPanelX();

        // ВРЕМЯ

        timeToggleButton = addTracked(
                Button.builder(
                        getTimeToggleText(),
                        button -> {
                            if (config.localTimeEnabled
                                    && !mod.timeController().isReturningToServer()) {

                                mod.timeController().disableLocalTime();

                            } else if (!config.localTimeEnabled) {

                                mod.timeController().applyPreset(
                                        mod.timeController().getDisplayTimeTicks()
                                );

                                mod.configManager().save();
                            }

                            button.setMessage(getTimeToggleText());
                        }
                ).bounds(x + 16, panelY + 44, 308, 24).build(),
                16,
                44
        );

        // ПРЕСЕТЫ ВРЕМЕНИ

        int presetGap = 4;
        int presetWidth = 74;

        addTimePreset(
                x + 16, panelY + 100, presetWidth,
                "Рассвет", 23000L
        );

        addTimePreset(
                x + 16 + presetWidth + presetGap,
                panelY + 100, presetWidth,
                "Утро", 1000L
        );

        addTimePreset(
                x + 16 + (presetWidth + presetGap) * 2,
                panelY + 100, presetWidth,
                "Полдень", 6000L
        );

        addTimePreset(
                x + 16 + (presetWidth + presetGap) * 3,
                panelY + 100, presetWidth,
                "Вечер", 12000L
        );

        addTimePreset(
                x + 16, panelY + 128, 100,
                "Ночь", 14000L
        );

        addTimePreset(
                x + 120, panelY + 128, 100,
                "Полночь", 18000L
        );

        addTimePreset(
                x + 224, panelY + 128, 100,
                "Закат", 11000L
        );

        // ПОЛЕ ВВОДА ВРЕМЕНИ

        Font font = Minecraft.getInstance().font;

        timeInput = new EditBox(
                font,
                x + 16,
                panelY + 180,
                308,
                20,
                Component.literal("Время или игровые тики")
        );

        timeInput.setMaxLength(5);

        timeInput.setValue(
                LocalTimeController.ticksToClock(config.timeOfDay)
        );

        timeInput.setResponder(this::onTimeInputChanged);

        addTracked(timeInput, 16, 180);

        // ПОГОДА

        weatherToggleButton = addTracked(
                Button.builder(
                        getWeatherToggleText(),
                        button -> {
                            if (config.localWeatherEnabled
                                    && !mod.weatherController().isReturningToServer()) {

                                mod.weatherController().disableLocalWeather(
                                        Minecraft.getInstance().level
                                );

                            } else if (!config.localWeatherEnabled) {

                                config.localWeatherEnabled = true;
                                mod.weatherController().syncFromConfig();
                                mod.configManager().save();
                            }

                            button.setMessage(getWeatherToggleText());
                        }
                ).bounds(x + 16, panelY + 267, 308, 24).build(),
                16,
                267
        );

        // ПРЕСЕТЫ ПОГОДЫ

        addWeatherPreset(
                x + 16, panelY + 309, 100,
                "Ясно", "CLEAR"
        );

        addWeatherPreset(
                x + 120, panelY + 309, 100,
                "Дождь", "RAIN"
        );

        addWeatherPreset(
                x + 224, panelY + 309, 100,
                "Гроза", "THUNDER"
        );

        // СБРОС И ЗАКРЫТИЕ

        addTracked(
                Button.builder(
                        Component.literal("Сбросить"),
                        button -> resetConfig()
                ).bounds(x + 16, panelY + 350, 148, 22).build(),
                16,
                350
        );

        addTracked(
                Button.builder(
                        Component.literal("Закрыть"),
                        button -> closeAnimated()
                ).bounds(x + 176, panelY + 350, 148, 22).build(),
                176,
                350
        );
    }

    /**
     * Поддерживает:
     * HH:mm — обычное время.
     * 0-23999 — игровые тики.
     */
    private void onTimeInputChanged(String value) {
        if (updatingTimeInput) {
            return;
        }

        try {
            long targetTicks;

            if (value.matches("\\d{2}:\\d{2}")) {

                int hours = Integer.parseInt(
                        value.substring(0, 2)
                );

                int minutes = Integer.parseInt(
                        value.substring(3, 5)
                );

                if (hours > 23 || minutes > 59) {
                    return;
                }

                targetTicks = LocalTimeController.clockToTicks(
                        hours,
                        minutes
                );

            } else if (value.matches("\\d{1,5}")) {

                targetTicks = Long.parseLong(value);

                if (targetTicks > 23999) {
                    return;
                }

            } else {
                return;
            }

            mod.timeController().applyPreset(targetTicks);
            mod.configManager().save();

            if (timeToggleButton != null) {
                timeToggleButton.setMessage(getTimeToggleText());
            }

        } catch (NumberFormatException ignored) {
        }
    }

    private void setTimeInputValue(String value) {
        if (timeInput == null) {
            return;
        }

        updatingTimeInput = true;
        timeInput.setValue(value);
        updatingTimeInput = false;
    }

    private <T extends AbstractWidget> T addTracked(
            T widget,
            int offsetX,
            int yOffset
    ) {
        addRenderableWidget(widget);

        movingWidgets.add(
                new WidgetPosition(widget, offsetX, yOffset)
        );

        return widget;
    }

    private int openPanelX() {
        return width - PANEL_WIDTH;
    }

    private int closedPanelX() {
        return width;
    }

    private int currentPanelX() {
        float p = Math.max(
                0.0F,
                Math.min(1.0F, animationProgress)
        );

        float eased = p * p * p
                * (p * (p * 6.0F - 15.0F) + 10.0F);

        return (int) (
                closedPanelX()
                        + (openPanelX() - closedPanelX()) * eased
        );
    }

    private void updateWidgetPositions() {
        panelX = currentPanelX();

        int offset = panelX - openPanelX();

        for (WidgetPosition position : movingWidgets) {
            position.widget().setPosition(
                    openPanelX() + position.offsetX() + offset,
                    panelY + position.yOffset()
            );
        }
    }

    @Override
    public void tick() {
        super.tick();

        float step = 1.0F / ANIMATION_TICKS;

        if (closing) {
            animationProgress = Math.max(
                    0.0F,
                    animationProgress - step
            );

            if (animationProgress <= 0.0F) {
                Minecraft.getInstance().setScreen(null);
            }

        } else {
            animationProgress = Math.min(
                    1.0F,
                    animationProgress + step
            );
        }
    }

    public void closeAnimated() {
        if (!closing) {
            closing = true;
        }
    }

    @Override
    public void onClose() {
        closeAnimated();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component getTimeToggleText() {
        return Component.literal(
                config.localTimeEnabled
                        ? "Отключить локальное время"
                        : "Включить локальное время"
        );
    }

    private Component getWeatherToggleText() {
        return Component.literal(
                config.localWeatherEnabled
                        ? "Отключить локальную погоду"
                        : "Включить локальную погоду"
        );
    }

    private void addTimePreset(
            int x,
            int y,
            int buttonWidth,
            String label,
            long value
    ) {
        addTracked(
                Button.builder(
                        Component.literal(label),
                        button -> {
                            mod.timeController().applyPreset(value);
                            mod.configManager().save();

                            setTimeInputValue(
                                    LocalTimeController.ticksToClock(value)
                            );

                            if (timeToggleButton != null) {
                                timeToggleButton.setMessage(getTimeToggleText());
                            }
                        }
                ).bounds(x, y, buttonWidth, 22).build(),
                x - openPanelX(),
                y - panelY
        );
    }

    private void addWeatherPreset(
            int x,
            int y,
            int buttonWidth,
            String label,
            String preset
    ) {
        addTracked(
                Button.builder(
                        Component.literal(label),
                        button -> {
                            mod.weatherController().setPreset(preset);
                            mod.weatherController().syncFromConfig();

                            mod.configManager().save();

                            if (weatherToggleButton != null) {
                                weatherToggleButton.setMessage(
                                        getWeatherToggleText()
                                );
                            }
                        }
                ).bounds(x, y, buttonWidth, 22).build(),
                x - openPanelX(),
                y - panelY
        );
    }

    private void resetConfig() {
        // Сначала запускаем возврат к серверным значениям.
        mod.timeController().disableLocalTime();

        mod.weatherController().disableLocalWeather(
                Minecraft.getInstance().level
        );

        // Сбрасываем остальные настройки.
        mod.configManager().resetToDefaults();

        // Восстанавливаем флаги, необходимые для работы переходов.
        mod.timeController().continueReturnAfterReset();
        mod.weatherController().continueReturnAfterReset();

        setTimeInputValue(
                LocalTimeController.ticksToClock(config.timeOfDay)
        );

        if (timeToggleButton != null) {
            timeToggleButton.setMessage(getTimeToggleText());
        }

        if (weatherToggleButton != null) {
            weatherToggleButton.setMessage(getWeatherToggleText());
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        updateWidgetPositions();

        int x = panelX;
        int y = panelY;

        // Полупрозрачная панель без затемнения игрового мира.
        graphics.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                COLOR_PANEL
        );

        graphics.outline(
                x,
                y,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                COLOR_BORDER
        );

        graphics.fill(
                x,
                y,
                x + 4,
                y + PANEL_HEIGHT,
                COLOR_ACCENT
        );

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        Font font = Minecraft.getInstance().font;

        // Динамически обновляем надписи кнопок.
        if (timeToggleButton != null) {
            timeToggleButton.setMessage(getTimeToggleText());
        }

        if (weatherToggleButton != null) {
            weatherToggleButton.setMessage(getWeatherToggleText());
        }

        long displayedTicks =
                mod.timeController().getDisplayTimeTicks();

        String displayedClock =
                LocalTimeController.ticksToClock(displayedTicks);

        graphics.text(
                font,
                Component.literal("LOCAL CONTROL"),
                x + 16,
                y + 15,
                COLOR_TEXT,
                true
        );

        graphics.text(
                font,
                Component.literal("Текущее время: " + displayedClock),
                x + 16,
                y + 76,
                COLOR_SECONDARY,
                false
        );

        graphics.text(
                font,
                Component.literal("Пресеты времени"),
                x + 16,
                y + 88,
                COLOR_SECONDARY,
                false
        );

        graphics.text(
                font,
                Component.literal("Время (ЧЧ:ММ или игровые тики)"),
                x + 16,
                y + 164,
                COLOR_SECONDARY,
                false
        );

        graphics.text(
                font,
                Component.literal("Игровые тики: " + displayedTicks),
                x + 16,
                y + 210,
                COLOR_SECONDARY,
                false
        );

        graphics.text(
                font,
                Component.literal("Погода"),
                x + 16,
                y + 250,
                COLOR_SECONDARY,
                false
        );

        if (!mod.timeController().hasServerTime()
                && !config.localTimeEnabled) {

            graphics.text(
                    font,
                    Component.literal("Ожидание времени сервера..."),
                    x + 16,
                    y + 228,
                    COLOR_SECONDARY,
                    false
            );
        }

        if (mod.timeController().isReturningToServer()) {
            graphics.text(
                    font,
                    Component.literal("Возврат к времени сервера..."),
                    x + 16,
                    y + 238,
                    COLOR_SECONDARY,
                    false
            );
        }

        if (mod.weatherController().isReturningToServer()) {
            graphics.text(
                    font,
                    Component.literal("Возврат к погоде сервера..."),
                    x + 16,
                    y + 294,
                    COLOR_SECONDARY,
                    false
            );
        }
    }

    private record WidgetPosition(
            AbstractWidget widget,
            int offsetX,
            int yOffset
    ) {
    }
}
