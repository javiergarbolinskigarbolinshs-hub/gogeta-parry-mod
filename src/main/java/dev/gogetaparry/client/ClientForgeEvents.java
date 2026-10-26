package dev.gogetaparry.client;

import dev.gogetaparry.ClientConfig;
import dev.gogetaparry.ClientParryState;
import dev.gogetaparry.GogetaParry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/** Client-Events auf dem Forge-Bus: Kamera-Wackeln beim Parry, Taste fuer die Kreis-Position. */
@Mod.EventBusSubscriber(modid = GogetaParry.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static final long SHAKE_MILLIS = 300;

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long now = System.currentTimeMillis();
        shake(event, now - ClientParryState.flashStartMillis, SHAKE_MILLIS, 2.5f);   // Parry
        shake(event, now - ClientParryState.impactStartMillis, 450, 5.0f);          // Finisher-Impact
    }

    private static void shake(ViewportEvent.ComputeCameraAngles event, long since, long duration, float power) {
        if (since < 0 || since >= duration) return;
        float strength = (1.0f - since / (float) duration) * power;
        event.setRoll(event.getRoll() + Mth.sin(since * 0.12f) * strength);
        event.setPitch(event.getPitch() + Mth.cos(since * 0.15f) * strength * 0.5f);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        while (ParryKeys.SWITCH_HUD.consumeClick()) {
            ClientConfig.HudPosition next = ClientConfig.HUD_POSITION.get().next();
            ClientConfig.HUD_POSITION.set(next);
            ClientConfig.HUD_POSITION.save();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable(
                        "msg.gogetaparry.hud_" + next.name().toLowerCase(Locale.ROOT)), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientParryState.reset();
        dev.gogetaparry.ClientParryFx.reset();
    }
}
