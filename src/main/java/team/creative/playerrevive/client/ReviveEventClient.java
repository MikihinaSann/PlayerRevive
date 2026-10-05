package team.creative.playerrevive.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import team.creative.creativecore.common.util.mc.TooltipUtils;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.mixin.LocalPlayerAccessor;
import team.creative.playerrevive.mixin.MinecraftAccessor;
import team.creative.playerrevive.packet.GiveUpPacket;
import team.creative.playerrevive.packet.StartSelfRevivePacket;
import team.creative.playerrevive.server.PlayerReviveServer;

@Environment(EnvType.CLIENT)
public class ReviveEventClient {

    public static Minecraft mc = Minecraft.getInstance();

    public static UUID helpTarget;
    public static boolean helpActive = false;
    public static boolean inPauseScreen = false;
    public boolean lastHighTension = false;

    private static TensionSound sound;

    private boolean addedEffect = false;
    private int giveUpTimer = 0;

    private final List<Component> hudLines = new ArrayList<>(4);

    public static void register() {
        ReviveEventClient instance = new ReviveEventClient();
        ClientTickEvents.END_CLIENT_TICK.register(instance::clientTick);
        HudElementRegistry.addLast(Identifier.tryBuild(PlayerReviveFabric.MODID, "revive_hud"), instance::extractRenderState);
        LevelRenderEvents.END_MAIN.register(instance::endMain);
    }

