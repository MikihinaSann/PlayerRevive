package team.creative.playerrevive.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import team.creative.creativecore.common.config.premade.MobEffectConfig;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.PlayerReviveConfig.DamageTypeConfig;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.api.PlayerExtender;
import team.creative.playerrevive.api.event.PlayerReviveEvents;
import team.creative.playerrevive.packet.HelperPacket;

public class ReviveEventServer {

    public static boolean isReviveActive(Entity player) {
        if (player instanceof Player p && p.isCreative() && !PlayerReviveFabric.CONFIG.bleeding.triggerForCreative)
            return false;
        return PlayerReviveFabric.CONFIG.bleedInSingleplayer || player.level().getServer().isPublished();
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                if (!isReviveActive(player) || !player.isAlive())
                    continue;
                IBleeding revive = PlayerReviveServer.getBleeding(player);

                if (revive.isBleeding()) {
                    revive.tick(player);

                    if (revive.downedTime() % 5 == 0)
                        PlayerReviveServer.sendUpdatePacket(player);

                    if (PlayerReviveFabric.CONFIG.bleeding.affectHunger)
                        player.getFoodData().setFoodLevel(PlayerReviveFabric.CONFIG.bleeding.remainingHunger);

                    for (MobEffectConfig effect : PlayerReviveFabric.CONFIG.bleeding.bleedingEffects)
                        player.addEffect(effect.create());

                    if (PlayerReviveFabric.CONFIG.bleeding.shouldGlow)
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10));

