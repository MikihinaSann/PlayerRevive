package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.BleedingHolder;
import team.creative.playerrevive.api.PlayerExtender;
import team.creative.playerrevive.cap.Bleeding;
import team.creative.playerrevive.server.PlayerReviveServer;
import team.creative.playerrevive.server.ReviveEventServer;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements PlayerExtender {

    @Unique
    private float overkill;

    @Inject(method = "getPermissionLevel", at = @At("HEAD"), require = 1, cancellable = true)
    public void getPermissionLevel(CallbackInfoReturnable<Integer> callback) {
        if (PlayerReviveFabric.CONFIG.bleeding.changePermissionLevel && PlayerReviveServer.isBleeding((ServerPlayerEntity) (Object) this))
            callback.setReturnValue(PlayerReviveFabric.CONFIG.bleeding.permissionLevel);
    }

    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (ReviveEventServer.onPlayerDeath(player, source)) {
            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (ReviveEventServer.onPlayerDamaged(player, source)) {
            cir.setReturnValue(false);
        }
    }

    // Save bleeding state to NBT (like NeoForge AttachmentType.serializable)
    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void onSave(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Bleeding bleeding = ((BleedingHolder) player).playerrevive$getBleeding();
        if (bleeding != null && bleeding.isBleeding()) {
            nbt.put("playerrevive:bleeding", bleeding.serializeNBT());
        }
    }

    // Restore bleeding state from NBT (like NeoForge AttachmentType.serializable)
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void onLoad(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (nbt.contains("playerrevive:bleeding")) {
            Bleeding bleeding = new Bleeding();
            bleeding.deserializeNBT(nbt.getCompound("playerrevive:bleeding"));
            ((BleedingHolder) player).playerrevive$setBleeding(bleeding);
        }
    }

    @Override
    public float getOverkill() {
        return overkill;
    }

    @Override
    public void setOverkill(float overkill) {
        this.overkill = overkill;
    }

}
