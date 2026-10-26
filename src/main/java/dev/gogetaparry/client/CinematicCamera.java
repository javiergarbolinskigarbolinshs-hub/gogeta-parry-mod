package dev.gogetaparry.client;

import dev.gogetaparry.ClientConfig;
import dev.gogetaparry.ClientParryFx;
import dev.gogetaparry.ClientParryState;
import dev.gogetaparry.GogetaParry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kamerafahrt wie in Dragon Ball Legends:
 *  - Parry: Kamera springt vor den Spieler (man sieht die Hand und die Ki-Welle auf sich zukommen)
 *  - Konter-Kick: Kamera bleibt vorne - Hocke, Sprint, Drehung und Kick wie im Video
 *    (der Spieler schaut dabei etwas neben den Gegner, damit beide im Bild sind)
 *  - Ki-Blast (gegen Fernkampf): Kamera von hinten, die Kugel fliegt sichtbar zum Schuetzen
 *  - kurz nach dem Treffer zurueck zur eigenen Ansicht
 */
@Mod.EventBusSubscriber(modid = GogetaParry.MODID, value = Dist.CLIENT)
public final class CinematicCamera {
    private static final long FRONT_MILLIS = 550; // so lange von vorne

    private static CameraType saved;

    private CinematicCamera() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            saved = null;
            return;
        }

        long sinceParry = System.currentTimeMillis() - ClientParryState.flashStartMillis;
        long kick = ClientParryFx.kickElapsed(mc.player.getId());
        boolean blast = ClientParryFx.blast(mc.player.getId()) != null;
        boolean inScene = ClientConfig.CINEMATIC_CAMERA.get()
                && ((sinceParry >= 0 && sinceParry < FRONT_MILLIS) || kick >= 0 || blast);

        if (inScene) {
            if (saved == null) saved = mc.options.getCameraType();
            // Ki-Blast: von hinten (man sieht die Kugel zum Schuetzen fliegen), sonst von vorne
            CameraType wanted = blast ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT;
            if (mc.options.getCameraType() != wanted) mc.options.setCameraType(wanted);
        } else if (saved != null) {
            mc.options.setCameraType(saved);
            saved = null;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (saved != null) {
            Minecraft.getInstance().options.setCameraType(saved);
            saved = null;
        }
    }
}
