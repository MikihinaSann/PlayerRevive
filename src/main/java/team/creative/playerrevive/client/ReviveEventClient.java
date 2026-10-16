package team.creative.playerrevive.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.mixin.LocalPlayerAccessor;
import team.creative.playerrevive.mixin.MinecraftAccessor;
import team.creative.playerrevive.packet.GiveUpPacket;
import team.creative.playerrevive.server.PlayerReviveServer;

public class ReviveEventClient {

    public static Minecraft mc = Minecraft.getInstance();

    public static UUID helpTarget;
    public static boolean helpActive = false;
    public static boolean inPauseScreen = false;
    public boolean lastHighTension = false;

    private static TensionSound sound;

    private boolean addedEffect = false;
    private int giveUpTimer = 0;

    private static final ReviveEventClient INSTANCE = new ReviveEventClient();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> INSTANCE.clientTick());
        WorldRenderEvents.END_MAIN.register(context -> INSTANCE.frameEvent(context));
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> INSTANCE.renderHud(graphics));
    }

    public static void render(GuiGraphics graphics, List<Component> list) {
        int space = 15;
        int width = 0;
        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i).getString();
            width = Math.max(width, mc.font.width(text) + 10);
        }

        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i).getString();
            graphics.drawString(mc.font, text, mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(text) / 2, mc.getWindow().getGuiScaledHeight() / 2 + ((list
                    .size() / 2) * space - space * (i + 1)), -2039584);
        }
    }

    public void clientTick() {
        Player player = mc.player;
        if (player == null)
            return;

        for (Player member : player.level().players()) {
            IBleeding memberRevive = PlayerReviveServer.getBleeding(member);
            if (memberRevive.isBleeding())
                member.setPose(Pose.SWIMMING);
        }

        IBleeding revive = PlayerReviveServer.getBleeding(player);

        if (revive.isBleeding())
            if (mc.options.keyAttack.isDown())
                if (giveUpTimer > PlayerReviveFabric.CONFIG.bleeding.giveUpSeconds * 20) {
                    PlayerReviveFabric.NETWORK.sendToServer(new GiveUpPacket());
                    giveUpTimer = 0;
                } else
                    giveUpTimer++;
            else
                giveUpTimer = 0;
        else
            giveUpTimer = 0;
    }

    private static Player getPlayerByUUID(Player viewer, UUID uuid) {
        for (Player p : viewer.level().players())
            if (p.getUUID().equals(uuid))
                return p;
        return null;
    }

    public void frameEvent(WorldRenderContext context) {
        Player player = mc.player;
        if (player != null && PlayerReviveFabric.CONFIG.revive.forceLookAt) {
            IBleeding revive = PlayerReviveServer.getBleeding(player);
            if (!revive.isBleeding() && helpActive) {
                Player other = getPlayerByUUID(player, helpTarget);
                if (other != null) {
                    float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
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

    public void renderHud(GuiGraphics graphics) {
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

                if (helpActive && !mc.options.hideGui && mc.screen == null) {
                    Player other = getPlayerByUUID(player, helpTarget);
                    if (other != null) {
                        List<Component> list = new ArrayList<>();
                        IBleeding bleeding = PlayerReviveServer.getBleeding(other);
                        list.add(Component.translatable("playerrevive.gui.label.time_left", formatTime(bleeding.timeLeft())));
                        list.add(Component.literal("" + bleeding.getProgress() + "/" + PlayerReviveFabric.CONFIG.revive.requiredReviveProgress));
                        render(graphics, list);
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
                            sound = new TensionSound(ResourceLocation.tryBuild(PlayerReviveFabric.MODID, "hightension"), PlayerReviveFabric.CONFIG.countdownMusicVolume, 1.0F, false);
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
                            sound = new TensionSound(ResourceLocation.tryBuild(PlayerReviveFabric.MODID, "tension"), PlayerReviveFabric.CONFIG.bleedingMusicVolume, 1.0F, true);
                            mc.getSoundManager().play(sound);
                        }
                    }
                }

                addedEffect = true;

                if (PlayerReviveFabric.CONFIG.bleeding.hasShaderEffect)
                    mc.gameRenderer.processBlurEffect();

                if (!mc.options.hideGui && mc.screen == null) {
                    List<Component> list = new ArrayList<>();
                    IBleeding bleeding = PlayerReviveServer.getBleeding(player);
                    list.add(Component.translatable("playerrevive.gui.label.time_left", formatTime(bleeding.timeLeft())));
                    list.add(Component.literal("" + bleeding.getProgress() + "/" + PlayerReviveFabric.CONFIG.revive.requiredReviveProgress));
                    list.add(Component.translatable("playerrevive.gui.hold", mc.options.keyAttack.getTranslatedKeyMessage(),
                        ((PlayerReviveFabric.CONFIG.bleeding.giveUpSeconds * 20 - giveUpTimer) / 20) + 1));
                    render(graphics, list);
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
