package team.creative.playerrevive.api.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import team.creative.playerrevive.api.IBleeding;

/**
 * Central event callback registry replacing NeoForge's event bus.
 * Other mods can register listeners here.
 */
public class PlayerReviveEvents {

    @FunctionalInterface
    public interface BleedOutCallback {
        void onBleedOut(PlayerEntity player, IBleeding bleeding);
    }

    @FunctionalInterface
    public interface RevivedCallback {
        void onRevived(PlayerEntity player, IBleeding bleeding);
    }

    @FunctionalInterface
    public interface ReviveCancelCallback {
        void onReviveCancel(PlayerEntity helper, PlayerEntity target);
    }

    @FunctionalInterface
    public interface ReviveCompleteCallback {
        void onReviveComplete(PlayerEntity helper, PlayerEntity target);
    }

    @FunctionalInterface
    public interface ReviveStartCallback {
        void onReviveStart(PlayerEntity helper, PlayerEntity target);
    }

    public static final List<BleedOutCallback> BLEED_OUT = new ArrayList<>();
    public static final List<RevivedCallback> REVIVED = new ArrayList<>();
    public static final List<ReviveCancelCallback> REVIVE_CANCEL = new ArrayList<>();
    public static final List<ReviveCompleteCallback> REVIVE_COMPLETE = new ArrayList<>();
    public static final List<ReviveStartCallback> REVIVE_START = new ArrayList<>();

    public static void fireBleedOut(PlayerEntity player, IBleeding bleeding) {
        for (BleedOutCallback cb : BLEED_OUT)
            cb.onBleedOut(player, bleeding);
    }

    public static void fireRevived(PlayerEntity player, IBleeding bleeding) {
        for (RevivedCallback cb : REVIVED)
            cb.onRevived(player, bleeding);
    }

    public static void fireReviveCancel(PlayerEntity helper, PlayerEntity target) {
        for (ReviveCancelCallback cb : REVIVE_CANCEL)
            cb.onReviveCancel(helper, target);
    }

    public static void fireReviveComplete(PlayerEntity helper, PlayerEntity target) {
        for (ReviveCompleteCallback cb : REVIVE_COMPLETE)
            cb.onReviveComplete(helper, target);
    }

    public static void fireReviveStart(PlayerEntity helper, PlayerEntity target) {
        for (ReviveStartCallback cb : REVIVE_START)
            cb.onReviveStart(helper, target);
    }

}
