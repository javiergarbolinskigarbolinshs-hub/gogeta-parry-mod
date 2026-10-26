package dev.gogetaparry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.gogetaparry.ClientParryFx;
import dev.gogetaparry.GogetaParry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Der Gogeta-Parry aus Dragon Ball Legends:
 *  1. greller blauer Blitz an der Handflaeche
 *  2. eine grosse gebogene Ki-Welle, die von der Hand aus nach vorne schwappt und verblasst
 * Wird fuer jeden parryenden Spieler in der Naehe gezeichnet.
 */
@Mod.EventBusSubscriber(modid = GogetaParry.MODID, value = Dist.CLIENT)
public final class KiWaveRenderer {
    private static final ResourceLocation WAVE_TEX = new ResourceLocation(GogetaParry.MODID, "textures/effect/ki_wave.png");
    private static final ResourceLocation FLASH_TEX = new ResourceLocation(GogetaParry.MODID, "textures/effect/ki_flash.png");

    private static final int RINGS = 10;      // Unterteilung von der Mitte zum Rand
    private static final int SEGMENTS = 32;   // Unterteilung rundherum
    private static final double CAP_ANGLE = Math.toRadians(50); // wie weit die Welle sich um die Hand woelbt
    private static final float BRIGHTNESS = 0.7f;               // Grundhelligkeit
    private static final float FIRST_PERSON_BRIGHTNESS = 0.55f; // eigene Welle etwas dunkler, damit man noch was sieht

    private KiWaveRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Map<Integer, Long> active = ClientParryFx.active();
        if (active.isEmpty()) return;

