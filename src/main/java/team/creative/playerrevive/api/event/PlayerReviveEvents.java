package team.creative.playerrevive.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.entity.player.Player;
import team.creative.playerrevive.api.IBleeding;

public class PlayerReviveEvents {

    private static final List<Consumer<PlayerBleedOutEvent>> BLEED_OUT = new ArrayList<>();
    private static final List<Consumer<PlayerRevivedEvent>> REVIVED = new ArrayList<>();
    private static final List<Consumer<ReviveStartEvent>> REVIVE_START = new ArrayList<>();
    private static final List<Consumer<ReviveCancelEvent>> REVIVE_CANCEL = new ArrayList<>();
    private static final List<Consumer<ReviveCompleteEvent>> REVIVE_COMPLETE = new ArrayList<>();

    public static void onBleedOut(Consumer<PlayerBleedOutEvent> listener) {
        BLEED_OUT.add(listener);
    }

    public static void onRevived(Consumer<PlayerRevivedEvent> listener) {
        REVIVED.add(listener);
    }

    public static void onReviveStart(Consumer<ReviveStartEvent> listener) {
        REVIVE_START.add(listener);
    }

    public static void onReviveCancel(Consumer<ReviveCancelEvent> listener) {
        REVIVE_CANCEL.add(listener);
    }

    public static void onReviveComplete(Consumer<ReviveCompleteEvent> listener) {
        REVIVE_COMPLETE.add(listener);
    }

    public static void fireBleedOut(Player player, IBleeding revive) {
        PlayerBleedOutEvent event = new PlayerBleedOutEvent(player, revive);
        for (Consumer<PlayerBleedOutEvent> listener : BLEED_OUT)
            listener.accept(event);
    }

    public static void fireRevived(Player player, IBleeding revive) {
        PlayerRevivedEvent event = new PlayerRevivedEvent(player, revive);
        for (Consumer<PlayerRevivedEvent> listener : REVIVED)
            listener.accept(event);
    }

    public static void fireReviveStart(Player helper, Player target) {
        ReviveStartEvent event = new ReviveStartEvent(helper, target);
        for (Consumer<ReviveStartEvent> listener : REVIVE_START)
            listener.accept(event);
    }

    public static void fireReviveCancel(Player helper, Player target) {
        ReviveCancelEvent event = new ReviveCancelEvent(helper, target);
        for (Consumer<ReviveCancelEvent> listener : REVIVE_CANCEL)
            listener.accept(event);
    }

    public static void fireReviveComplete(Player helper, Player target) {
        ReviveCompleteEvent event = new ReviveCompleteEvent(helper, target);
        for (Consumer<ReviveCompleteEvent> listener : REVIVE_COMPLETE)
            listener.accept(event);
    }

}
