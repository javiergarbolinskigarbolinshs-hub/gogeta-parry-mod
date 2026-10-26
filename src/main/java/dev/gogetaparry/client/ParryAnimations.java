package dev.gogetaparry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.gogetaparry.ClientParryFx;
import dev.gogetaparry.GogetaParry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Die Animationen wie bei Gogeta in Dragon Ball Legends:
 * Parry (Hand vor, Ki-Welle) und der Konter-Kick (Hocke, Sprint, Drehung, fliegender Kick).
 *
 *  - Ich-Perspektive: eigener Arm wird nach vorne Richtung Bildmitte gestossen
 *  - Dritte Person / andere Spieler: Arm wird am Spielermodell nach vorne gestreckt
 */
@Mod.EventBusSubscriber(modid = GogetaParry.MODID, value = Dist.CLIENT)
public final class ParryAnimations {

    /**
     * Pose am Spielermodell wie Gogeta im Video. Die Phase ergibt sich aus der Zeit:
     *  - Parry: freie Hand gerade zum Gegner, andere Hand als Faust nach hinten, breiter Stand
     *  - Kick:  Hocke -> Sprint (Arme nach hinten) -> Drehung (Arme seitlich) -> Kick (Bein gestreckt)
     */
    public static final HumanoidModel.ArmPose PARRY_POSE = HumanoidModel.ArmPose.create(
            "GOGETAPARRY_PARRY", false, (model, entity, arm) -> {
                boolean leftFree = entity.getMainArm() == HumanoidArm.RIGHT;
                ModelPart free = leftFree ? model.leftArm : model.rightArm;
                ModelPart main = leftFree ? model.rightArm : model.leftArm;
                ModelPart kickLeg = leftFree ? model.rightLeg : model.leftLeg;
                ModelPart standLeg = leftFree ? model.leftLeg : model.rightLeg;
                // nach aussen: rechter Arm/Bein +zRot, linker -zRot
                float outFree = leftFree ? -1f : 1f;
                float outMain = -outFree;

                ClientParryFx.Blast blast = ClientParryFx.blast(entity.getId());
                if (blast != null) {
                    poseBlast(model, free, main, outFree, outMain, blast);
                    return;
                }

                long kick = ClientParryFx.kickElapsed(entity.getId());
                if (kick >= 0) {
                    poseKick(model, free, main, kickLeg, standLeg, outFree, outMain, kick);
                    return;
                }

                // ---- Parry ----
                free.xRot = model.head.xRot - (float) Math.PI / 2.0f - 0.1f;
                free.yRot = model.head.yRot - 0.35f * outFree;
                free.zRot = 0.0f;
                main.xRot = 0.75f;
                main.zRot = 0.35f * outMain;
                if (!model.crouching) {
                    model.rightLeg.xRot = 0.0f;
                    model.leftLeg.xRot = 0.0f;
                    model.rightLeg.zRot = 0.28f;
                    model.leftLeg.zRot = -0.28f;
                }
            });

    /** Ki-Blast: erst Haende tief zusammen (Aufladen), dann beide Arme gerade nach vorne (Abfeuern). */
    private static void poseBlast(HumanoidModel<?> model, ModelPart free, ModelPart main,
                                  float outFree, float outMain, ClientParryFx.Blast blast) {
        boolean fired = blast.fireMillis >= 0;
        if (!fired) {
            free.xRot = -0.55f;
            main.xRot = -0.55f;
            free.yRot = 0f;
            main.yRot = 0f;
            free.zRot = -0.45f * outFree;   // Haende vor dem Bauch zusammen
            main.zRot = -0.45f * outMain;
        } else {
            float aim = model.head.xRot - (float) Math.PI / 2.0f;
            free.xRot = aim;
            main.xRot = aim;
            free.yRot = 0.18f * outFree;    // Handflaechen zueinander
            main.yRot = 0.18f * outMain;
            free.zRot = 0f;
            main.zRot = 0f;
        }
        if (!model.crouching) {
            model.rightLeg.xRot = 0.0f;
            model.leftLeg.xRot = 0.0f;
            model.rightLeg.zRot = 0.3f;
            model.leftLeg.zRot = -0.3f;
        }
    }

