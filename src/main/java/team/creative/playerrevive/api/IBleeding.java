package team.creative.playerrevive.api;

import java.util.List;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public interface IBleeding {

    void tick(PlayerEntity player);

    float getProgress();

    boolean isBleeding();

    boolean bledOut();

    void forceBledOut();

    void knockOut(PlayerEntity player, DamageSource source);

    boolean revived();

    void revive(PlayerEntity player);

    int timeLeft();

    int downedTime();

    List<PlayerEntity> revivingPlayers();

    DamageSource getSource(DynamicRegistryManager access);

    DamageTrackerClone getTrackerClone();

    boolean isItemConsumed();

    void setItemConsumed();

    void startSelfRevive();

    boolean isSelfReviving();

    NbtCompound serializeNBT();

    void deserializeNBT(NbtCompound nbt);
}
