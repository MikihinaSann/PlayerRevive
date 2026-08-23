package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.server.PlayerReviveServer;
import team.creative.playerrevive.server.ReviveEventServer;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void onCanBeSeenAsEnemy(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            IBleeding bleeding = PlayerReviveServer.getBleeding(player);
            if (bleeding.isBleeding() && (bleeding.downedTime() <= PlayerReviveFabric.CONFIG.bleeding.initialDamageCooldown ||
                    PlayerReviveFabric.CONFIG.bleeding.disableMobDamage))
                cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void onIsPushable(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            IBleeding bleeding = PlayerReviveServer.getBleeding(player);
            if (bleeding.isBleeding() && !PlayerReviveFabric.CONFIG.bleeding.canBePushed)
                cir.setReturnValue(false);
        }
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V", ordinal = 0))
    private void onDamageApply(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            ReviveEventServer.onPlayerDamagePre(player, amount);
        }
    }
}
