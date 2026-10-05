package team.creative.playerrevive.api.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import team.creative.playerrevive.api.IBleeding;

/**
 * Central event callback registry replacing NeoForge's event bus.
 * Other mods can register listeners here.
 */
public class PlayerReviveEvents {

    @FunctionalInterface
    public interface BleedOutCallback {
        void onBleedOut(Player player, IBleeding bleeding);
    }

    @FunctionalInterface
    public interface RevivedCallback {
        void onRevived(Player player, IBleeding bleeding);
    }

    @FunctionalInterface
    public interface ReviveCancelCallback {
        void onReviveCancel(Player helper, Player target);
    }

    @FunctionalInterface
    public interface ReviveCompleteCallback {
        void onReviveComplete(Player helper, Player target);
    }

    @FunctionalInterface
    public interface ReviveStartCallback {
        void onReviveStart(Player helper, Player target);
    }

    public static final List<BleedOutCallback> BLEED_OUT = new ArrayList<>();
    public static final List<RevivedCallback> REVIVED = new ArrayList<>();
    public static final List<ReviveCancelCallback> REVIVE_CANCEL = new ArrayList<>();
    public static final List<ReviveCompleteCallback> REVIVE_COMPLETE = new ArrayList<>();
    public static final List<ReviveStartCallback> REVIVE_START = new ArrayList<>();

    public static void fireBleedOut(Player player, IBleeding bleeding) {
        for (BleedOutCallback cb : BLEED_OUT)
            cb.onBleedOut(player, bleeding);
    }

    public static void fireRevived(Player player, IBleeding bleeding) {
        for (RevivedCallback cb : REVIVED)
            cb.onRevived(player, bleeding);
    }

    public static void fireReviveCancel(Player helper, Player target) {
        for (ReviveCancelCallback cb : REVIVE_CANCEL)
            cb.onReviveCancel(helper, target);
    }

    public static void fireReviveComplete(Player helper, Player target) {
        for (ReviveCompleteCallback cb : REVIVE_COMPLETE)
            cb.onReviveComplete(helper, target);
    }

    public static void fireReviveStart(Player helper, Player target) {
        for (ReviveStartCallback cb : REVIVE_START)
            cb.onReviveStart(helper, target);
    }

}
