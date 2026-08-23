package team.creative.playerrevive.packet;

import java.util.UUID;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityPose;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import team.creative.creativecore.common.network.CreativePacket;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.server.PlayerReviveServer;

public class ReviveUpdatePacket extends CreativePacket {

    public UUID uuid;
    public NbtCompound nbt;

    public ReviveUpdatePacket(PlayerEntity player) {
        this.nbt = PlayerReviveServer.getBleeding(player).serializeNBT();
        this.uuid = player.getUuid();
    }

    public ReviveUpdatePacket() {

    }

    @Override
    public void executeClient(PlayerEntity player) {
        PlayerEntity member = MinecraftClient.getInstance().world.getPlayerByUuid(uuid);
        if (member != null) {
            IBleeding bleeding = PlayerReviveServer.getBleeding(member);
            bleeding.deserializeNBT(nbt);
            if (!bleeding.isBleeding() && member.isAlive())
                member.setPose(EntityPose.STANDING);
        }
    }

    @Override
    public void executeServer(ServerPlayerEntity player) {

    }

}
