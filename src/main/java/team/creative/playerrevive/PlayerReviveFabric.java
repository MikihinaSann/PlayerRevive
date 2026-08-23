package team.creative.playerrevive;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.entity.damage.DamageType;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.playerrevive.packet.GiveUpPacket;
import team.creative.playerrevive.packet.HelperPacket;
import team.creative.playerrevive.packet.ReviveUpdatePacket;
import team.creative.playerrevive.packet.StartSelfRevivePacket;
import team.creative.playerrevive.server.PlayerReviveServer;
import team.creative.playerrevive.server.ReviveEventServer;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class PlayerReviveFabric implements ModInitializer {

    public static final String MODID = "playerrevive";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static PlayerReviveConfig CONFIG;
    public static final CreativeNetwork NETWORK = new CreativeNetwork(1, LOGGER, Identifier.of(MODID, "main"));

    public static final Identifier BLEEDING_NAME = Identifier.of(MODID, "bleeding");
    public static final RegistryKey<DamageType> BLED_TO_DEATH = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MODID, "bled_to_death"));

    public static final SoundEvent DEATH_SOUND = SoundEvent.of(Identifier.of(MODID, "death"));
    public static final SoundEvent REVIVED_SOUND = SoundEvent.of(Identifier.of(MODID, "revived"));

    @Override
    public void onInitialize() {
        // Initialize tracked data for bleeding state
        PlayerReviveServer.initTrackedData();

        // Register sounds
        Registry.register(Registries.SOUND_EVENT, Identifier.of(MODID, "death"), DEATH_SOUND);
        Registry.register(Registries.SOUND_EVENT, Identifier.of(MODID, "revived"), REVIVED_SOUND);

        // Register packets
        NETWORK.registerType(ReviveUpdatePacket.class, ReviveUpdatePacket::new);
        NETWORK.registerType(HelperPacket.class, HelperPacket::new);
        NETWORK.registerType(GiveUpPacket.class, GiveUpPacket::new);
        NETWORK.registerType(StartSelfRevivePacket.class, StartSelfRevivePacket::new);

        // Register config
        CreativeConfigRegistry.ROOT.registerValue(MODID, CONFIG = new PlayerReviveConfig());

        // Register server events
        ReviveEventServer.register();

        // Register entity selector option via reflection (putOption is private)
        try {
            java.lang.reflect.Method putOption = net.minecraft.command.EntitySelectorOptions.class.getDeclaredMethod("putOption",
                    String.class, net.minecraft.command.EntitySelectorOptions.SelectorHandler.class,
                    java.util.function.Predicate.class, Text.class);
            putOption.setAccessible(true);
            putOption.invoke(null, "bleeding",
                    (net.minecraft.command.EntitySelectorOptions.SelectorHandler) reader -> {
                        boolean value = reader.getReader().readBoolean();
                        reader.addPredicate(entity -> {
                            boolean entityValue = false;
                            if (entity instanceof PlayerEntity p) {
                                var bleeding = PlayerReviveServer.getBleeding(p);
                                entityValue = bleeding.isBleeding();
                            }
                            return entityValue == value;
                        });
                    },
                    (java.util.function.Predicate<net.minecraft.command.EntitySelectorReader>) reader -> true,
                    Text.translatable("argument.entity.options.bleeding.description"));
        } catch (Exception e) {
            LOGGER.error("Failed to register entity selector option", e);
        }

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("revive").requires(src -> src.hasPermissionLevel(2)).then(argument("players", EntityArgumentType.players()).executes(ctx -> {
                Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "players");
                for (ServerPlayerEntity player : players)
                    if (PlayerReviveServer.getBleeding(player).isBleeding())
                        PlayerReviveServer.revive(player);
                return 0;
            })));
        });
    }
}
