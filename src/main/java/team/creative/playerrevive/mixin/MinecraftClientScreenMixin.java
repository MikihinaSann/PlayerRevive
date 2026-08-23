package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.client.ReviveEventClient;
import team.creative.playerrevive.server.PlayerReviveServer;

@Mixin(MinecraftClient.class)
public class MinecraftClientScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        MinecraftClient self = (MinecraftClient) (Object) this;
        if (self.player == null || !self.player.isAlive())
            return;

        IBleeding bleeding = PlayerReviveServer.getBleeding(self.player);
        if (!bleeding.isBleeding())
            return;

        if (screen == null) {
            ReviveEventClient.inPauseScreen = false;
            return;
        }

        if (PlayerReviveFabric.CONFIG.bleeding.disableInventoryAccess && screen instanceof InventoryScreen) {
            ci.cancel();
        } else if (PlayerReviveFabric.CONFIG.bleeding.disableChatAccess && screen instanceof ChatScreen) {
            ci.cancel();
        } else if (PlayerReviveFabric.CONFIG.bleeding.disableAllGUIAccess && !(screen instanceof DeathScreen)) {
            if (screen instanceof GameMenuScreen) {
                ReviveEventClient.inPauseScreen = true;
            }
            if (!ReviveEventClient.inPauseScreen) {
                ci.cancel();
            }
        } else {
            ReviveEventClient.inPauseScreen = true;
        }
    }
}
