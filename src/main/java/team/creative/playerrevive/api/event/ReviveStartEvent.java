package team.creative.playerrevive.api.event;

import net.minecraft.world.entity.player.Player;

public class ReviveStartEvent {

    private final Player helper;
    private final Player target;

    public ReviveStartEvent(Player helper, Player target) {
        this.helper = helper;
        this.target = target;
    }

    public Player getHelper() {
        return helper;
    }

    public Player getTarget() {
        return target;
    }

}