    private static void poseKick(HumanoidModel<?> model, ModelPart free, ModelPart main, ModelPart kickLeg,
                                 ModelPart standLeg, float outFree, float outMain, long ms) {
        free.yRot = 0f;
        main.yRot = 0f;
        if (ms < ClientParryFx.KICK_CROUCH_END) {
            // Hocke: Arme als Deckung vor dem Koerper
            model.crouching = true;
            free.xRot = -1.0f;
            main.xRot = -0.8f;
            free.zRot = -0.25f * outFree;
            main.zRot = -0.25f * outMain;
        } else if (ms < ClientParryFx.KICK_DASH_END) {
            // Sprint: Arme nach hinten gerissen, Beine gestreckt
            free.xRot = 1.1f;
            main.xRot = 1.1f;
            free.zRot = 0.2f * outFree;
            main.zRot = 0.2f * outMain;
            kickLeg.xRot = 0.35f;
            standLeg.xRot = 0.15f;
        } else if (ms < ClientParryFx.KICK_SPIN_END) {
            // Drehung: Arme seitlich ausgebreitet, Bein holt aus
            free.xRot = 0f;
            main.xRot = 0f;
            free.zRot = 1.35f * outFree;
            main.zRot = 1.35f * outMain;
            float t = (ms - ClientParryFx.KICK_DASH_END) / (float) (ClientParryFx.KICK_SPIN_END - ClientParryFx.KICK_DASH_END);
            kickLeg.xRot = -0.4f - 1.0f * t;
            standLeg.xRot = 0.5f;
        } else {
            // Kick: Bein voll gestreckt zum Gegner, Deckung oben, andere Hand hinten
            kickLeg.xRot = -1.65f;
            kickLeg.zRot = 0f;
            standLeg.xRot = 0.55f;
            free.xRot = -1.2f;
            free.zRot = -0.3f * outFree;
            main.xRot = 0.9f;
            main.zRot = 0.4f * outMain;
        }
    }

    // Ich-Perspektive (Werte fuer den linken Arm, in der 3D-Simulation abgestimmt)
    private static final Vector3f SHOULDER_TARGET = new Vector3f(-0.42f, -0.62f, -0.45f);
    private static final Vector3f SHOULDER_HIDDEN = new Vector3f(-0.55f, -1.30f, -0.30f);
    private static final Vector3f ARM_DIRECTION = new Vector3f(0.30f, 0.50f, -0.60f);

    private ParryAnimations() {
    }

    /** 0 = Arm unten/weg, 1 = Arm voll ausgestreckt. Schnell rein, kurz halten, wieder runter. */
    public static float armProgress(long ms) {
        if (ms < 0) return 0f;
        if (ms < 90) {
            float t = 1f - ms / 90f;
            return 1f - t * t * t;
        }
        if (ms < 450) return 1f;
        if (ms < ClientParryFx.DURATION_MILLIS) {
            float t = (ms - 450) / (float) (ClientParryFx.DURATION_MILLIS - 450);
            return 1f - t * t;
        }
        return 0f;
    }

