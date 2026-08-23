package team.creative.playerrevive.api.event;

import net.minecraft.entity.player.PlayerEntity;

public class ReviveStartEvent {

    private final PlayerEntity helper;
    private final PlayerEntity target;

    public ReviveStartEvent(PlayerEntity helper, PlayerEntity target) {
        this.helper = helper;
        this.target = target;
    }

    public PlayerEntity getHelper() {
        return helper;
    }

    public PlayerEntity getTarget() {
        return target;
    }

}
