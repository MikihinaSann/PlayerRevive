package team.creative.playerrevive.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.entity.damage.DamageSource;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.ActionResult;
import team.creative.creativecore.common.config.premade.MobEffectConfig;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.PlayerReviveConfig.DamageTypeConfig;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.api.PlayerExtender;
import team.creative.playerrevive.api.event.PlayerReviveEvents;
import team.creative.playerrevive.packet.HelperPacket;

public class ReviveEventServer {

    public static boolean isReviveActive(Entity player) {
        if (player instanceof PlayerEntity p && p.isCreative() && !PlayerReviveFabric.CONFIG.bleeding.triggerForCreative)
            return false;
        return PlayerReviveFabric.CONFIG.bleedInSingleplayer || player.getServer().isRemote();
    }

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.isAlive())
                    continue;
                if (!isReviveActive(player))
                    continue;
                IBleeding revive = PlayerReviveServer.getBleeding(player);

                if (revive.isBleeding()) {
                    revive.tick(player);

                    if (revive.downedTime() % 5 == 0)
                        PlayerReviveServer.sendUpdatePacket(player);

                    if (PlayerReviveFabric.CONFIG.bleeding.affectHunger)
                        player.getHungerManager().setFoodLevel(PlayerReviveFabric.CONFIG.bleeding.remainingHunger);

                    for (MobEffectConfig effect : PlayerReviveFabric.CONFIG.bleeding.bleedingEffects)
                        player.addStatusEffect(effect.create());

                    if (PlayerReviveFabric.CONFIG.bleeding.shouldGlow)
                        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.GLOWING, 10));

                    if (revive.revived())
                        PlayerReviveServer.revive(player);
                    else if (revive.bledOut())
                        PlayerReviveServer.kill(player);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            IBleeding revive = PlayerReviveServer.getBleeding(player);
            if (revive.isBleeding() && !server.isStopping())
                PlayerReviveServer.kill(player);
            PlayerReviveServer.removePlayerAsHelper(player);
        });

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            IBleeding revive = PlayerReviveServer.getBleeding(player);
            if (revive.isBleeding()) {
                player.setHealth(PlayerReviveFabric.CONFIG.bleeding.bleedingHealth);
                if (PlayerReviveFabric.CONFIG.bleeding.affectHunger)
                    player.getHungerManager().setFoodLevel(PlayerReviveFabric.CONFIG.bleeding.remainingHunger);
                PlayerReviveServer.sendUpdatePacket(player);
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof PlayerEntity && !world.isClient) {
                PlayerEntity target = (PlayerEntity) entity;
                PlayerEntity helper = player;
                IBleeding revive = PlayerReviveServer.getBleeding(target);
                if (revive.isBleeding()) {
                    if (PlayerReviveFabric.CONFIG.revive.teammatesOnly && !helper.isTeammate(target)) {
                        helper.sendMessage(Text.translatable("playerrevive.revive.other_team"));
                        return ActionResult.FAIL;
                    }

                    if (PlayerReviveFabric.CONFIG.revive.needReviveItem) {
                        if (PlayerReviveFabric.CONFIG.revive.consumeReviveItem && !revive.isItemConsumed()) {
                            if (PlayerReviveFabric.CONFIG.revive.reviveItem.is(helper.getMainHandStack()) && helper.getMainHandStack()
                                    .getCount() >= PlayerReviveFabric.CONFIG.revive.reviveItemCount) {
                                if (!helper.isCreative()) {
                                    helper.getMainHandStack().decrement(PlayerReviveFabric.CONFIG.revive.reviveItemCount);
                                    helper.getInventory().markDirty();
                                }
                                revive.setItemConsumed();
                            } else {
                                helper.sendMessage(Text.translatable("playerrevive.revive.item", PlayerReviveFabric.CONFIG.revive.reviveItemCount,
                                        PlayerReviveFabric.CONFIG.revive.reviveItem.description()));
                                return ActionResult.FAIL;
                            }
                        } else if (!PlayerReviveFabric.CONFIG.revive.reviveItem.is(helper.getMainHandStack()))
                            return ActionResult.PASS;
                    }

                    PlayerReviveServer.removePlayerAsHelper(helper);
                    revive.revivingPlayers().add(helper);
                    PlayerReviveEvents.fireReviveStart(helper, target);
                    PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(target.getUuid(), true), (ServerPlayerEntity) helper);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    public static boolean doesByPass(PlayerEntity player, DamageSource source) {
        if (source.isOf(PlayerReviveFabric.BLED_TO_DEATH))
            return true;
        String name = source.getName();
        if (PlayerReviveFabric.CONFIG.bypassDamageSources.contains(name))
            return true;
        // Only do expensive lookup if name didn't match
        var key = source.getTypeRegistryEntry().getKey();
        if (key.isPresent() && PlayerReviveFabric.CONFIG.bypassDamageSources.contains(key.get().getValue().toString()))
            return true;
        return false;
    }

    public static boolean doesByPassDamageAmount(PlayerEntity player, DamageSource source) {
        if (!PlayerReviveFabric.CONFIG.enableBypassDamage)
            return false;
        float amount = ((PlayerExtender) player).getOverkill();
        if (PlayerReviveFabric.CONFIG.bypassDamage <= amount)
            return true;
        String name = source.getName();
        String fullId = null;
        for (DamageTypeConfig d : PlayerReviveFabric.CONFIG.bypassSourceByDamage) {
            if (d.damageAmount > amount)
                continue;
            if (d.damageType.equals(name))
                return true;
            if (fullId == null) {
                var key = source.getTypeRegistryEntry().getKey();
                fullId = key.isPresent() ? key.get().getValue().toString() : "";
            }
            if (d.damageType.equals(fullId))
                return true;
        }
        return false;
    }

    public static boolean onPlayerDamaged(PlayerEntity player, DamageSource source) {
        IBleeding revive = PlayerReviveServer.getBleeding(player);
        if (revive.isBleeding()) {
            if (doesByPass(player, source))
                return false;

            if (revive.bledOut())
                return true;

            if (revive.downedTime() <= PlayerReviveFabric.CONFIG.bleeding.initialDamageCooldown)
                return true;

            if (source.getAttacker() instanceof PlayerEntity) {
                if (PlayerReviveFabric.CONFIG.bleeding.disablePlayerDamage)
                    return true;
            } else if (source.getAttacker() instanceof LivingEntity) {
                if (PlayerReviveFabric.CONFIG.bleeding.disableMobDamage)
                    return true;
            } else if (PlayerReviveFabric.CONFIG.bleeding.disableOtherDamage)
                return true;

        } else if (PlayerReviveFabric.CONFIG.revive.abortOnDamage)
            PlayerReviveServer.removePlayerAsHelper(player);

        return false;
    }

    public static void onPlayerDamagePre(PlayerEntity player, float newDamage) {
        if (player instanceof PlayerExtender extender && isReviveActive(player))
            extender.setOverkill(Math.max(0, newDamage - player.getHealth()));
    }

    public static boolean onPlayerDeath(PlayerEntity player, DamageSource source) {
        if (!isReviveActive(player))
            return false;

        if (doesByPass(player, source) || doesByPassDamageAmount(player, source))
            return false;

        IBleeding revive = PlayerReviveServer.getBleeding(player);

        if (revive.bledOut() || revive.isBleeding()) {
            if (revive.isBleeding())
                PlayerReviveFabric.CONFIG.sounds.death.play(player, SoundCategory.PLAYERS);
            for (PlayerEntity helper : revive.revivingPlayers())
                PlayerReviveServer.cancelHelper(player, helper);
            revive.revivingPlayers().clear();
            return false;
        }

        PlayerReviveServer.removePlayerAsHelper(player);
        PlayerReviveFabric.NETWORK.sendToClient(new HelperPacket(null, false), (ServerPlayerEntity) player);

        PlayerReviveServer.startBleeding(player, source);

        if (player.hasVehicle())
            player.stopRiding();

        if (PlayerReviveFabric.CONFIG.bleeding.affectHunger)
            player.getHungerManager().setFoodLevel(PlayerReviveFabric.CONFIG.bleeding.remainingHunger);

        player.setHealth(PlayerReviveFabric.CONFIG.bleeding.bleedingHealth);

        if (PlayerReviveFabric.CONFIG.bleeding.bleedingMessage)
            if (PlayerReviveFabric.CONFIG.bleeding.bleedingMessageTrackingOnly) {
                if (player.getWorld().getChunkManager() instanceof ServerChunkManager chunkManager)
                    chunkManager.sendToOtherNearbyPlayers(player, new GameMessageS2CPacket(Text.translatable("playerrevive.chat.bleeding", player
                            .getDisplayName()), false));
            } else
                player.getServer().getPlayerManager().broadcast(Text.translatable("playerrevive.chat.bleeding", player.getDisplayName()), false);

        return true;
    }

    public static boolean onCommand(ServerCommandSource source) {
        if (PlayerReviveFabric.CONFIG.bleeding.disableServerCommands && source.isExecutedByPlayer() && PlayerReviveServer.getBleeding(source.getPlayer()).isBleeding()) {
            source.getPlayer().sendMessage(Text.translatable("playerrevive.chat.no_commands"));
            return true;
        }
        return false;
    }
}
