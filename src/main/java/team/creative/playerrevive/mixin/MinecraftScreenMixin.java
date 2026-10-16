package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.client.ReviveEventClient;
import team.creative.playerrevive.server.PlayerReviveServer;

@Mixin(Minecraft.class)
public class MinecraftScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void playerrevive$setScreen(Screen screen, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player == null || !mc.player.isAlive())
            return;

        IBleeding bleeding = PlayerReviveServer.getBleeding(mc.player);
        if (!bleeding.isBleeding())
            return;

        if (screen == null) {
            ReviveEventClient.inPauseScreen = false;
            return;
        }

        if (PlayerReviveFabric.CONFIG.bleeding.disableInventoryAccess && screen instanceof InventoryScreen)
            ci.cancel();
        else if (PlayerReviveFabric.CONFIG.bleeding.disableChatAccess && screen instanceof ChatScreen)
            ci.cancel();
        else if (PlayerReviveFabric.CONFIG.bleeding.disableAllGUIAccess && !(screen instanceof DeathScreen)) {
            if (screen instanceof PauseScreen)
                ReviveEventClient.inPauseScreen = true;
            if (!ReviveEventClient.inPauseScreen)
                ci.cancel();
        } else
            ReviveEventClient.inPauseScreen = true;
    }
}
