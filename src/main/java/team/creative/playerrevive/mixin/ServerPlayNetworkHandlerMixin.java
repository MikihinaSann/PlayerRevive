package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import team.creative.playerrevive.server.ReviveEventServer;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void onExecuteCommand(String command, CallbackInfo ci) {
        ServerPlayNetworkHandler self = (ServerPlayNetworkHandler) (Object) this;
        if (ReviveEventServer.onCommand(self.player.getCommandSource())) {
            ci.cancel();
        }
    }
}
