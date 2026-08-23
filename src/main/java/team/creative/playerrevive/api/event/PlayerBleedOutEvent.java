package team.creative.playerrevive.api.event;

import net.minecraft.entity.player.PlayerEntity;
import team.creative.playerrevive.api.IBleeding;

/** Fired before a player is killed */
public class PlayerBleedOutEvent {

    private final PlayerEntity player;
    private final IBleeding bleeding;

    public PlayerBleedOutEvent(PlayerEntity player, IBleeding bleeding) {
        this.player = player;
        this.bleeding = bleeding;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public IBleeding getBleeding() {
        return bleeding;
    }

}
