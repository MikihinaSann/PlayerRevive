package team.creative.playerrevive.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageTracker;

@Mixin(DamageTracker.class)
public interface DamageTrackerAccessor {

    @Accessor("recentDamage")
    List<DamageRecord> getEntries();

    @Accessor("ageOnLastDamage")
    int getLastDamageTime();

    @Accessor("ageOnLastDamage")
    void setLastDamageTime(int value);

    @Accessor("ageOnLastAttacked")
    int getCombatStartTime();

    @Accessor("ageOnLastAttacked")
    void setCombatStartTime(int value);

    @Accessor("ageOnLastUpdate")
    int getCombatEndTime();

    @Accessor("ageOnLastUpdate")
    void setCombatEndTime(int value);

    @Accessor("recentlyAttacked")
    boolean getInCombat();

    @Accessor("recentlyAttacked")
    void setInCombat(boolean value);

    @Accessor("hasDamage")
    boolean getTakingDamage();

    @Accessor("hasDamage")
    void setTakingDamage(boolean value);

}
