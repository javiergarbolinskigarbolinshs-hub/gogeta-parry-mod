package dev.gogetaparry.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import dev.gogetaparry.ClientConfig;
import dev.gogetaparry.ClientParryFx;
import dev.gogetaparry.ClientParryState;
import dev.gogetaparry.GogetaParry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Die Parry-Anzeige wie der Counter-Kreis in Dragon Ball Legends:
 * schwarzer Zacken-Kreis mit grauem Ring und Symbol, der Ring laedt sich blau auf und leuchtet, wenn er voll ist.
 * Dazu: blauer Blitz beim Parry und der Impact-Moment beim Finisher.
 * Position mit Taste H umschalten: unten bei der Hotbar / oben rechts / klein am Fadenkreuz.
 */
public final class ParryHud implements IGuiOverlay {
    public static final ParryHud INSTANCE = new ParryHud();

    private static final long CHARGE_FLASH_MILLIS = 350;
    private static final long BURST_MILLIS = 450;

    private final GaugePainter.State state = new GaugePainter.State();
    /** Angezeigter Fuellstand (laeuft weich hinter dem echten Wert her, Zahl zaehlt hoch). */
    private float displayFill;
    private long lastFrameMillis;

    private ParryHud() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        long now = System.currentTimeMillis();

        // Blauer Blitz ueber den ganzen Bildschirm beim Parry
        long sinceFlash = now - ClientParryState.flashStartMillis;
        if (sinceFlash >= 0 && sinceFlash < 350) {
            int alpha = (int) ((1.0f - sinceFlash / 350.0f) * 110);
            g.fill(0, 0, width, height, (alpha << 24) | 0x40B8FF);
        }

        // Speed-Lines beim Sprint zum Gegner (wie im Anime)
        long kick = ClientParryFx.kickElapsed(mc.player.getId());
        if (kick >= SPEED_FROM && kick < SPEED_TO) {
            drawSpeedLines(g, width, height, kick);
        }

        // Impact beim Finisher: dunkler Bildschirm mit weissen Schlag-Streifen, dann oranger Knall
        long sinceImpact = now - ClientParryState.impactStartMillis;
        if (sinceImpact >= 0 && sinceImpact < IMPACT_DARK_MILLIS + IMPACT_FLASH_MILLIS) {
            drawImpact(g, width, height, sinceImpact);
        }

        if (!ClientParryState.active) return;

        // ----- Zustand -----
        GaugePainter.State s = state;
        s.timeMillis = now;
        s.max = Math.max(1, ClientParryState.maxCharge);
        s.charge = (int) Math.floor(ClientParryState.charge);
        s.cooldown = now < ClientParryState.cooldownEndMillis;
        s.cooldownProgress = s.cooldown
                ? 1.0f - (ClientParryState.cooldownEndMillis - now) / (float) ClientParryState.cooldownTotalMillis
                : 0f;
        s.full = !s.cooldown && ClientParryState.charge >= s.max - 0.001f;

        long sinceCharge = now - ClientParryState.lastChargeMillis;
        if (sinceCharge >= 0 && sinceCharge < CHARGE_FLASH_MILLIS) {
            s.flashSegment = ClientParryState.lastChargeSegment;
            s.flashAmount = 1.0f - sinceCharge / (float) CHARGE_FLASH_MILLIS;
        } else {
            s.flashSegment = -1;
            s.flashAmount = 0;
        }
        long sinceFull = now - ClientParryState.fullSinceMillis;
        s.burst = (s.full && sinceFull >= 0 && sinceFull < BURST_MILLIS) ? sinceFull / (float) BURST_MILLIS : -1;

        // Fuellstand weich nachziehen
        float target = s.cooldown ? 0f : Math.min(1f, ClientParryState.charge / s.max);
        float dt = lastFrameMillis == 0 ? 0f : Math.min(0.1f, (now - lastFrameMillis) / 1000f);
        lastFrameMillis = now;
        displayFill += (target - displayFill) * Math.min(1f, dt * 9f);
        if (Math.abs(target - displayFill) < 0.004f) displayFill = target;

