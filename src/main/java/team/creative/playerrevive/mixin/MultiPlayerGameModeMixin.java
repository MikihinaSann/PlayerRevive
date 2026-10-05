package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import team.creative.playerrevive.api.BleedingHolder;
import team.creative.playerrevive.cap.Bleeding;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void playerrevive$startDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (isBleeding())
            cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void playerrevive$continueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (isBleeding())
            cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void playerrevive$attack(Player player, Entity target, CallbackInfo ci) {
        if (isBleeding())
            ci.cancel();
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void playerrevive$useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (isBleeding())
            cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void playerrevive$useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (isBleeding())
            cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void playerrevive$interact(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (isBleeding())
            cir.setReturnValue(InteractionResult.PASS);
    }

    private static boolean isBleeding() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive())
            return false;
        Bleeding bleeding = ((BleedingHolder) mc.player).playerrevive$getBleeding();
        return bleeding != null && bleeding.isBleeding();
    }
}
