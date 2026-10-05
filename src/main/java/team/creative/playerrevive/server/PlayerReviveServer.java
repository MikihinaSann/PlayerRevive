package team.creative.playerrevive.server;

import java.io.IOException;
import java.util.Iterator;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import team.creative.creativecore.common.config.premade.MobEffectConfig;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.BleedingHolder;
import team.creative.playerrevive.api.CombatTrackerClone;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.api.event.PlayerReviveEvents;
import team.creative.playerrevive.cap.Bleeding;
import team.creative.playerrevive.packet.HelperPacket;
import team.creative.playerrevive.packet.ReviveUpdatePacket;

public class PlayerReviveServer {

    public static EntityDataAccessor<Boolean> BLEEDING_TRACKER;

    public static void initTrackedData() {
        BLEEDING_TRACKER = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    }

    public static boolean isBleeding(Player player) {
        return getBleeding(player).isBleeding();
    }

    public static int timeLeft(Player player) {
        return getBleeding(player).timeLeft();
    }

    public static int downedTime(Player player) {
        return getBleeding(player).downedTime();
    }

    public static IBleeding getBleeding(Player player) {
        BleedingHolder holder = (BleedingHolder) player;
        if (holder.playerrevive$getBleeding() != null)
            return holder.playerrevive$getBleeding();
        Bleeding bleeding = new Bleeding();
        holder.playerrevive$setBleeding(bleeding);
        return bleeding;
    }

    public static void sendUpdatePacket(Player player) {
        ReviveUpdatePacket packet = new ReviveUpdatePacket(player);
        PlayerReviveFabric.NETWORK.sendToClientTracking(packet, player);
        PlayerReviveFabric.NETWORK.sendToClient(packet, (ServerPlayer) player);
    }

    public static void startBleeding(Player player, DamageSource source) {
        getBleeding(player).knockOut(player, source);
        player.getEntityData().set(BLEEDING_TRACKER, true);
        sendUpdatePacket(player);
    }

    public static void cancelHelper(Player bleeding, Player helper) {
        PlayerReviveEvents.fireReviveCancel(helper, bleeding);
        PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayer) helper);
    }

    public static void completeHelper(Player bleeding, Player helper) {
        PlayerReviveEvents.fireReviveComplete(helper, bleeding);
        PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayer) helper);
    }

    private static void resetPlayer(Player player, IBleeding revive, boolean successful) {
        for (Player helper : revive.revivingPlayers())
            if (successful)
                completeHelper(player, helper);
            else
                cancelHelper(player, helper);
        revive.revivingPlayers().clear();

        player.getEntityData().set(BLEEDING_TRACKER, false);
        sendUpdatePacket(player);
    }

    public static void revive(Player player) {
        IBleeding revive = getBleeding(player);
        revive.revive(player);

        for (MobEffectConfig effect : PlayerReviveFabric.CONFIG.revive.revivedEffects)
            player.addEffect(effect.create());

        resetPlayer(player, revive, true);
        player.setHealth(PlayerReviveFabric.CONFIG.revive.healthAfter);

        PlayerReviveFabric.CONFIG.sounds.revived.play(player, SoundSource.PLAYERS);

        PlayerReviveEvents.fireRevived(player, revive);

        sendUpdatePacket(player);

        player.setPose(Pose.STANDING);
    }

    public static void kill(Player player) {
        IBleeding revive = getBleeding(player);
        PlayerReviveEvents.fireBleedOut(player, revive);
        DamageSource source = revive.getSource(player.level().registryAccess());
        CombatTrackerClone trackerClone = revive.getTrackerClone();
        if (trackerClone != null)
            trackerClone.overwriteTracker(player.getCombatTracker());
        player.setHealth(0.0F);
        revive.forceBledOut();
        player.die(source);
        resetPlayer(player, revive, false);
        revive.revive(player); // Done for compatibility reason for rare scenarios the player will not die
        player.setPose(Pose.STANDING);

        PlayerReviveFabric.CONFIG.sounds.death.play(player, SoundSource.PLAYERS);

        if (PlayerReviveFabric.CONFIG.banPlayerAfterDeath) {
            try {
                player.level().getServer().getPlayerList().getBans().add(new UserBanListEntry(player.nameAndId()));
                player.level().getServer().getPlayerList().getBans().save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        sendUpdatePacket(player);
    }

    public static void removePlayerAsHelper(Player player) {
        for (Iterator<ServerPlayer> iterator = player.level().getServer().getPlayerList().getPlayers().iterator(); iterator.hasNext();) {
            ServerPlayer member = iterator.next();
            IBleeding revive = getBleeding(member);
            PlayerReviveEvents.fireReviveCancel(player, member);
            revive.revivingPlayers().remove(player);
        }

    }
}
