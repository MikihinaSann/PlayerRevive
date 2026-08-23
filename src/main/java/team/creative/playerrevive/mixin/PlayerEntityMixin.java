package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import team.creative.playerrevive.api.BleedingHolder;
import team.creative.playerrevive.cap.Bleeding;
import team.creative.playerrevive.server.PlayerReviveServer;
import team.creative.playerrevive.server.ReviveEventServer;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements BleedingHolder {

    @Unique
    private Bleeding playerrevive$bleeding;

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initDataTrackerBleeding(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(PlayerReviveServer.BLEEDING_TRACKER, false);
    }

    @Override
    public Bleeding playerrevive$getBleeding() {
        return playerrevive$bleeding;
    }

    @Override
    public void playerrevive$setBleeding(Bleeding bleeding) {
        this.playerrevive$bleeding = bleeding;
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (ReviveEventServer.onPlayerDamaged(player, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (ReviveEventServer.onPlayerDeath(player, source)) {
            ci.cancel();
        }
    }

    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void onUpdatePose(CallbackInfo ci) {
        Bleeding bleeding = this.playerrevive$getBleeding();
        if (bleeding != null && bleeding.isBleeding()) {
            ci.cancel();
        }
    }
}
