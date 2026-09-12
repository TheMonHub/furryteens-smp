package net.themonhub.ftsmp.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.themonhub.ftsmp.pvphandler.PvpHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class DisconnectMixin {
    @Inject(method = {"disconnect"}, at = {@At("HEAD")})
    private void injectDisconnectMethod(CallbackInfo ci) {
        PvpHandler.INSTANCE.onPlayerLeave((ServerPlayer)(Object)this);
    }
}
