package team.creative.playerrevive.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import team.creative.creativecore.common.util.mc.TooltipUtils;
import team.creative.playerrevive.PlayerReviveFabric;
import team.creative.playerrevive.api.IBleeding;
import team.creative.playerrevive.mixin.GameRendererAccessor;
import team.creative.playerrevive.packet.GiveUpPacket;
import team.creative.playerrevive.packet.StartSelfRevivePacket;
import team.creative.playerrevive.server.PlayerReviveServer;

@Environment(EnvType.CLIENT)
public class ReviveEventClient {

    private static final Identifier BLUR_SHADER = Identifier.of("minecraft", "shaders/post/blur.json");
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static TensionSound sound;

    public static UUID helpTarget;
    public static boolean helpActive = false;
    public static boolean inPauseScreen = false;

    public static void register() {
        ReviveEventClient instance = new ReviveEventClient();
        ClientTickEvents.END_CLIENT_TICK.register(instance::clientTick);
        HudRenderCallback.EVENT.register(instance::renderHud);
    }

    public static void render(DrawContext graphics, List<Text> list) {
        int space = 15;
        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i).getString();
            graphics.drawText(mc.textRenderer, text,
                    mc.getWindow().getScaledWidth() / 2 - mc.textRenderer.getWidth(text) / 2,
                    mc.getWindow().getScaledHeight() / 2 + ((list.size() / 2) * space - space * (i + 1)),
                    0xFFFFFF, true);
        }
    }

    public boolean lastShader = false;
    public boolean lastHighTension = false;

    private boolean addedEffect = false;
    private int giveUpTimer = 0;

    public void clientTick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || !player.isAlive()) {
            giveUpTimer = 0;
            return;
        }

        IBleeding revive = PlayerReviveServer.getBleeding(player);

        if (revive.isBleeding()) {
            // Handle give up
            if (client.options.attackKey.isPressed()) {
                if (giveUpTimer > PlayerReviveFabric.CONFIG.bleeding.giveUpSeconds * 20) {
                    PlayerReviveFabric.NETWORK.sendToServer(new GiveUpPacket());
                    giveUpTimer = 0;
                } else {
                    giveUpTimer++;
                }
            } else {
                giveUpTimer = 0;
            }

            // Handle self-revive
            if (PlayerReviveFabric.CONFIG.revive.selfRevive.enabled && client.options.useKey.isPressed() &&
                    player.isHolding(stack -> PlayerReviveFabric.CONFIG.revive.selfRevive.item.is(stack) &&
                            stack.getCount() >= PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount))
                PlayerReviveFabric.NETWORK.sendToServer(new StartSelfRevivePacket());

            // Screen blocking is handled by MinecraftClientScreenMixin (like original ScreenEvent.Opening)
        } else {
            giveUpTimer = 0;

            // Force look at target when helping (forceLookAt)
            if (PlayerReviveFabric.CONFIG.revive.forceLookAt && helpActive) {
                PlayerEntity other = player.getWorld().getPlayerByUuid(helpTarget);
                if (other != null) {
                    float partial = client.getRenderTickCounter().getTickDelta(false);
                    Vec3d vec3 = player.getEyePos();
                    Vec3d center = other.getLerpedPos(partial);
                    double d0 = center.x - vec3.x;
                    double d1 = center.y - vec3.y;
                    double d2 = center.z - vec3.z;
                    double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                    player.setPitch(MathHelper.wrapDegrees((float) (-(MathHelper.atan2((float) d1, (float) d3) * 180.0F / (float) Math.PI))));
                    player.setYaw(MathHelper.wrapDegrees((float) (MathHelper.atan2((float) d2, (float) d0) * 180.0F / (float) Math.PI) - 90.0F));
                    player.headYaw = player.getYaw();
                    player.prevPitch = player.getPitch();
                    player.prevYaw = player.getYaw();
                    player.prevHeadYaw = player.headYaw;
                    player.bodyYaw = player.headYaw;
                    player.prevBodyYaw = player.bodyYaw;
                }
            }
        }
    }

    public void renderHud(DrawContext graphics, RenderTickCounter tickCounter) {
        PlayerEntity player = mc.player;
        if (player == null)
            return;

        IBleeding revive = PlayerReviveServer.getBleeding(player);

        if (!revive.isBleeding() || !player.isAlive()) {
            // --- Not bleeding: cleanup ---
            lastHighTension = false;
            if (lastShader) {
                ((GameRendererAccessor) mc.gameRenderer).setPostProcessorEnabled(false);
                mc.gameRenderer.onCameraEntitySet(mc.getCameraEntity());
                lastShader = false;
            }

            if (addedEffect) {
                player.removeStatusEffect(StatusEffects.JUMP_BOOST);
                addedEffect = false;
            }

            if (sound != null) {
                mc.getSoundManager().stop(sound);
                sound = null;
            }

            // Show helper HUD
            if (helpActive && !mc.options.hudHidden && mc.currentScreen == null) {
                PlayerEntity other = player.getWorld().getPlayerByUuid(helpTarget);
                if (other != null) {
                    List<Text> list = new ArrayList<>();
                    IBleeding bleeding = PlayerReviveServer.getBleeding(other);
                    list.add(Text.translatable("playerrevive.gui.label.time_left", formatTime(bleeding.timeLeft())));
                    list.add(Text.literal("" + bleeding.getProgress() + "/" + PlayerReviveFabric.CONFIG.revive.requiredReviveProgress));
                    render(graphics, list);
                }
            }
        } else {
            // --- Bleeding: effects + HUD ---

            // Only add jump effect once
            if (!addedEffect) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 0, -10));
                addedEffect = true;
            }
            player.hurtTime = 0;

            // Tension sounds
            if (revive.timeLeft() < 400) {
                if (!lastHighTension) {
                    if (!PlayerReviveFabric.CONFIG.disableMusic) {
                        if (sound != null)
                            mc.getSoundManager().stop(sound);
                        sound = new TensionSound(Identifier.of(PlayerReviveFabric.MODID, "hightension"), PlayerReviveFabric.CONFIG.countdownMusicVolume, 1.0F, false);
                        mc.getSoundManager().play(sound);
                    }
                    lastHighTension = true;
                }
            } else {
                if (!lastShader) {
                    if (sound != null)
                        mc.getSoundManager().stop(sound);
                    if (!PlayerReviveFabric.CONFIG.disableMusic) {
                        sound = new TensionSound(Identifier.of(PlayerReviveFabric.MODID, "tension"), PlayerReviveFabric.CONFIG.bleedingMusicVolume, 1.0F, true);
                        mc.getSoundManager().play(sound);
                    }
                }
            }

            // Shader effect — reload if missing (F5/reset can clear it)
            if (PlayerReviveFabric.CONFIG.bleeding.hasShaderEffect) {
                if (mc.gameRenderer.getPostProcessor() == null) {
                    ((GameRendererAccessor) mc.gameRenderer).invokeLoadPostProcessor(BLUR_SHADER);
                    ((GameRendererAccessor) mc.gameRenderer).setPostProcessorEnabled(true);
                    lastShader = true;
                } else if (!((GameRendererAccessor) mc.gameRenderer).getPostProcessorEnabled()) {
                    ((GameRendererAccessor) mc.gameRenderer).setPostProcessorEnabled(true);
                    lastShader = true;
                }
            }

            // Render bleeding HUD text
            if (!mc.options.hudHidden && (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen)) {
                List<Text> list = new ArrayList<>();
                list.add(Text.translatable("playerrevive.gui.label.time_left", formatTime(revive.timeLeft())));
                list.add(Text.literal("" + TooltipUtils.print(revive.getProgress()) + "/" + PlayerReviveFabric.CONFIG.revive.requiredReviveProgress));
                list.add(Text.translatable("playerrevive.gui.give_up.hold", mc.options.attackKey.getBoundKeyLocalizedText(),
                        ((PlayerReviveFabric.CONFIG.bleeding.giveUpSeconds * 20 - giveUpTimer) / 20) + 1));

                if (PlayerReviveFabric.CONFIG.revive.selfRevive.enabled)
                    list.add(Text.translatable("playerrevive.gui.self_revive.hold", PlayerReviveFabric.CONFIG.revive.selfRevive.itemCount,
                            PlayerReviveFabric.CONFIG.revive.selfRevive.item.description()));
                render(graphics, list);
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
