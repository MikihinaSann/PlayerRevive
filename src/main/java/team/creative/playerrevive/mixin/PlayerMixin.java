package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.BleedingHolder;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.cap.Bleeding;
import team.creative.playerrevive.server.PlayerReviveServer;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements BleedingHolder {

    @Unique
    private Bleeding playerrevive$bleeding;

    protected PlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Override
    public Bleeding playerrevive$getBleeding() {
        return playerrevive$bleeding;
    }

    @Override
    public void playerrevive$setBleeding(Bleeding bleeding) {
        this.playerrevive$bleeding = bleeding;
    }

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/world/entity/player/Player;canBeSeenAsEnemy()Z", cancellable = true)
    public void isBleeding(CallbackInfoReturnable<Boolean> cir) {
        IBleeding bleeding = PlayerReviveServer.getBleeding((Player) (Object) this);
        if (bleeding.isBleeding() && (bleeding.downedTime() <= PlayerReviveFabric.CONFIG.bleeding.initialDamageCooldown || PlayerReviveFabric.CONFIG.bleeding.disableMobDamage))
            cir.setReturnValue(false);
    }

    @Override
    protected float getJumpPower(float power) {
        IBleeding bleeding = PlayerReviveServer.getBleeding((Player) (Object) this);
        if (bleeding.isBleeding())
            return 0;
        return super.getJumpPower(power);
    }

    @Override
    public boolean isPushable() {
        if (super.isPushable()) {
            if (PlayerReviveServer.getBleeding((Player) (Object) this).isBleeding() && !PlayerReviveFabric.CONFIG.bleeding.canBePushed)
                return false;
            return true;
        }
        return false;
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void playerrevive$defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(PlayerReviveServer.BLEEDING_TRACKER, false);
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void playerrevive$updatePlayerPose(CallbackInfo ci) {
        if (this.playerrevive$bleeding != null && this.playerrevive$bleeding.isBleeding())
            ci.cancel();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void playerrevive$save(ValueOutput output, CallbackInfo ci) {
        if (this.playerrevive$bleeding != null && this.playerrevive$bleeding.isBleeding())
            this.playerrevive$bleeding.serialize(output.child("playerrevive:bleeding"));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void playerrevive$load(ValueInput input, CallbackInfo ci) {
        input.child("playerrevive:bleeding").ifPresent(child -> {
            Bleeding bleeding = new Bleeding();
            bleeding.deserialize(child);
            this.playerrevive$bleeding = bleeding;
        });
    }

}
