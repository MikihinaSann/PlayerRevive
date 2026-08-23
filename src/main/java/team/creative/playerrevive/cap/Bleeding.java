package team.creative.playerrevive.cap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.DamageTrackerClone;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.api.event.PlayerReviveEvents;
import team.creative.playerrevive.packet.HelperPacket;

public class Bleeding implements IBleeding {

    private static final Identifier JUMP_HEIGHT = Identifier.of(PlayerReviveFabric.MODID, "stopjump");

    private boolean bleeding;
    private float progress;
    private int timeLeft;
    private int downedTime;

    private DamageSource lastSource;
    private DamageTrackerClone trackerClone;
    private boolean itemConsumed = false;

    private boolean selfReviving = false;

    public final List<PlayerEntity> revivingPlayers = new ArrayList<>();

    public Bleeding() {}

    @Override
    public void tick(PlayerEntity player) {
        if (player.getPose() != EntityPose.SWIMMING)
            player.setPose(EntityPose.SWIMMING);
        // Pose is maintained by mixin canceling updatePose() while bleeding
        for (Iterator<PlayerEntity> iterator = revivingPlayers.iterator(); iterator.hasNext();) {
            PlayerEntity helper = iterator.next();
            if (helper.distanceTo(player) > PlayerReviveFabric.CONFIG.revive.maxDistance) {
                PlayerReviveEvents.fireReviveCancel(helper, player);
                PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayerEntity) helper);
                iterator.remove();
            }
        }
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
                revivingPlayers.get(i).getHungerManager().addExhaustion(PlayerReviveFabric.CONFIG.revive.exhaustion);
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
    public NbtCompound serializeNBT() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("timeLeft", timeLeft);
        nbt.putFloat("progress", progress);
        nbt.putBoolean("bleeding", bleeding);
        nbt.putBoolean("consumed", itemConsumed);
        nbt.putBoolean("selfRevive", selfReviving);
        return nbt;
    }

    @Override
    public void deserializeNBT(NbtCompound nbt) {
        timeLeft = nbt.getInt("timeLeft");
        progress = nbt.getFloat("progress");
        bleeding = nbt.getBoolean("bleeding");
        itemConsumed = nbt.getBoolean("consumed");
        selfReviving = nbt.getBoolean("selfRevive");
    }

    @Override
    public boolean isBleeding() {
        return bleeding;
    }

    @Override
    public void knockOut(PlayerEntity player, DamageSource source) {
        this.bleeding = true;
        this.progress = 0;
        this.downedTime = 0;
        this.timeLeft = PlayerReviveFabric.CONFIG.bleeding.bleedTime;
        this.lastSource = source;
        this.trackerClone = new DamageTrackerClone(player.getDamageTracker());
        if (PlayerReviveFabric.CONFIG.bleeding.disableJump)
            player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH).addTemporaryModifier(
                    new EntityAttributeModifier(JUMP_HEIGHT, -1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override
    public void revive(PlayerEntity player) {
        this.bleeding = false;
        this.progress = 0;
        this.timeLeft = 0;
        this.downedTime = 0;
        this.lastSource = null;
        this.trackerClone = null;
        this.itemConsumed = false;
        this.selfReviving = false;
        if (player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH) != null)
            player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH).removeModifier(JUMP_HEIGHT);
    }

    @Override
    public int timeLeft() {
        return timeLeft;
    }

    @Override
    public List<PlayerEntity> revivingPlayers() {
        return revivingPlayers;
    }

    @Override
    public DamageTrackerClone getTrackerClone() {
        return trackerClone;
    }

    @Override
    public DamageSource getSource(DynamicRegistryManager access) {
        if (lastSource != null)
            return lastSource;
        return new DamageSource(access.get(RegistryKeys.DAMAGE_TYPE).entryOf(PlayerReviveFabric.BLED_TO_DEATH));
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
