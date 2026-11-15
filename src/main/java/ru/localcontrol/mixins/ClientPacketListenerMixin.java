
package ru.localcontrol.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.localcontrol.LocalControlClient;
import ru.localcontrol.weather.LocalWeatherController;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(
            method = "handleGameEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void localcontrol$captureServerWeather(
            ClientboundGameEventPacket packet,
            CallbackInfo ci
    ) {
        LocalControlClient mod = LocalControlClient.INSTANCE;

        if (mod == null) {
            return;
        }

        LocalWeatherController controller = mod.weatherController();

        ClientboundGameEventPacket.Type event = packet.getEvent();
        float value = packet.getParam();

        boolean weatherEvent = true;

        if (event == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
            controller.recordServerRain(value);

        } else if (event == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
            controller.recordServerThunder(value);

        } else if (event == ClientboundGameEventPacket.START_RAINING) {
            controller.recordServerRain(1.0F);

        } else if (event == ClientboundGameEventPacket.STOP_RAINING) {
            controller.recordServerRain(0.0F);

        } else {
            weatherEvent = false;
        }

        if (!weatherEvent) {
            return;
        }

        if (controller.isLocalWeatherEnabled()) {
            ci.cancel();
        }
    }
}
