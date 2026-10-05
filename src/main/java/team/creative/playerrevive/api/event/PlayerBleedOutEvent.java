package team.creative.playerrevive.api.event;

import net.minecraft.world.entity.player.Player;
import team.creative.playerrevive.api.IBleeding;

/** Fired before a player is killed */
public class PlayerBleedOutEvent {

    private final Player player;
    private final IBleeding bleeding;

    public PlayerBleedOutEvent(Player player, IBleeding bleeding) {
        this.player = player;
        this.bleeding = bleeding;
    }

    public Player getPlayer() {
        return player;
    }

    public IBleeding getBleeding() {
        return bleeding;
    }

}
