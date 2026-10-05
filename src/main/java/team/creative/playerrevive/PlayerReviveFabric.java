package team.creative.playerrevive;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.playerrevive.packet.GiveUpPacket;
import team.creative.playerrevive.packet.HelperPacket;
import team.creative.playerrevive.packet.ReviveUpdatePacket;
import team.creative.playerrevive.packet.StartSelfRevivePacket;
import team.creative.playerrevive.server.PlayerReviveServer;
import team.creative.playerrevive.server.ReviveEventServer;

public class PlayerReviveFabric implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger(PlayerReviveFabric.MODID);
    public static final String MODID = "playerrevive";
    public static PlayerReviveConfig CONFIG;
    public static final CreativeNetwork NETWORK = new CreativeNetwork(1, LOGGER, Identifier.tryBuild(PlayerReviveFabric.MODID, "main"));

    public static final Identifier BLEEDING_NAME = Identifier.tryBuild(MODID, "bleeding");
    public static final ResourceKey<DamageType> BLED_TO_DEATH = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.tryBuild(MODID, "bled_to_death"));

    public static final SoundEvent DEATH_SOUND = SoundEvent.createVariableRangeEvent(Identifier.tryBuild(MODID, "death"));
    public static final SoundEvent REVIVED_SOUND = SoundEvent.createVariableRangeEvent(Identifier.tryBuild(MODID, "revived"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.tryBuild(MODID, "death"), DEATH_SOUND);
        Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.tryBuild(MODID, "revived"), REVIVED_SOUND);

        PlayerReviveServer.initTrackedData();

        NETWORK.registerType(ReviveUpdatePacket.class, ReviveUpdatePacket::new);
        NETWORK.registerType(HelperPacket.class, HelperPacket::new);
        NETWORK.registerType(GiveUpPacket.class, GiveUpPacket::new);
        NETWORK.registerType(StartSelfRevivePacket.class, StartSelfRevivePacket::new);

        CreativeConfigRegistry.ROOT.registerValue(MODID, CONFIG = new PlayerReviveConfig());

        ReviveEventServer.register();

        registerSelectorOption();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("revive").requires(x -> x.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("players",
                EntityArgument.players()).executes(x -> {
                    Collection<ServerPlayer> players = EntityArgument.getPlayers(x, "players");
                    for (ServerPlayer player : players)
                        if (PlayerReviveServer.getBleeding(player).isBleeding())
                            PlayerReviveServer.revive(player);
                    return 0;
                })));
        });
    }

    private void registerSelectorOption() {
        try {
            Method register = EntitySelectorOptions.class.getDeclaredMethod("register", String.class, EntitySelectorOptions.Modifier.class, Predicate.class, Component.class);
            register.setAccessible(true);
            register.invoke(null, "bleeding", (EntitySelectorOptions.Modifier) reader -> {
                boolean value = reader.getReader().readBoolean();
                reader.addPredicate(entity -> {
                    boolean entityValue = false;
                    if (entity instanceof Player p)
                        entityValue = PlayerReviveServer.getBleeding(p).isBleeding();
                    return entityValue == value;
                });
            }, (Predicate<EntitySelectorParser>) reader -> true, Component.translatable("argument.entity.options.bleeding.description"));
        } catch (Exception e) {
            LOGGER.error("Failed to register entity selector option", e);
        }
    }

}
