package team.creative.playerrevive.cap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.CombatTrackerClone;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.api.event.PlayerReviveEvents;
import team.creative.playerrevive.packet.HelperPacket;

public class Bleeding implements IBleeding {
    
    private static final Identifier JUMP_HEIGHT = Identifier.tryBuild(PlayerReviveFabric.MODID, "stopjump");
    
    private boolean bleeding;
    private float progress;
    private int timeLeft;
    private int downedTime;
    
    private DamageSource lastSource;
    private CombatTrackerClone trackerClone;
    private boolean itemConsumed = false;
    
    private boolean selfReviving = false;
    
    public final List<Player> revivingPlayers = new ArrayList<>();
    
    public Bleeding() {}
    
    @Override
    public void tick(Player player) {
        if (player.getPose() != Pose.SWIMMING)
            player.setPose(Pose.SWIMMING);
        for (Iterator<Player> iterator = revivingPlayers.iterator(); iterator.hasNext();) {
            Player helper = iterator.next();
            if (helper.distanceTo(player) > PlayerReviveFabric.CONFIG.revive.maxDistance) {
                PlayerReviveEvents.fireReviveCancel(helper, player);
                PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayer) helper);
                iterator.remove();
            }
        }
        //player.setPose(Pose.SWIMMING);
        if (revivingPlayers.isEmpty() || !PlayerReviveFabric.CONFIG.revive.haltBleedTime)
            timeLeft--;
        if (revivingPlayers.isEmpty() && PlayerReviveFabric.CONFIG.revive.resetProgress && !selfReviving)
            progress = 0;
        
        progress += revivingPlayers.size() * PlayerReviveFabric.CONFIG.revive.progressPerPlayer;
        if (selfReviving)
            progress += PlayerReviveFabric.CONFIG.revive.selfRevive.progress;
        downedTime++;
        
        if (PlayerReviveFabric.CONFIG.revive.exhaustion > 0)
            for (int i = 0; i < revivingPlayers.size(); i++)
                revivingPlayers.get(i).causeFoodExhaustion(PlayerReviveFabric.CONFIG.revive.exhaustion);
    }
    
    @Override
    public void forceBledOut() {
        bleeding = true;
        timeLeft = 0;
    }
    
    @Override
    public int downedTime() {
        return downedTime;
    }
    
    @Override
    public float getProgress() {
        return progress;
    }
    
    @Override
    public boolean revived() {
        return progress >= PlayerReviveFabric.CONFIG.revive.requiredReviveProgress;
    }
    
    @Override
    public boolean bledOut() {
        return bleeding && timeLeft <= 0;
    }
    
    @Override
    public void serialize(ValueOutput output) {
        output.putInt("timeLeft", timeLeft);
        output.putFloat("progress", progress);
        output.putBoolean("bleeding", bleeding);
        output.putBoolean("consumed", itemConsumed);
        output.putBoolean("selfRevive", selfReviving);
    }
    
    @Override
    public void deserialize(ValueInput input) {
        timeLeft = input.getIntOr("timeLeft", 0);
        progress = input.getFloatOr("progress", 0);
        bleeding = input.getBooleanOr("bleeding", false);
        itemConsumed = input.getBooleanOr("consumed", false);
        selfReviving = input.getBooleanOr("selfRevive", false);
    }
    
    @Override
    public boolean isBleeding() {
        return bleeding;
    }
    
    @Override
    public void knockOut(Player player, DamageSource source) {
        this.bleeding = true;
        this.progress = 0;
        this.downedTime = 0;
        this.timeLeft = PlayerReviveFabric.CONFIG.bleeding.bleedTime;
        this.lastSource = source;
        this.trackerClone = new CombatTrackerClone(player.getCombatTracker());
        if (PlayerReviveFabric.CONFIG.bleeding.disableJump)
            player.getAttribute(Attributes.JUMP_STRENGTH).addTransientModifier(new AttributeModifier(JUMP_HEIGHT, -1, Operation.ADD_MULTIPLIED_TOTAL));
    }
    
    @Override
    public void revive(Player player) {
        this.bleeding = false;
        this.progress = 0;
        this.timeLeft = 0;
        this.downedTime = 0;
        this.lastSource = null;
        this.trackerClone = null;
        this.itemConsumed = false;
        this.selfReviving = false;
        if (player.getAttribute(Attributes.JUMP_STRENGTH) != null)
            player.getAttribute(Attributes.JUMP_STRENGTH).removeModifier(JUMP_HEIGHT);
    }
    
    @Override
    public int timeLeft() {
        return timeLeft;
    }
    
    @Override
    public List<Player> revivingPlayers() {
        return revivingPlayers;
    }
    
    @Override
    public CombatTrackerClone getTrackerClone() {
        return trackerClone;
    }
    
    @Override
    public DamageSource getSource(RegistryAccess access) {
        if (lastSource != null)
            return lastSource;
        return new DamageSource(access.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(PlayerReviveFabric.BLED_TO_DEATH));
    }
    
    @Override
    public boolean isItemConsumed() {
        return itemConsumed;
    }
    
    @Override
    public void setItemConsumed() {
        itemConsumed = true;
    }
    
    @Override
    public void startSelfRevive() {
        selfReviving = true;
    }
    
    @Override
    public boolean isSelfReviving() {
        return selfReviving;
    }
}
