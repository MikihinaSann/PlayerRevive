package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import team.creative.playerrevive.server.ReviveEventServer;

@Mixin(Commands.class)
public class CommandsMixin {

    @Inject(method = "performPrefixedCommand", at = @At("HEAD"), cancellable = true)
    private static void playerrevive$performPrefixedCommand(CommandSourceStack source, String command, CallbackInfo ci) {
        if (ReviveEventServer.onCommand(source))
            ci.cancel();
    }
}
