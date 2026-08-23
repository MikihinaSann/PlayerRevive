package team.creative.playerrevive.server;

import java.io.IOException;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import team.creative.creativecore.common.config.premade.MobEffectConfig;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.BleedingHolder;
import team.creative.playerrevive.api.DamageTrackerClone;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.api.event.PlayerReviveEvents;
import team.creative.playerrevive.cap.Bleeding;
import team.creative.playerrevive.packet.HelperPacket;
import team.creative.playerrevive.packet.ReviveUpdatePacket;

public class PlayerReviveServer {

    public static boolean isBleeding(PlayerEntity player) {
        return getBleeding(player).isBleeding();
    }

    public static int timeLeft(PlayerEntity player) {
        return getBleeding(player).timeLeft();
    }

    public static int downedTime(PlayerEntity player) {
        return getBleeding(player).downedTime();
    }

    public static IBleeding getBleeding(PlayerEntity player) {
        BleedingHolder holder = (BleedingHolder) player;
        if (holder.playerrevive$getBleeding() != null)
            return holder.playerrevive$getBleeding();
        Bleeding bleeding = new Bleeding();
        holder.playerrevive$setBleeding(bleeding);
        return bleeding;
    }

    public static void sendUpdatePacket(PlayerEntity player) {
        ReviveUpdatePacket packet = new ReviveUpdatePacket(player);
        PlayerReviveFabric.NETWORK.sendToClientTracking(packet, player);
        PlayerReviveFabric.NETWORK.sendToClient(packet, (ServerPlayerEntity) player);
    }

    public static void startBleeding(PlayerEntity player, DamageSource source) {
        getBleeding(player).knockOut(player, source);
        player.getDataTracker().set(PlayerReviveServer.BLEEDING_TRACKER, true);
        sendUpdatePacket(player);
    }

    public static void cancelHelper(PlayerEntity bleeding, PlayerEntity helper) {
        PlayerReviveEvents.fireReviveCancel(helper, bleeding);
        PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayerEntity) helper);
    }

    public static void completeHelper(PlayerEntity bleeding, PlayerEntity helper) {
        PlayerReviveEvents.fireReviveComplete(helper, bleeding);
        PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayerEntity) helper);
    }

    private static void resetPlayer(PlayerEntity player, IBleeding revive, boolean successful) {
        for (PlayerEntity helper : revive.revivingPlayers())
            if (successful)
                completeHelper(player, helper);
            else
                cancelHelper(player, helper);
        revive.revivingPlayers().clear();

        player.getDataTracker().set(PlayerReviveServer.BLEEDING_TRACKER, false);
        sendUpdatePacket(player);
    }

    public static void revive(PlayerEntity player) {
        IBleeding revive = getBleeding(player);
        revive.revive(player);

        for (MobEffectConfig effect : PlayerReviveFabric.CONFIG.revive.revivedEffects)
            player.addStatusEffect(effect.create());

        resetPlayer(player, revive, true);
        player.setHealth(PlayerReviveFabric.CONFIG.revive.healthAfter);

        PlayerReviveFabric.CONFIG.sounds.revived.play(player, SoundCategory.PLAYERS);

        PlayerReviveEvents.fireRevived(player, revive);

        sendUpdatePacket(player);

        player.setPose(net.minecraft.entity.EntityPose.STANDING);
    }

    public static void kill(PlayerEntity player) {
        IBleeding revive = getBleeding(player);
        PlayerReviveEvents.fireBleedOut(player, revive);
        DamageSource source = revive.getSource(player.getRegistryManager());
        DamageTrackerClone trackerClone = revive.getTrackerClone();
        if (trackerClone != null)
            trackerClone.overwriteTracker(player.getDamageTracker());
        player.setHealth(0.0F);
        revive.forceBledOut();
        player.onDeath(source);
        resetPlayer(player, revive, false);
        revive.revive(player); // Done for compatibility reason for rare scenarios the player will not die
        player.setPose(net.minecraft.entity.EntityPose.STANDING);

        PlayerReviveFabric.CONFIG.sounds.death.play(player, SoundCategory.PLAYERS);

        if (PlayerReviveFabric.CONFIG.banPlayerAfterDeath) {
            try {
                player.getServer().getPlayerManager().getUserBanList().add(new BannedPlayerEntry(player.getGameProfile()));
                player.getServer().getPlayerManager().getUserBanList().save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // resetPlayer already sends update packet, but state changed after (revive + pose)
        // so send one final packet with the correct final state
        sendUpdatePacket(player);
    }

    public static void removePlayerAsHelper(PlayerEntity player) {
        for (ServerPlayerEntity member : player.getServer().getPlayerManager().getPlayerList()) {
            IBleeding revive = getBleeding(member);
            if (revive.revivingPlayers().contains(player)) {
                PlayerReviveEvents.fireReviveCancel(player, member);
                revive.revivingPlayers().remove(player);
            }
        }
    }

    // TrackedData for persistent bleeding state (replaces getPersistentData().putBoolean)
    public static net.minecraft.entity.data.TrackedData<java.lang.Boolean> BLEEDING_TRACKER;

    public static void initTrackedData() {
        BLEEDING_TRACKER = net.minecraft.entity.data.DataTracker.registerData(PlayerEntity.class, net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    }
}
