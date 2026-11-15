
package ru.localcontrol.time;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import ru.localcontrol.LocalControlClient;
import ru.localcontrol.config.ConfigManager;

import java.util.Locale;

public class LocalTimeController {

    private static final long DAY_LENGTH = 24000L;
    private static final int TRANSITION_TICKS = 40;

    private final ConfigManager.ModConfig config;

    private boolean transitioning = false;
    private boolean returningToServer = false;

    private int transitionTick = 0;

    private double transitionStart = 0.0D;
    private double transitionDifference = 0.0D;

    private long lastServerTime = 18000L;
    private boolean serverTimeReceived = false;

    private double returnOffset = 0.0D;

    public LocalTimeController(ConfigManager.ModConfig config) {
        this.config = config;
    }

    public void syncFromConfig() {
        config.timeOfDay = Math.floorMod(
                config.timeOfDay,
                DAY_LENGTH
        );
    }

    /**
     * Запоминает настоящее время сервера до локального переопределения.
     */
    public void recordServerTime(
            Holder<WorldClock> definition,
            long serverTicks
    ) {
        if (definition == null
                || !definition.is(WorldClocks.OVERWORLD)) {
            return;
        }

        lastServerTime = Math.floorMod(
                serverTicks,
                DAY_LENGTH
        );

        serverTimeReceived = true;
    }

    public void tick(ClientLevel world) {
        if (world == null || !config.localTimeEnabled) {
            return;
        }

        if (!transitioning) {
            return;
        }

        transitionTick++;

        double progress = Math.min(
                1.0D,
                (double) transitionTick / TRANSITION_TICKS
        );

        double eased = smoothStep(progress);

        if (returningToServer) {

            double remainingOffset = returnOffset * (1.0D - eased);

            config.timeOfDay = Math.floorMod(
                    Math.round(lastServerTime + remainingOffset),
                    DAY_LENGTH
            );

        } else {

            double currentTime = transitionStart
                    + transitionDifference * eased;

            config.timeOfDay = Math.floorMod(
                    Math.round(currentTime),
                    DAY_LENGTH
            );
        }

        if (transitionTick >= TRANSITION_TICKS) {

            if (returningToServer) {
                finishReturnToServer();
            } else {
                config.timeOfDay = Math.floorMod(
                        Math.round(
                                transitionStart + transitionDifference
                        ),
                        DAY_LENGTH
                );

                transitioning = false;
                transitionTick = 0;
            }
        }
    }

    public long overrideTime(
            Holder<WorldClock> definition,
            long originalTicks
    ) {
        if (definition == null
                || !definition.is(WorldClocks.OVERWORLD)) {
            return originalTicks;
        }

        if (returningToServer && transitioning) {

            double progress = Math.min(
                    1.0D,
                    (double) transitionTick / TRANSITION_TICKS
            );

            double remainingOffset =
                    returnOffset * (1.0D - smoothStep(progress));

            return originalTicks + Math.round(remainingOffset);
        }

        if (!config.localTimeEnabled) {
            return originalTicks;
        }

        long localTime = Math.floorMod(
                config.timeOfDay,
                DAY_LENGTH
        );

        long dayStart = originalTicks
                - Math.floorMod(originalTicks, DAY_LENGTH);

        return dayStart + localTime;
    }

    /**
     * Плавно устанавливает новое локальное время.
     */
    public void applyPreset(long targetTime) {
        targetTime = Math.floorMod(targetTime, DAY_LENGTH);

        long currentTime = getDisplayTimeTicks();

        config.timeOfDay = currentTime;
        config.localTimeEnabled = true;
        config.timeLocked = true;

        returningToServer = false;

        transitionStart = currentTime;

        double difference = targetTime - transitionStart;

        if (difference > DAY_LENGTH / 2.0D) {
            difference -= DAY_LENGTH;
        } else if (difference < -DAY_LENGTH / 2.0D) {
            difference += DAY_LENGTH;
        }

        transitionDifference = difference;
        transitionTick = 0;
        transitioning = true;
    }

    /**
     * Запускает плавное возвращение к настоящему времени сервера.
     */
    public void disableLocalTime() {
        if (returningToServer) {
            return;
        }

        if (!config.localTimeEnabled) {
            return;
        }

        if (!serverTimeReceived) {
            config.localTimeEnabled = false;
            transitioning = false;
            returningToServer = false;
            saveConfig();
            return;
        }

        double difference = config.timeOfDay - lastServerTime;

        if (difference > DAY_LENGTH / 2.0D) {
            difference -= DAY_LENGTH;
        } else if (difference < -DAY_LENGTH / 2.0D) {
            difference += DAY_LENGTH;
        }

        returnOffset = difference;

        returningToServer = true;
        transitioning = true;
        transitionTick = 0;

        // Флаг остаётся включённым до завершения перехода,
        // чтобы существующий ClientTickEvent продолжал вызывать tick().
        config.localTimeEnabled = true;
    }

    /**
     * Используется после сброса конфигурации.
     * Сам переход уже запущен, поэтому восстанавливаем флаг обновления.
     */
    public void continueReturnAfterReset() {
        if (returningToServer && transitioning) {
            config.localTimeEnabled = true;
        }
    }

    private void finishReturnToServer() {
        config.timeOfDay = lastServerTime;

        transitioning = false;
        returningToServer = false;

        transitionTick = 0;
        returnOffset = 0.0D;

        config.localTimeEnabled = false;

        saveConfig();
    }

    private void saveConfig() {
        if (LocalControlClient.INSTANCE != null) {
            LocalControlClient.INSTANCE.configManager().save();
        }
    }

    private static double smoothStep(double value) {
        return value * value * value
                * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    public static long clockToTicks(int hours, int minutes) {
        if (hours < 0 || hours > 23
                || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Invalid time");
        }

        long minutesFromMidnight = hours * 60L + minutes;

        long ticksFromMidnight = Math.round(
                minutesFromMidnight * DAY_LENGTH / 1440.0D
        );

        return Math.floorMod(
                ticksFromMidnight - 6000L,
                DAY_LENGTH
        );
    }

    public static String ticksToClock(long ticks) {
        long clockTicks = Math.floorMod(
                ticks + 6000L,
                DAY_LENGTH
        );

        long totalMinutes = Math.round(
                clockTicks * 1440.0D / DAY_LENGTH
        );

        totalMinutes = Math.floorMod(totalMinutes, 1440L);

        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;

        return String.format(
                Locale.ROOT,
                "%02d:%02d",
                hours,
                minutes
        );
    }

    public long getDisplayTimeTicks() {
        if (returningToServer && transitioning) {
            double progress = Math.min(
                    1.0D,
                    (double) transitionTick / TRANSITION_TICKS
            );

            return Math.floorMod(
                    Math.round(
                            lastServerTime
                                    + returnOffset * (1.0D - smoothStep(progress))
                    ),
                    DAY_LENGTH
            );
        }

        if (config.localTimeEnabled) {
            return Math.floorMod(config.timeOfDay, DAY_LENGTH);
        }

        return serverTimeReceived
                ? lastServerTime
                : config.timeOfDay;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public boolean isReturningToServer() {
        return returningToServer;
    }

    public boolean hasServerTime() {
        return serverTimeReceived;
    }
}
