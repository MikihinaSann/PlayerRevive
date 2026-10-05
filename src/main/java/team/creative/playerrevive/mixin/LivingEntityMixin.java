package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import team.creative.playerrevive.server.ReviveEventServer;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void playerrevive$hurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            ReviveEventServer.onPlayerDamagePre(player, amount);
            if (ReviveEventServer.onPlayerDamaged(player, source))
                cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void playerrevive$die(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof Player player && ReviveEventServer.onPlayerDeath(player, source))
            ci.cancel();
    }
}
