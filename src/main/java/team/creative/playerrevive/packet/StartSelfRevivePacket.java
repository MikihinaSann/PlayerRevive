package team.creative.playerrevive.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import team.creative.creativecore.common.network.CreativePacket;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.server.PlayerReviveServer;

public class StartSelfRevivePacket extends CreativePacket {
    
    @Override
    public void executeClient(Player player) {}
    
    @Override
    public void executeServer(ServerPlayer player) {
        if (!PlayerReviveFabric.CONFIG.revive.selfRevive.enabled)
            return;
        
        IBleeding bleeding = PlayerReviveServer.getBleeding(player);
        if (bleeding == null || bleeding.isSelfReviving())
            return;
        
        boolean consumed = false;
        if (PlayerReviveFabric.CONFIG.revive.selfRevive.item.is(player.level(), player.getMainHandItem()) && player.getMainHandItem()
                .getCount() >= PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount) {
            if (PlayerReviveFabric.CONFIG.revive.selfRevive.consumeItem) {
                player.getInventory().getSelectedItem().shrink(PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount);
                player.getInventory().setChanged();
            }
            consumed = true;
        }
        
        if (!consumed && PlayerReviveFabric.CONFIG.revive.selfRevive.item.is(player.level(), player.getOffhandItem()) && player.getOffhandItem()
                .getCount() >= PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount) {
            if (PlayerReviveFabric.CONFIG.revive.selfRevive.consumeItem) {
                player.getInventory().getItem(Inventory.SLOT_OFFHAND).shrink(PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount);
                player.getInventory().setChanged();
            }
            consumed = true;
        }
        
        if (!consumed)
            return;
        
        bleeding.startSelfRevive();
        PlayerReviveFabric.CONFIG.revive.selfRevive.sound.play(player, SoundSource.PLAYERS);
    }
    
}
