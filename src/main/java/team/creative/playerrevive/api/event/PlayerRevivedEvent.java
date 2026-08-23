package team.creative.playerrevive.api.event;

import net.minecraft.entity.player.PlayerEntity;
import team.creative.playerrevive.api.IBleeding;

/** Fired before a player is revived. */
public class PlayerRevivedEvent {

    private final PlayerEntity player;
    private final IBleeding bleeding;

    public PlayerRevivedEvent(PlayerEntity player, IBleeding bleeding) {
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
