package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.ParseResults;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import team.creative.playerrevive.server.ReviveEventServer;

@Mixin(Commands.class)
public class CommandsMixin {

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void playerrevive$performCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        if (ReviveEventServer.onCommand(parseResults))
            ci.cancel();
    }
}
