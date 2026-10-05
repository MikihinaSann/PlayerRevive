package team.creative.playerrevive.api;

import java.util.List;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface IBleeding {

    public void tick(Player player);

    public float getProgress();

    public boolean isBleeding();

    public boolean bledOut();

    public void forceBledOut();

    public void knockOut(Player player, DamageSource source);

    public boolean revived();

    public void revive(Player player);

    public int timeLeft();

    public int downedTime();

    public List<Player> revivingPlayers();

    public DamageSource getSource(RegistryAccess access);

    public CombatTrackerClone getTrackerClone();

    public boolean isItemConsumed();

    public void setItemConsumed();

    public void startSelfRevive();

    public boolean isSelfReviving();

    public void serialize(ValueOutput output);

    public void deserialize(ValueInput input);

}