    public static void render(GuiGraphicsExtractor graphics, List<Component> list) {
        int space = 15;
        int width = 0;
        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i).getString();
            width = Math.max(width, mc.font.width(text) + 10);
        }

        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i).getString();
            graphics.text(mc.font, text, mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(text) / 2, mc.getWindow().getGuiScaledHeight() / 2 + ((list
                    .size() / 2) * space - space * (i + 1)), -2039584);
        }
    }

    public void clientTick(Minecraft client) {
        Player player = client.player;
        if (player != null) {
            IBleeding revive = PlayerReviveServer.getBleeding(player);

            if (revive.isBleeding()) {
                if (client.options.keyAttack.isDown())
                    if (giveUpTimer > PlayerReviveFabric.CONFIG.bleeding.giveUpSeconds * 20) {
                        PlayerReviveFabric.NETWORK.sendToServer(new GiveUpPacket());
                        giveUpTimer = 0;
                    } else
                        giveUpTimer++;
                else
                    giveUpTimer = 0;

                if (PlayerReviveFabric.CONFIG.revive.selfRevive.enabled && client.options.keyUse.isDown() && player.isHolding(x -> PlayerReviveFabric.CONFIG.revive.selfRevive.item.is(player
                        .level(), x) && x.getCount() >= PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount))
                    PlayerReviveFabric.NETWORK.sendToServer(new StartSelfRevivePacket());
            } else
                giveUpTimer = 0;

            if (PlayerReviveFabric.CONFIG.revive.forceLookAt && !revive.isBleeding() && helpActive) {
                Player other = player.level().getPlayerByUUID(helpTarget);
                if (other != null) {
                    float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                    Vec3 vec3 = player.getEyePosition(partial);
                    Vec3 center = other.getPosition(partial);
                    double d0 = center.x - vec3.x;
                    double d1 = center.y - vec3.y;
                    double d2 = center.z - vec3.z;
                    double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                    player.setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 180.0F / (float) Math.PI))));
                    player.setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 180.0F / (float) Math.PI) - 90.0F));
                    player.setYHeadRot(player.getYRot());
                    player.xRotO = player.getXRot();
                    player.yRotO = player.getYRot();
                    player.yHeadRotO = player.yHeadRot;
                    player.yBodyRot = player.yHeadRot;
                    player.yBodyRotO = player.yBodyRot;
                }
            }
        }
    }

    public void endMain(LevelRenderContext context) {
        Player player = mc.player;
        if (player != null) {
            IBleeding revive = PlayerReviveServer.getBleeding(player);

            if (revive.isBleeding() && PlayerReviveFabric.CONFIG.bleeding.hasShaderEffect) {
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mc.gameRenderer.mainRenderTarget().getDepthTexture(), 1.0);
                mc.gameRenderer.processBlurEffect();
            }
        }
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Player player = mc.player;
        if (player != null) {
            IBleeding revive = PlayerReviveServer.getBleeding(player);

            if (!revive.isBleeding()) {
                lastHighTension = false;

                if (addedEffect) {
                    ((LocalPlayerAccessor) player).setHandsBusy(false);
                    addedEffect = false;
                }

                if (sound != null) {
                    mc.getSoundManager().stop(sound);
                    sound = null;
                }

                if (helpActive && !mc.gui.hud.isHidden() && mc.gui.screen() == null) {
                    Player other = player.level().getPlayerByUUID(helpTarget);
                    if (other != null) {
                        IBleeding bleeding = PlayerReviveServer.getBleeding(other);
                        hudLines.clear();
                        hudLines.add(Component.translatable("playerrevive.gui.label.time_left", formatTime(bleeding.timeLeft())));
                        hudLines.add(Component.literal("" + bleeding.getProgress() + "/" + PlayerReviveFabric.CONFIG.revive.requiredReviveProgress));
                        render(graphics, hudLines);
                    }
                }
            } else {
                player.setPose(Pose.SWIMMING);
                ((LocalPlayerAccessor) player).setHandsBusy(true);
                ((MinecraftAccessor) mc).setMissTime(2);

                player.hurtTime = 0;

                if (revive.timeLeft() < 400) {
                    if (!lastHighTension) {
                        if (!PlayerReviveFabric.CONFIG.disableMusic) {
                            if (sound != null)
                                mc.getSoundManager().stop(sound);
                            sound = new TensionSound(Identifier.tryBuild(PlayerReviveFabric.MODID, "hightension"), PlayerReviveFabric.CONFIG.countdownMusicVolume, 1.0F, false);
                            mc.getSoundManager().play(sound);
                        }
                        lastHighTension = true;

                    }
                } else {
                    if (!addedEffect) {
                        if (sound != null) {
                            mc.getSoundManager().stop(sound);
                            sound = null;
                        }
                        if (!PlayerReviveFabric.CONFIG.disableMusic) {
                            sound = new TensionSound(Identifier.tryBuild(PlayerReviveFabric.MODID, "tension"), PlayerReviveFabric.CONFIG.bleedingMusicVolume, 1.0F, true);
                            mc.getSoundManager().play(sound);
                        }
                    }
                }

                addedEffect = true;

                if (!mc.gui.hud.isHidden() && mc.gui.screen() == null) {
                    hudLines.clear();
                    hudLines.add(Component.translatable("playerrevive.gui.label.time_left", formatTime(revive.timeLeft())));
                    hudLines.add(Component.literal("" + TooltipUtils.print(revive.getProgress()) + "/" + PlayerReviveFabric.CONFIG.revive.requiredReviveProgress));
                    hudLines.add(Component.translatable("playerrevive.gui.give_up.hold", mc.options.keyAttack.getTranslatedKeyMessage(),
                        ((PlayerReviveFabric.CONFIG.bleeding.giveUpSeconds * 20 - giveUpTimer) / 20) + 1));

                    if (PlayerReviveFabric.CONFIG.revive.selfRevive.enabled)
                        hudLines.add(Component.translatable("playerrevive.gui.self_revive.hold", PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount,
                            PlayerReviveFabric.CONFIG.revive.selfRevive.item.description()));
                    render(graphics, hudLines);
                }
            }

        }
    }

    public String formatTime(int timeLeft) {
        int lengthOfMinute = 20 * 60;
        int lengthOfHour = lengthOfMinute * 60;

        int hours = timeLeft / lengthOfHour;
        timeLeft -= hours * lengthOfHour;

        int minutes = timeLeft / lengthOfMinute;
        timeLeft -= minutes * lengthOfMinute;

        int seconds = timeLeft / 20;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
