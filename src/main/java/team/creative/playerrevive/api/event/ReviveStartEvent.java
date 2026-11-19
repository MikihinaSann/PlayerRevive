package team.creative.playerrevive.api.event;

import net.minecraft.world.entity.player.Player;

/** Fired when a player starts reviving another player. */
public class ReviveStartEvent {

    private final Player player;
    private final Player target;

    public ReviveStartEvent(Player player, Player target) {
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
