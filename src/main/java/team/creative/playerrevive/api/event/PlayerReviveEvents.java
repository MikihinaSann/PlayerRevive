package team.creative.playerrevive.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.entity.player.Player;
import team.creative.playerrevive.api.IBleeding;

public class PlayerReviveEvents {

    private static final List<Consumer<PlayerBleedOutEvent>> BLEED_OUT = new ArrayList<>();
    private static final List<Consumer<PlayerRevivedEvent>> REVIVED = new ArrayList<>();

    public static void onBleedOut(Consumer<PlayerBleedOutEvent> listener) {
        BLEED_OUT.add(listener);
    }

    public static void onRevived(Consumer<PlayerRevivedEvent> listener) {
        REVIVED.add(listener);
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

}