        ClientConfig.HudPosition pos = ClientConfig.HUD_POSITION.get();
        if (pos == ClientConfig.HudPosition.CROSSHAIR) {
            drawPixels(g, width / 2, height / 2, GaugePainter.extent(10, s),
                    (dx, dy) -> GaugePainter.colorAt(dx, dy, 10, 2, s));
            return;
        }

        // ----- Position (Mittelpunkt des Counter-Kreises) -----
        int cx;
        int cy;
        if (pos == ClientConfig.HudPosition.HOTBAR) {
            cx = width / 2 - 91 - 27;
            if (!mc.player.getOffhandItem().isEmpty()) cx -= 29; // Platz fuer die Zweithand
            cx = Math.max(cx, 24);
            cy = height - 23;
        } else {
            cx = width - 30;
            cy = 30;
            if (!mc.player.getActiveEffects().isEmpty()) cy += 50; // unter die Trank-Symbole
        }

        drawCounterEmblem(g, cx, cy, EMBLEM_SIZE, s, now);
        drawHealText(g, mc, cx, cy, now);
        drawPowerUp(g, mc, cx, cy, now);
    }

    /** Power-Up-Anzeige unter dem Kreis: Schwert-Symbol, +30 % und Restzeit. */
    private static void drawPowerUp(GuiGraphics g, Minecraft mc, int cx, int cy, long now) {
        long left = ClientParryState.buffEndMillis - now;
        if (left <= 0) return;
        int secs = (int) Math.ceil(left / 1000.0);
        String text = "\u2694 +" + Math.round(dev.gogetaparry.ParryConfig.BUFF_PERCENT.get()) + "%  \u00BB +"
                + Math.round(dev.gogetaparry.ParryConfig.BUFF_SPEED.get()) + "%  " + secs + "s";
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 160.0);
        int color = GaugePainter.lerp(0xFF4FC3FF, 0xFFD8F6FF, pulse) & 0xFFFFFF;
        int w = mc.font.width(text);
        int y = cy + EMBLEM_SIZE / 2 - 2;
        if (y + 10 > mc.getWindow().getGuiScaledHeight()) y = cy - EMBLEM_SIZE / 2 - 12;
        g.drawString(mc.font, text, cx - w / 2, y, 0xFF000000 | color, true);
    }

    /** Gruene "+2 ❤"-Anzeige, die ueber dem Kreis aufsteigt und verblasst. */
    private static void drawHealText(GuiGraphics g, Minecraft mc, int cx, int cy, long now) {
        long since = now - ClientParryState.healMillis;
        if (since < 0 || since > 1400) return;
        float hearts = ClientParryState.healAmount / 2f;
        String num = Math.abs(hearts - Math.round(hearts)) < 0.05f
                ? String.valueOf(Math.round(hearts)) : String.format(java.util.Locale.ROOT, "%.1f", hearts);
        String text = "+" + num + " \u2764";
        float t = since / 1400f;
        int alpha = (int) (255 * (t < 0.7f ? 1f : (1f - t) / 0.3f));
        if (alpha < 8) return;
        float rise = 10f + 18f * (1f - (1f - t) * (1f - t));
        g.pose().pushPose();
        g.pose().translate(cx, cy - rise - 14, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        int w = mc.font.width(text);
        g.drawString(mc.font, text, -w / 2, 0, (alpha << 24) | 0x5CFF6A, true);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------
    // Counter-Kreis wie in Dragon Ball Legends
    // ------------------------------------------------------------------
    private static final ResourceLocation SHEET =
            new ResourceLocation(GogetaParry.MODID, "textures/gui/counter_emblem.png");
    private static final int CELL = 128;        // Groesse einer Grafik im Raster
    private static final int SHEET_SIZE = 1024; // 8 x 8 Zellen
    private static final int SPRITE_BASE = 0;        // Zacken + Koerper + Metall-Ring
    private static final int SPRITE_GLOW = 1;        // blaues Leuchten mit Strahlen
    private static final int SPRITE_ICON_GREY = 2;
    private static final int SPRITE_ICON_WHITE = 3;
    private static final int SPRITE_ICON_GLOW = 4;
    private static final int SPRITE_SPARK = 5;       // Funke
    private static final int SPRITE_SHOCKWAVE = 6;   // Ring fuer Treffer-/Voll-Welle
    private static final int SPRITE_ARC_BLUE = 8;    // 8..32  = blauer Bogen 0/24 .. 24/24
    private static final int SPRITE_ARC_GREY = 33;   // 33..57 = grauer Bogen (Abklingzeit)
    private static final int SPRITE_LIGHTNING = 58;  // 58..61 = knisternde Blitze (4 Bilder)
    private static final int STEPS = 24;
    private static final int EMBLEM_SIZE = 40;

    private boolean filterSet;

    /**
     * Schwarzer Zacken-Kreis mit Metall-Ring und Symbol (zerbrochene Raute + Pfeil).
     * Laedt sich im Uhrzeigersinn blau auf; voll = Leuchten, Blitze, kreisende Funken, Zacken drehen sich.
     */
    private boolean wasCooldown;
    private long readyPingMillis = -100000L;

    private void drawCounterEmblem(GuiGraphics g, int baseX, int baseY, int size, GaugePainter.State s, long now) {
        if (!filterSet) {
            // weiche Kanten statt Pixel-Treppen beim Verkleinern
            Minecraft.getInstance().getTextureManager().getTexture(SHEET).setFilter(true, false);
            filterSet = true;
        }

        // ---- Zeitpunkte fuer die Animationen ----
        if (wasCooldown && !s.cooldown) readyPingMillis = now;          // Abklingzeit gerade vorbei
        wasCooldown = s.cooldown;
        long sinceParry = now - ClientParryState.flashStartMillis;      // Counter gerade ausgeloest
        float used = (sinceParry >= 0 && sinceParry < 450) ? 1f - sinceParry / 450f : 0f;
        long sincePing = now - readyPingMillis;
        float ping = (sincePing >= 0 && sincePing < 500) ? 1f - sincePing / 500f : 0f;
        boolean buff = ClientParryState.buffEndMillis > now;
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 130.0);

        // ---- Wackeln: bei Treffern leicht, beim Counter stark ----
        float cx = baseX;
        float cy = baseY;
        if (s.flashAmount > 0) {
            float a = s.flashAmount * 2.2f;
            cx += (float) Math.sin(now * 0.9) * a;
            cy += (float) Math.cos(now * 1.3) * a * 0.7f;
        }
        if (used > 0) {
            float a = used * 3f;
            cx += (float) Math.sin(now * 1.1) * a;
            cy += (float) Math.cos(now * 0.8) * a;
        }

        // ---- Groesse: Hupfer + Atmen, wenn voll ----
        float pop = 1f + 0.18f * s.flashAmount + 0.3f * used * used + 0.15f * ping;
        if (s.burst >= 0) pop += 0.25f * (1f - s.burst);
        float breathe = s.full ? 1f + 0.035f * (float) Math.sin(now / 220.0) : 1f;
        float sz = size * pop * breathe;

        RenderSystem.enableBlend();

        // 0) Power-Up: flackernder Ki-Schein hinter dem Kreis
        if (buff) {
            additive();
            float flicker = 0.35f + 0.15f * (float) Math.sin(now / 45.0) + 0.1f * (float) Math.sin(now / 71.0);
            sprite(g, SPRITE_GLOW, cx, cy, sz * 1.25f, 0, 0.6f, 0.85f, 1f, flicker);
        }

        // 1) Leuchten dahinter + Energiewelle alle 1,2 Sekunden, wenn voll
        if (s.full) {
            additive();
            sprite(g, SPRITE_GLOW, cx, cy, sz * (1.45f + 0.1f * pulse), 0, 1f, 1f, 1f, 0.55f + 0.45f * pulse);
            float t = (now % 1200L) / 700f;
            if (t < 1f) sprite(g, SPRITE_SHOCKWAVE, cx, cy, sz * (0.85f + 0.9f * t), 0, 1f, 1f, 1f, (1f - t) * 0.6f);
        }

        // 2) Zacken-Koerper (dreht sich langsam, wenn voll)
        normal();
        float shade = s.cooldown ? 0.85f : 1f;
        float spin = s.full ? (now / 12L) % 360L : 0f;
        sprite(g, SPRITE_BASE, cx, cy, sz, spin, shade, shade, shade, 1f);

        // 3) Lade-Bogen + Funke an der Spitze der Ladung
        if (s.cooldown) {
            int k = Math.round(Math.max(0f, Math.min(1f, s.cooldownProgress)) * STEPS);
            if (k > 0) sprite(g, SPRITE_ARC_GREY + k, cx, cy, sz, 0, 1f, 1f, 1f, 1f);
        } else {
            int k = Math.round(displayFill * STEPS);
            if (k > 0) {
                sprite(g, SPRITE_ARC_BLUE + k, cx, cy, sz, 0, 1f, 1f, 1f, 1f);
                additive();
                if (s.flashAmount > 0) sprite(g, SPRITE_ARC_BLUE + k, cx, cy, sz, 0, 1f, 1f, 1f, s.flashAmount * 0.7f);
                if (s.full) sprite(g, SPRITE_ARC_BLUE + k, cx, cy, sz, 0, 1f, 1f, 1f, 0.3f * pulse);
                normal();
            }
            if (s.flashAmount > 0 && displayFill > 0.01f && displayFill < 0.999f) {
                double ang = Math.toRadians(-90.0 + 360.0 * displayFill);
                float r = sz * 0.216f; // Mitte des Rings
                additive();
                sprite(g, SPRITE_SPARK, cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r,
                        sz * (0.22f + 0.25f * s.flashAmount), (now / 3L) % 360L, 1f, 1f, 1f, 0.4f + 0.6f * s.flashAmount);
                normal();
            }
        }

        // 4) Symbol, Blitze und kreisende Funken
        if (s.full) {
            additive();
            sprite(g, SPRITE_LIGHTNING + (int) ((now / 70L) % 4L), cx, cy, sz * 1.05f, 0, 1f, 1f, 1f, 0.85f);
            sprite(g, SPRITE_ICON_GLOW, cx, cy, sz, 0, 1f, 1f, 1f, 0.6f + 0.4f * pulse);
            normal();
            sprite(g, SPRITE_ICON_WHITE, cx, cy, sz, 0, 1f, 1f, 1f, 1f);
            additive();
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(now / 5.0 + i * 120.0);
                float r = sz * 0.43f;
                float twinkle = 0.6f + 0.4f * (float) Math.sin(now / 90.0 + i * 2.0);
                sprite(g, SPRITE_SPARK, cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r,
                        sz * (0.1f + 0.32f * twinkle), 0, 1f, 1f, 1f, twinkle);
            }
        } else {
            sprite(g, SPRITE_ICON_GREY, cx, cy, sz, 0, 1f, 1f, 1f, 1f);
            additive();
            if (ping > 0) sprite(g, SPRITE_ICON_GLOW, cx, cy, sz, 0, 1f, 1f, 1f, ping);
        }

        // 5) Druckwellen: Treffer klein, Vollwerden gross
        if (s.flashAmount > 0) {
            float t = 1f - s.flashAmount;
            sprite(g, SPRITE_SHOCKWAVE, cx, cy, sz * (0.9f + 0.8f * t), 0, 1f, 1f, 1f, 1f - t);
        }
        if (s.burst >= 0) {
            float t = s.burst;
            sprite(g, SPRITE_SHOCKWAVE, cx, cy, sz * (0.8f + 1.6f * t), 0, 1f, 1f, 1f, 1f - t);
            sprite(g, SPRITE_GLOW, cx, cy, sz * 1.6f, 0, 1f, 1f, 1f, 1f - t);
        }

        // 6) Counter ausgeloest: weisser Blitz, grosse Welle, Splitter fliegen weg
        if (used > 0) {
            float t = 1f - used;
            sprite(g, SPRITE_GLOW, cx, cy, sz * 1.7f, 0, 1f, 1f, 1f, used * used);
            sprite(g, SPRITE_SHOCKWAVE, cx, cy, sz * (0.8f + 2.0f * t), 0, 1f, 1f, 1f, used);
            for (int i = 0; i < 8; i++) {
                double ang = Math.toRadians(i * 45.0 + 20.0);
                float dist = sz * (0.3f + 0.9f * t);
                sprite(g, SPRITE_SPARK, cx + (float) Math.cos(ang) * dist, cy + (float) Math.sin(ang) * dist,
                        sz * 0.28f * (1f - t * 0.5f), i * 45f, 1f, 1f, 1f, used);
            }
        }

        // 7) Wieder bereit: kurzes "Ping"
        if (ping > 0) {
            float t = 1f - ping;
            sprite(g, SPRITE_SHOCKWAVE, cx, cy, sz * (0.9f + 0.6f * t), 0, 1f, 1f, 1f, ping * 0.8f);
        }

        // 8) Power-Up: blaue Ki-Funken steigen vom Kreis auf
        if (buff) {
            for (int i = 0; i < 5; i++) {
                float cycle = ((now + i * 260L) % 1300L) / 1300f;
                float x = cx + (float) Math.sin(i * 1.7 + now / 400.0) * sz * 0.3f;
                float y = cy + sz * 0.2f - cycle * sz * 0.9f;
                float alpha = (float) Math.sin(Math.PI * cycle);
                sprite(g, SPRITE_SPARK, x, y, sz * 0.18f, 0, 0.6f, 0.9f, 1f, alpha);
            }
        }

        normal();
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static void additive() {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    private static void normal() {
        RenderSystem.defaultBlendFunc();
    }

    /** Zeichnet eine Grafik aus dem Raster, zentriert auf (cx, cy), optional gedreht. */
    private static void sprite(GuiGraphics g, int index, float cx, float cy, float size, float rotDeg,
                               float r, float gr, float b, float a) {
        if (a <= 0.01f || size < 1f) return;
        int u = (index % 8) * CELL;
        int v = (index / 8) * CELL;
        g.setColor(r, gr, b, Math.min(1f, a));
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        if (rotDeg != 0f) g.pose().mulPose(Axis.ZP.rotationDegrees(rotDeg));
        g.pose().scale(size / CELL, size / CELL, 1f);
        g.blit(SHEET, -CELL / 2, -CELL / 2, CELL, CELL, u, v, CELL, CELL, SHEET_SIZE, SHEET_SIZE);
        g.pose().popPose();
    }

    interface PixelSource {
        int colorAt(int dx, int dy);
    }

    /** Zeichnet zeilenweise; gleichfarbige Pixel werden zu einem Rechteck zusammengefasst. */
    private static void drawPixels(GuiGraphics g, int cx, int cy, int e, PixelSource src) {
        for (int dy = -e - 1; dy <= e; dy++) {
            int runStart = 0;
            int runColor = 0;
            for (int dx = -e - 1; dx <= e + 1; dx++) {
                int color = dx <= e ? src.colorAt(dx, dy) : 0;
                if (color != runColor) {
                    if (runColor != 0) {
                        g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColor);
                    }
                    runColor = color;
                    runStart = dx;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Impact beim Finisher
    // ------------------------------------------------------------------
    private static final long SPEED_FROM = 120;
    private static final long SPEED_TO = 460;

    /** Weisse Linien, die vom Bildschirmrand zur Mitte zeigen. Wechseln alle 40 ms fuer Bewegung. */
    private static void drawSpeedLines(GuiGraphics g, int width, int height, long ms) {
        float fade = ms < SPEED_FROM + 40 ? (ms - SPEED_FROM) / 40f
                : ms > SPEED_TO - 80 ? (SPEED_TO - ms) / 80f : 1f;
        fade = Math.max(0f, Math.min(1f, fade));
        java.util.Random rnd = new java.util.Random(ms / 40);
        float half = (float) Math.sqrt((double) width * width + (double) height * height) / 2f;
        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0f);
        for (int i = 0; i < 46; i++) {
            float angle = rnd.nextFloat() * 360f;
            float r0 = half * (0.38f + rnd.nextFloat() * 0.3f);
            int thick = 1 + rnd.nextInt(2);
            int alpha = (int) ((90 + rnd.nextInt(130)) * fade);
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            g.fill((int) r0, 0, (int) half + 4, thick, (alpha << 24) | 0xFFFFFF);
            g.pose().popPose();
        }
        g.pose().popPose();
    }

    private static final long IMPACT_DARK_MILLIS = 200;
    private static final long IMPACT_FLASH_MILLIS = 230;

    /** Schlag-Streifen: {Abstand von der Mitte (-1..1), Dicke, Startverzoegerung (0..1), Laenge}. */
    private static final float[][] SLASHES = {
            {-0.05f, 5f, 0.00f, 1.00f},
            {0.10f, 2f, 0.08f, 0.90f},
            {-0.22f, 3f, 0.15f, 0.80f},
            {0.28f, 1f, 0.05f, 0.95f},
            {-0.38f, 1f, 0.25f, 0.70f},
            {0.45f, 4f, 0.20f, 0.85f},
            {0.62f, 1f, 0.30f, 0.60f},
            {-0.60f, 2f, 0.35f, 0.75f},
    };

    private static void drawImpact(GuiGraphics g, int width, int height, long ms) {
        if (ms < IMPACT_DARK_MILLIS) {
            // 1) Bildschirm wird schlagartig dunkel
            int dark = ms < 30 ? (int) (ms / 30f * 230) : 230;
            g.fill(0, 0, width, height, (dark << 24) | 0x070B22);

            // 2) Weisse Streifen schiessen schraeg durchs Bild
            int diag = (int) Math.sqrt((double) width * width + (double) height * height);
            float sweep = ms / (float) IMPACT_DARK_MILLIS;
            g.pose().pushPose();
            g.pose().translate(width / 2f, height / 2f, 0f);
            g.pose().mulPose(Axis.ZP.rotationDegrees(-58f));
            for (float[] sl : SLASHES) {
                float t = sweep - sl[2];
                if (t <= 0f) continue;
                int len = (int) (diag * sl[3] * Math.min(1f, t * 3.5f));
                int y = (int) (sl[0] * height * 0.5f);
                int alpha = (int) (255 * Math.max(0f, Math.min(1f, 1.6f - t * 1.8f)));
                if (alpha <= 0) continue;
                int x0 = -diag / 2;
                g.fill(x0, y, x0 + len, y + (int) sl[1], (alpha << 24) | 0xFFFFFF);
            }
            g.pose().popPose();
        } else {
            // 3) Gelb-oranger Knall, der ausblendet
            float t = (ms - IMPACT_DARK_MILLIS) / (float) IMPACT_FLASH_MILLIS;
            int alpha = (int) ((1f - t) * (1f - t) * 210);
            int color = GaugePainter.lerp(0xFFFFF4C0, 0xFFFF9A30, Math.min(1f, t * 2f)) & 0xFFFFFF;
            g.fill(0, 0, width, height, (alpha << 24) | color);
        }
    }
}
