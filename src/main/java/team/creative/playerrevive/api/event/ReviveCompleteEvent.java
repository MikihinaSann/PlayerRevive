package team.creative.playerrevive.api.event;

import net.minecraft.world.entity.player.Player;

/** Fired for every reviving player when a player is successfully revived. */
public class ReviveCompleteEvent {

    private final Player player;
    private final Player target;

    public ReviveCompleteEvent(Player player, Player target) {
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
