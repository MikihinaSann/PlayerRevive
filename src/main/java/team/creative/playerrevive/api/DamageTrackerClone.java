package team.creative.playerrevive.api;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageTracker;
import team.creative.playerrevive.mixin.DamageTrackerAccessor;

public class DamageTrackerClone {

    private final List<DamageRecord> combatEntries = Lists.<DamageRecord>newArrayList();
    private int lastDamageTime;
    private int combatStartTime;
    private int combatEndTime;
    private boolean inCombat;
    private boolean takingDamage;

    public DamageTrackerClone(DamageTracker tracker) {
        DamageTrackerAccessor ac = (DamageTrackerAccessor) tracker;
        combatEntries.addAll(ac.getEntries());
        lastDamageTime = ac.getLastDamageTime();
        combatStartTime = ac.getCombatStartTime();
        combatEndTime = ac.getCombatEndTime();
        inCombat = ac.getInCombat();
        takingDamage = ac.getTakingDamage();
    }

    public void overwriteTracker(DamageTracker tracker) {
        DamageTrackerAccessor ac = (DamageTrackerAccessor) tracker;
        List<DamageRecord> entries = ac.getEntries();
        entries.clear();
        entries.addAll(combatEntries);
        ac.setLastDamageTime(lastDamageTime);
        ac.setCombatStartTime(combatStartTime);
        ac.setCombatEndTime(combatEndTime);
        ac.setInCombat(inCombat);
        ac.setTakingDamage(takingDamage);
    }

}