    // ------------------------------------------------------------------
    // Dritte Person: Spielermodell
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        int id = event.getEntity().getId();
        ClientParryFx.Blast blast = ClientParryFx.blast(id);
        long kick = ClientParryFx.kickElapsed(id);
        long parry = ClientParryFx.elapsed(id);
        if (blast == null && kick < 0 && parry < 0) return;

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        if (blast != null) {
            model.leftArmPose = PARRY_POSE;
            model.rightArmPose = PARRY_POSE;
            // Spieler schaut etwas neben das Ziel (fuer die Kamera) -> Koerper zum Ziel drehen
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees(blast.yawOffset));
        } else if (kick >= 0) {
            // beim Kick bewegt sich der ganze Koerper -> beide Arme uebernehmen
            model.leftArmPose = PARRY_POSE;
            model.rightArmPose = PARRY_POSE;
            tiltBody(event, kick);
        } else if (event.getEntity().getMainArm().getOpposite() == HumanoidArm.LEFT) {
            model.leftArmPose = PARRY_POSE;
        } else {
            model.rightArmPose = PARRY_POSE;
        }
    }

    /**
     * Kippt/dreht den ganzen Koerper waehrend des Kicks (um die Huefte).
     * Die Drehung wird im Koerper-Raum gemacht: R * Kippung * R^-1, Minecraft haengt danach R selbst an.
     */
    private static void tiltBody(RenderPlayerEvent.Pre event, long ms) {
        var player = event.getEntity();
        PoseStack ps = event.getPoseStack();
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);

        Quaternionf local = null;
        float drop = 0f;
        if (ms < ClientParryFx.KICK_CROUCH_END) {
            drop = -0.12f;                                            // tiefer in die Knie
        } else if (ms < ClientParryFx.KICK_DASH_END) {
            local = Axis.XP.rotationDegrees(-65f);                    // nach vorne gelehnt fliegen
        } else if (ms < ClientParryFx.KICK_SPIN_END) {
            float t = (ms - ClientParryFx.KICK_DASH_END) / (float) (ClientParryFx.KICK_SPIN_END - ClientParryFx.KICK_DASH_END);
            float e = 1f - (1f - t) * (1f - t);
            local = Axis.YP.rotationDegrees(540f * e).mul(Axis.XP.rotationDegrees(-25f * (1f - t)));
        } else {
            local = Axis.XP.rotationDegrees(18f);                     // beim Kick leicht nach hinten lehnen
        }

        ps.translate(0.0, 0.9 + drop, 0.0);
        if (ms >= ClientParryFx.KICK_CROUCH_END) {
            // Spieler schaut etwas neben den Gegner (fuer die Kamera) -> Koerper wieder zum Gegner drehen
            ps.mulPose(Axis.YP.rotationDegrees(ClientParryFx.kickYawOffset(player.getId())));
        }
        if (local != null) {
            ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
            ps.mulPose(local);
            ps.mulPose(Axis.YP.rotationDegrees(bodyYaw - 180f));
        }
        ps.translate(0.0, -0.9, 0.0);
    }

    // ------------------------------------------------------------------
    // Ich-Perspektive: eigener Arm
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.OFF_HAND) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isInvisible()) return;

        long ms = ClientParryFx.elapsed(player.getId());
        if (ms < 0) return;

        event.setCanceled(true); // statt dem normalen Offhand-Item zeichnen wir den Parry-Arm

        float p = armProgress(ms);
        if (p <= 0.001f) return;

        HumanoidArm arm = player.getMainArm().getOpposite();
        float mirror = arm == HumanoidArm.LEFT ? 1.0f : -1.0f;

        Vector3f shoulder = new Vector3f(SHOULDER_HIDDEN).lerp(SHOULDER_TARGET, p);
        shoulder.x *= mirror;

        // kleines Zittern beim Aufprall des Angriffs
        if (ms > 70 && ms < 200) {
            float shake = (1f - (ms - 70) / 130f) * 0.012f;
            shoulder.add(Mth.sin(ms * 0.9f) * shake, Mth.cos(ms * 1.1f) * shake, 0f);
        }

        Vector3f dir = new Vector3f(ARM_DIRECTION.x * mirror, ARM_DIRECTION.y, ARM_DIRECTION.z).normalize();
        Vector3f up = new Vector3f(0f, 1f, 0f);
        Vector3f z = new Vector3f(up).sub(new Vector3f(dir).mul(dir.dot(up))).normalize();
        Vector3f x = new Vector3f(dir).cross(z);
        Matrix3f basis = new Matrix3f(x.x, x.y, x.z, dir.x, dir.y, dir.z, z.x, z.y, z.z);
        Quaternionf rotation = new Quaternionf().setFromNormalized(basis);

        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(shoulder.x, shoulder.y, shoulder.z);
        ps.mulPose(rotation);
        // Der Arm dreht sich im Modell um seine Schulter (Pivot 5/2/0) - das hier gleicht es aus
        ps.translate(-mirror * 5.0f / 16.0f, -2.0f / 16.0f, 0.0f);

        PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        if (arm == HumanoidArm.LEFT) {
            renderer.renderLeftHand(ps, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            renderer.renderRightHand(ps, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        ps.popPose();
    }
}
