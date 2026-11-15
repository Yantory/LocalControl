
package ru.localcontrol.mixins;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.localcontrol.LocalControlClient;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {

    @Inject(
            method = "getTotalTicks",
            at = @At("RETURN"),
            cancellable = true
    )
    private void localcontrol$overrideTime(
            Holder<WorldClock> definition,
            CallbackInfoReturnable<Long> cir
    ) {
        LocalControlClient mod = LocalControlClient.INSTANCE;

        if (mod == null) {
            return;
        }

        long serverTime = cir.getReturnValue();

        mod.timeController().recordServerTime(
                definition,
                serverTime
        );

        long localTime = mod.timeController().overrideTime(
                definition,
                serverTime
        );

        cir.setReturnValue(localTime);
    }
}
