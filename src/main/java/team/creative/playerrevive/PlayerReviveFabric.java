package team.creative.playerrevive;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.EntitySelectorOptionRegistry;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.playerrevive.packet.GiveUpPacket;
import team.creative.playerrevive.packet.HelperPacket;
import team.creative.playerrevive.packet.ReviveUpdatePacket;
import team.creative.playerrevive.server.PlayerReviveServer;
import team.creative.playerrevive.server.ReviveEventServer;

public class PlayerReviveFabric implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger(PlayerReviveFabric.MODID);
    public static final String MODID = "playerrevive";
    public static PlayerReviveConfig CONFIG;
    public static final CreativeNetwork NETWORK = new CreativeNetwork(1, LOGGER, ResourceLocation.tryBuild(MODID, "main"));

    public static final ResourceLocation BLEEDING_NAME = ResourceLocation.tryBuild(MODID, "bleeding");
    public static final ResourceKey<DamageType> BLED_TO_DEATH = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryBuild(MODID, "bled_to_death"));

    public static final SoundEvent DEATH_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(MODID, "death"));
    public static final SoundEvent REVIVED_SOUND = SoundEvent.createVariableRangeEvent(ResourceLocation.tryBuild(MODID, "revived"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ResourceLocation.tryBuild(MODID, "death"), DEATH_SOUND);
        Registry.register(BuiltInRegistries.SOUND_EVENT, ResourceLocation.tryBuild(MODID, "revived"), REVIVED_SOUND);

        NETWORK.registerType(ReviveUpdatePacket.class, ReviveUpdatePacket::new);
        NETWORK.registerType(HelperPacket.class, HelperPacket::new);
        NETWORK.registerType(GiveUpPacket.class, GiveUpPacket::new);

        CreativeConfigRegistry.ROOT.registerValue(MODID, CONFIG = new PlayerReviveConfig());

        ReviveEventServer.register();

        EntitySelectorOptionRegistry.register(BLEEDING_NAME, Component.translatable("argument.entity.options.bleeding.description"), x -> {
            boolean value = x.getReader().readBoolean();
            x.addPredicate(entity -> {
                var entityValue = false;
                if (entity instanceof Player p)
                    entityValue = PlayerReviveServer.getBleeding(p).isBleeding();
                return entityValue == value;
            });
        }, x -> true);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.commands.Commands.literal("revive").requires(x -> x.hasPermission(2)).then(net.minecraft.commands.Commands
                    .argument("players", EntityArgument.players()).executes(x -> {
                        Collection<ServerPlayer> players = EntityArgument.getPlayers(x, "players");
                        for (ServerPlayer player : players)
                            if (PlayerReviveServer.getBleeding(player).isBleeding())
                                PlayerReviveServer.revive(player);
                        return 0;
                    })));
        });
    }
}