                    if (revive.revived())
                        PlayerReviveServer.revive(player);
                    else if (revive.bledOut())
                        PlayerReviveServer.kill(player);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            IBleeding revive = PlayerReviveServer.getBleeding(player);
            if (revive.isBleeding())
                PlayerReviveServer.kill(player);
            if (!player.level().isClientSide())
                PlayerReviveServer.removePlayerAsHelper(player);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            IBleeding revive = PlayerReviveServer.getBleeding(player);
            if (revive.isBleeding()) {
                player.setHealth(PlayerReviveFabric.CONFIG.bleeding.bleedingHealth);
                if (PlayerReviveFabric.CONFIG.bleeding.affectHunger)
                    player.getFoodData().setFoodLevel(PlayerReviveFabric.CONFIG.bleeding.remainingHunger);
                PlayerReviveServer.sendUpdatePacket(player);
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof Player && !player.level().isClientSide()) {
                Player target = (Player) entity;
                Player helper = player;
                IBleeding revive = PlayerReviveServer.getBleeding(target);
                if (revive.isBleeding()) {
                    if (PlayerReviveFabric.CONFIG.revive.teammatesOnly && !helper.getTeam().isAlliedTo(target.getTeam())) {
                        helper.sendSystemMessage(Component.translatable("playerrevive.revive.other_team"));
                        return InteractionResult.FAIL;
                    }

                    if (PlayerReviveFabric.CONFIG.revive.needReviveItem) {
                        if (PlayerReviveFabric.CONFIG.revive.consumeReviveItem && !revive.isItemConsumed()) {
                            if (PlayerReviveFabric.CONFIG.revive.reviveItem.is(helper.level(), helper.getMainHandItem()) && helper.getMainHandItem()
                                    .getCount() >= PlayerReviveFabric.CONFIG.revive.reviveItemCount) {
                                if (!helper.isCreative()) {
                                    helper.getMainHandItem().shrink(PlayerReviveFabric.CONFIG.revive.reviveItemCount);
                                    helper.getInventory().setChanged();
                                }
                                revive.setItemConsumed();
                            } else {
                                if (!helper.level().isClientSide())
                                    helper.sendSystemMessage(Component.translatable("playerrevive.revive.item", PlayerReviveFabric.CONFIG.revive.reviveItem.description()));
                                return InteractionResult.FAIL;
                            }
                        } else if (!PlayerReviveFabric.CONFIG.revive.reviveItem.is(helper.level(), helper.getMainHandItem()))
                            return InteractionResult.PASS;
                    }

                    PlayerReviveServer.removePlayerAsHelper(helper);
                    revive.revivingPlayers().add(helper);
                    PlayerReviveEvents.fireReviveStart(helper, target);
                    PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(target.getUUID(), true), (ServerPlayer) helper);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });
    }

    public static boolean onCommand(CommandSourceStack source) {
        if (PlayerReviveFabric.CONFIG.bleeding.disableServerCommands && source.isPlayer() && PlayerReviveServer.getBleeding(source.getPlayer()).isBleeding()) {
            source.getPlayer().sendSystemMessage(Component.translatable("playerrevive.chat.no_commands"));
            return true;
        }
        return false;
    }

    private static boolean doesByPass(Player player, DamageSource source) {
        if (source.typeHolder().is(PlayerReviveFabric.BLED_TO_DEATH))
            return true;
        if (PlayerReviveFabric.CONFIG.bypassDamageSources.contains(source.getMsgId()))
            return true;
        if (PlayerReviveFabric.CONFIG.bypassDamageSources.contains(source.typeHolder().getRegisteredName()))
            return true;

        return false;
    }

    private static boolean doesByPassDamageAmount(Player player, DamageSource source) {
        if (!PlayerReviveFabric.CONFIG.enableBypassDamage)
            return false;
        var amount = ((PlayerExtender) player).getOverkill();
        if (PlayerReviveFabric.CONFIG.bypassDamage <= amount)
            return true;
        for (DamageTypeConfig d : PlayerReviveFabric.CONFIG.bypassSourceByDamage) {
            if (d.damageAmount > amount)
                continue;
            if (d.damageType.equals(source.getMsgId()) || d.damageType.equals(source.typeHolder().getRegisteredName()))
                return true;
        }
        return false;
    }

    /** Called from LivingEntityMixin when a player takes damage. Return true to cancel the damage. */
    public static boolean onPlayerDamaged(Player player, DamageSource source) {
        IBleeding revive = PlayerReviveServer.getBleeding(player);
        if (revive.isBleeding()) {
            if (doesByPass(player, source))
                return false;

            if (revive.bledOut())
                return true;

            if (revive.downedTime() <= PlayerReviveFabric.CONFIG.bleeding.initialDamageCooldown)
                return true;

            if (source.getEntity() instanceof Player) {
                if (PlayerReviveFabric.CONFIG.bleeding.disablePlayerDamage)
                    return true;
            } else if (source.getEntity() instanceof LivingEntity) {
                if (PlayerReviveFabric.CONFIG.bleeding.disableMobDamage)
                    return true;
            } else if (PlayerReviveFabric.CONFIG.bleeding.disableOtherDamage)
                return true;

        } else if (PlayerReviveFabric.CONFIG.revive.abortOnDamage)
            PlayerReviveServer.removePlayerAsHelper(player);

        return false;
    }

    /** Called from LivingEntityMixin before damage is applied, tracks overkill damage. */
    public static void onPlayerDamagePre(Player player, float newDamage) {
        if (!player.level().isClientSide() && player instanceof PlayerExtender extender && isReviveActive(player))
            extender.setOverkill(Math.max(0, newDamage - player.getHealth()));
    }

    /** Called from LivingEntityMixin when a player dies. Return true to cancel the death. */
    public static boolean onPlayerDeath(Player player, DamageSource source) {
        if (player.level().isClientSide() || !isReviveActive(player))
            return false;

        if (doesByPass(player, source) || doesByPassDamageAmount(player, source))
            return false;

        IBleeding revive = PlayerReviveServer.getBleeding(player);

        if (revive.bledOut() || revive.isBleeding()) {
            if (revive.isBleeding())
                PlayerReviveFabric.CONFIG.sounds.death.play(player, SoundSource.PLAYERS);
            for (Player helper : revive.revivingPlayers())
                PlayerReviveServer.cancelHelper(player, helper);
            revive.revivingPlayers().clear();
            return false;
        }

        PlayerReviveServer.removePlayerAsHelper(player);
        PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayer) player);

        PlayerReviveServer.startBleeding(player, source);

        if (player.isPassenger())
            player.stopRiding();

        if (PlayerReviveFabric.CONFIG.bleeding.affectHunger)
            player.getFoodData().setFoodLevel(PlayerReviveFabric.CONFIG.bleeding.remainingHunger);

        player.setHealth(PlayerReviveFabric.CONFIG.bleeding.bleedingHealth);

        if (PlayerReviveFabric.CONFIG.bleeding.bleedingMessage)
            if (PlayerReviveFabric.CONFIG.bleeding.bleedingMessageTrackingOnly) {
                if (player.level().getChunkSource() instanceof ServerChunkCache chunkCache)
                    chunkCache.sendToTrackingPlayersAndSelf(player, new ClientboundSystemChatPacket(Component.translatable("playerrevive.chat.bleeding", player
                            .getDisplayName()), false));
            } else
                player.level().getServer().getPlayerList().broadcastSystemMessage(Component.translatable("playerrevive.chat.bleeding", player.getDisplayName()), false);

        return true;
    }
}
