package dev.gogetaparry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.gogetaparry.ClientParryFx;
import dev.gogetaparry.GogetaParry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Die Ki-Kugel des Fernkampf-Konters:
 *  - Aufladen: Kugel waechst zwischen den Haenden, Strahlen drehen sich
 *  - Flug: Kugel fliegt mit Leuchtspur zum Schuetzen
 *  - Einschlag: grosser Lichtblitz
 * Wird fuer jeden Spieler in der Naehe gezeichnet.
 */
@Mod.EventBusSubscriber(modid = GogetaParry.MODID, value = Dist.CLIENT)
public final class KiBlastRenderer {
    private static final ResourceLocation ORB = new ResourceLocation(GogetaParry.MODID, "textures/effect/ki_orb.png");
    private static final ResourceLocation RAYS = new ResourceLocation(GogetaParry.MODID, "textures/effect/ki_flash.png");
    private static final int TRAIL = 7;

    private KiBlastRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Map<Integer, ClientParryFx.Blast> blasts = ClientParryFx.blasts();
        if (blasts.isEmpty()) return;

        float pt = event.getPartialTick();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Vector3f l = camera.getLeftVector();
        Vector3f u = camera.getUpVector();
        Vec3 left = new Vec3(l.x(), l.y(), l.z());
        Vec3 up = new Vec3(u.x(), u.y(), u.z());
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        long now = System.currentTimeMillis();

        for (int layer = 0; layer < 2; layer++) {
            RenderType type = RenderType.eyes(layer == 0 ? ORB : RAYS);
            VertexConsumer vc = buffers.getBuffer(type);
            for (Map.Entry<Integer, ClientParryFx.Blast> e : blasts.entrySet()) {
                Entity shooter = mc.level.getEntity(e.getKey());
                ClientParryFx.Blast b = e.getValue();
                Entity target = mc.level.getEntity(b.targetId);
                if (shooter == null) continue;

                Vec3 targetPos = target != null
                        ? target.getPosition(pt).add(0, target.getBbHeight() * 0.6, 0)
                        : shooter.getEyePosition(pt).add(shooter.getViewVector(pt).scale(10));
                Vec3 eye = shooter.getEyePosition(pt);
                Vec3 dir = targetPos.subtract(eye);
                dir = dir.lengthSqr() < 1.0E-4 ? shooter.getViewVector(pt) : dir.normalize();
                Vec3 hands = eye.add(dir.scale(0.75)).add(0, -0.35, 0);

                ps.pushPose();
                ps.translate(-cam.x, -cam.y, -cam.z);
                Matrix4f pose = ps.last().pose();
                Matrix3f normal = ps.last().normal();

                if (b.fireMillis < 0) {
                    // Aufladen: Kugel waechst in den Haenden
                    float t = Math.min(1f, (now - b.start) / (float) ClientParryFx.BLAST_CHARGE_END);
                    float pulse = 1f + 0.12f * (float) Math.sin(now * 0.05);
                    if (layer == 0) {
                        billboard(vc, pose, normal, hands, left, up, (0.12f + 0.3f * t) * pulse, 0f, 1f);
                    } else {
                        billboard(vc, pose, normal, hands, left, up, (0.2f + 0.5f * t) * pulse, now * 0.3f, 0.5f + 0.4f * t);
                    }
                } else {
                    float t = (now - b.fireMillis) / (float) Math.max(1L, b.flightMillis);
                    if (t < 1f) {
                        // Flug mit Leuchtspur
                        for (int i = TRAIL; i >= 0; i--) {
                            float ti = t - i * 0.05f;
                            if (ti < 0f) continue;
                            Vec3 p = hands.lerp(targetPos, ti);
                            float fade = 1f - i / (float) (TRAIL + 1);
                            if (layer == 0) {
                                billboard(vc, pose, normal, p, left, up, 0.45f * (0.45f + 0.55f * fade), 0f, fade);
                            } else if (i == 0) {
                                billboard(vc, pose, normal, p, left, up, 0.8f, now * 0.4f, 0.8f);
                            }
                        }
                    } else {
                        // Einschlag: grosser Lichtblitz
                        float k = Math.min(1f, (now - b.fireMillis - b.flightMillis) / 350f);
                        if (k < 1f) {
                            float size = layer == 0 ? 0.8f + 2.6f * k : 1.2f + 3.2f * k;
                            billboard(vc, pose, normal, targetPos, left, up, size, now * 0.2f, (1f - k) * (layer == 0 ? 1f : 0.7f));
                        }
                    }
                }
                ps.popPose();
            }
            buffers.endBatch(type);
        }
    }

    /** Quadrat, das immer zur Kamera zeigt (additiv, Schwarz = unsichtbar). */
    private static void billboard(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vec3 c, Vec3 left, Vec3 up,
                                  float size, float spinDeg, float bright) {
        if (bright <= 0.01f || size <= 0f) return;
        double s = Math.toRadians(spinDeg);
        Vec3 r = left.scale(Math.cos(s)).add(up.scale(Math.sin(s))).scale(size);
        Vec3 v = up.scale(Math.cos(s)).subtract(left.scale(Math.sin(s))).scale(size);
        float k = Math.min(1f, bright);
        vertex(vc, pose, normal, c.subtract(r).subtract(v), 0, 1, k);
        vertex(vc, pose, normal, c.add(r).subtract(v), 1, 1, k);
        vertex(vc, pose, normal, c.add(r).add(v), 1, 0, k);
        vertex(vc, pose, normal, c.subtract(r).add(v), 0, 0, k);
        vertex(vc, pose, normal, c.subtract(r).add(v), 0, 0, k);
        vertex(vc, pose, normal, c.add(r).add(v), 1, 0, k);
        vertex(vc, pose, normal, c.add(r).subtract(v), 1, 1, k);
        vertex(vc, pose, normal, c.subtract(r).subtract(v), 0, 1, k);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vec3 p, float u, float v, float k) {
        vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .color(k, k, k, 1.0f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0f, 1f, 0f)
                .endVertex();
    }
}
