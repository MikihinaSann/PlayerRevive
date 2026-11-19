package team.creative.playerrevive.api.event;

import net.minecraft.world.entity.player.Player;

/** Fired when a player stops reviving another player without completing it. */
public class ReviveCancelEvent {

    private final Player player;
    private final Player target;

    public ReviveCancelEvent(Player player, Player target) {
        this.player = player;
        this.target = target;
    }

    public Player getEntity() {
        return player;
    }

    public Player getTarget() {
        return target;
    }
}