        float partialTick = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        Quaternionf camRot = event.getCamera().rotation();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType waveType = RenderType.eyes(WAVE_TEX);   // additiv: leuchtet, Schwarz = unsichtbar
        RenderType flashType = RenderType.eyes(FLASH_TEX);

        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> e : active.entrySet()) {
            Entity entity = mc.level.getEntity(e.getKey());
            if (entity == null) continue;
            long ms = now - e.getValue();

            Vec3 look = entity.getViewVector(partialTick).normalize();
            Vec3 palm = entity.getEyePosition(partialTick).add(look.scale(0.75)).add(0, -0.15, 0);

            boolean ownFirstPerson = entity == mc.player && mc.options.getCameraType().isFirstPerson();
            float base = ownFirstPerson ? FIRST_PERSON_BRIGHTNESS : BRIGHTNESS;

            // Von aussen gesehen: Welle kommt aus der ausgestreckten Hand (linke oder rechte Seite)
            if (!ownFirstPerson && entity instanceof Player player) {
                Vec3 side = look.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() > 1.0E-4) {
                    double sign = player.getMainArm().getOpposite() == HumanoidArm.LEFT ? -1.0 : 1.0;
                    palm = palm.add(side.normalize().scale(0.15 * sign));
                }
            }

            ps.pushPose();
            ps.translate(palm.x - cam.x, palm.y - cam.y, palm.z - cam.z);
            Matrix4f pose = ps.last().pose();
            Matrix3f normal = ps.last().normal();

            // Haupt-Welle
            drawWave(buffers.getBuffer(waveType), pose, normal, look, ms, 1.0f, base);
            // zweite, groessere und schwaechere Welle kurz danach (gibt Tiefe)
            if (ms > 70) {
                drawWave(buffers.getBuffer(waveType), pose, normal, look, ms - 70, 1.3f, base * 0.45f);
            }
            // Blitz an der Handflaeche
            if (ms < 220) {
                float grow = (ownFirstPerson ? 0.2f : 0.25f) + (ownFirstPerson ? 0.4f : 0.6f) * easeOut(ms / 120f);
                float bright = 1f - ms / 220f;
                drawFlash(buffers.getBuffer(flashType), pose, normal, camRot, look, grow, bright);
            }
            ps.popPose();
        }
        buffers.endBatch(waveType);
        buffers.endBatch(flashType);
    }

    private static float easeOut(float t) {
        float u = 1f - Math.min(1f, Math.max(0f, t));
        return 1f - u * u * u;
    }

    /** Gebogene Ki-Welle als Ausschnitt einer Kugel, die nach vorne waechst. */
    private static void drawWave(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vec3 f,
                                 long ms, float scale, float strength) {
        if (ms < 0) return;
        float p = easeOut(ms / 320f);
        float bright = (ms < 260 ? 1f : Math.max(0f, 1f - (ms - 260) / 300f)) * strength;
        if (bright <= 0.01f) return;

        double radius = (0.3 + 0.9 * p) * scale;                  // Kugelradius
        Vec3 apex = f.scale(0.2 + 0.9 * p);                         // Welle fliegt nach vorne zum Gegner
        Vec3 center = apex.subtract(f.scale(radius));               // Kugelmitte hinter der Welle

        Vec3 right = f.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(f).normalize();

        float scroll = ms * 0.00025f; // Lichtstreifen drehen sich langsam
        float nx = (float) f.x, ny = (float) f.y, nz = (float) f.z;

        for (int i = 0; i < RINGS; i++) {
            double t0 = CAP_ANGLE * i / RINGS;
            double t1 = CAP_ANGLE * (i + 1) / RINGS;
            float v0 = (float) i / RINGS;
            float v1 = (float) (i + 1) / RINGS;
            for (int j = 0; j < SEGMENTS; j++) {
                double a0 = 2 * Math.PI * j / SEGMENTS;
                double a1 = 2 * Math.PI * (j + 1) / SEGMENTS;
                float u0 = (float) j / SEGMENTS + scroll;
                float u1 = (float) (j + 1) / SEGMENTS + scroll;

                Vec3 p00 = capPoint(center, radius, f, right, up, t0, a0);
                Vec3 p01 = capPoint(center, radius, f, right, up, t0, a1);
                Vec3 p11 = capPoint(center, radius, f, right, up, t1, a1);
                Vec3 p10 = capPoint(center, radius, f, right, up, t1, a0);

                // von beiden Seiten sichtbar
                vertex(vc, pose, normal, p00, u0, v0, bright, nx, ny, nz);
                vertex(vc, pose, normal, p10, u0, v1, bright, nx, ny, nz);
                vertex(vc, pose, normal, p11, u1, v1, bright, nx, ny, nz);
                vertex(vc, pose, normal, p01, u1, v0, bright, nx, ny, nz);

                vertex(vc, pose, normal, p01, u1, v0, bright, -nx, -ny, -nz);
                vertex(vc, pose, normal, p11, u1, v1, bright, -nx, -ny, -nz);
                vertex(vc, pose, normal, p10, u0, v1, bright, -nx, -ny, -nz);
                vertex(vc, pose, normal, p00, u0, v0, bright, -nx, -ny, -nz);
            }
        }
    }

    private static Vec3 capPoint(Vec3 center, double radius, Vec3 f, Vec3 right, Vec3 up, double theta, double phi) {
        double s = Math.sin(theta);
        double c = Math.cos(theta);
        Vec3 side = right.scale(Math.cos(phi)).add(up.scale(Math.sin(phi)));
        return center.add(f.scale(c * radius)).add(side.scale(s * radius));
    }

    /** Blitz, der immer zur Kamera zeigt. */
    private static void drawFlash(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Quaternionf camRot,
                                  Vec3 f, float size, float bright) {
        Vector3f[] corners = {
                new Vector3f(-size, -size, 0), new Vector3f(size, -size, 0),
                new Vector3f(size, size, 0), new Vector3f(-size, size, 0)
        };
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        Vec3 off = f.scale(0.05); // minimal vor der Hand
        for (int k = 0; k < 4; k++) {
            corners[k].rotate(camRot);
            Vec3 p = new Vec3(corners[k].x() + off.x, corners[k].y() + off.y, corners[k].z() + off.z);
            vertex(vc, pose, normal, p, uv[k][0], uv[k][1], bright, 0, 1, 0);
        }
        for (int k = 3; k >= 0; k--) {
            Vec3 p = new Vec3(corners[k].x() + off.x, corners[k].y() + off.y, corners[k].z() + off.z);
            vertex(vc, pose, normal, p, uv[k][0], uv[k][1], bright, 0, -1, 0);
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vec3 p,
                               float u, float v, float bright, float nx, float ny, float nz) {
        float c = Math.min(1f, bright);
        vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .color(c, c, c, 1.0f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}
