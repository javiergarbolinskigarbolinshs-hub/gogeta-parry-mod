package dev.gogetaparry.client;

/**
 * Berechnet Pixel fuer Pixel, wie der Parry-Kreis (Unique Gauge im Dragon-Ball-Legends-Stil) aussieht.
 * Reine Mathematik ohne Minecraft-Klassen, damit man sie auch ausserhalb des Spiels testen kann.
 *
 * Aufbau von innen nach aussen:
 *  - dunkle Scheibe (dahinter liegt das Amulett-Symbol)
 *  - dunkler Rand
 *  - der eigentliche Ring mit Segmenten (leer = dunkelgrau, gefuellt = blau)
 *  - wenn voll: pulsierender Lichtschein, kreisende Funken, Lade-Explosion
 */
public final class GaugePainter {

    public static final class State {
        public int charge;
        public int max = 3;
        public boolean full;
        public boolean cooldown;
        /** 0..1, wie weit die Abklingzeit schon um ist. */
        public float cooldownProgress;
        /** Segment, das gerade frisch geladen wurde (-1 = keins). */
        public int flashSegment = -1;
        /** 0..1, wie stark das frische Segment weiss aufblitzt. */
        public float flashAmount;
        /** 0..1 Fortschritt der Lade-Explosion, wenn der Kreis gerade voll wurde (-1 = keine). */
        public float burst = -1;
        public long timeMillis;
    }

    private static final int BACKING = 0xE0081428;
    private static final int EMPTY = 0xC0263447;
    private static final int FILL_INNER = 0xFF1E6FE0;
    private static final int FILL_OUTER = 0xFF5CC8FF;
    private static final int FULL_A = 0xFF4FC3FF;
    private static final int FULL_B = 0xFFEAFBFF;
    private static final int CD_DONE = 0xFF9AA3AE;
    private static final int CD_BG = 0xC02A2F38;
    private static final int DISC = 0xA0050A14;
    private static final int DISC_FULL = 0xB0123E70;
    private static final int GLOW_RGB = 0x4FC3FF;
    private static final int BURST_RGB = 0xBFF0FF;
    private static final int WHITE = 0xFFFFFFFF;

    private GaugePainter() {
    }

    /** Wie weit (in Pixeln vom Mittelpunkt) ueberhaupt etwas gezeichnet wird. */
    public static int extent(int r, State s) {
        int e = r + (s.full ? 7 : 2);
        if (s.burst >= 0) e = Math.max(e, r + 15);
        return e;
    }

    /** ARGB-Farbe des Pixels (dx, dy) relativ zum Mittelpunkt, 0 = durchsichtig. */
    public static int colorAt(int dx, int dy, int r, int thick, State s) {
        double fx = dx + 0.5;
        double fy = dy + 0.5;
        double dist = Math.sqrt(fx * fx + fy * fy);
        double inner = r - thick;
        double t = s.timeMillis;
        double pulse = 0.5 + 0.5 * Math.sin(t / 140.0);

        // Winkel: 0 = oben, im Uhrzeigersinn
        double angle = Math.toDegrees(Math.atan2(fy, fx)) + 90.0;
        if (angle < 0) angle += 360.0;

        int color = 0;

        if (dist < inner - 1) {
            // Innere Scheibe
            color = s.full ? lerp(DISC, DISC_FULL, (float) pulse) : DISC;
        } else if (dist < inner) {
            color = BACKING;
        } else if (dist < r) {
            color = ringColor(dist, angle, inner, thick, s, t, pulse);
        } else if (dist < r + 1) {
            color = BACKING;
        } else if (s.full) {
            // Lichtschein um den vollen Kreis
            double hw = 3.0 + 2.0 * pulse;
            double d = dist - (r + 1);
            if (d < hw) {
                double a = 1.0 - d / hw;
                int alpha = (int) (a * a * (110 + 60 * pulse));
                color = (clamp(alpha) << 24) | GLOW_RGB;
            }
        }

        // Lade-Explosion: Ring, der nach aussen fliegt
        if (s.burst >= 0 && dist >= r + 1) {
            double br = r + 2 + s.burst * 11.0;
            if (Math.abs(dist - br) < 1.0) {
                int alpha = (int) ((1.0 - s.burst) * 220);
                color = over(color, (clamp(alpha) << 24) | BURST_RGB);
            }
        }

        // Kreisende Funken, wenn voll
        if (s.full) {
            double orbit = r + 3.0;
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(t / 3.5 + i * 120.0);
                double sx = Math.sin(a) * orbit;
                double sy = -Math.cos(a) * orbit;
                double ds = Math.hypot(fx - sx, fy - sy);
                if (ds < 1.25) {
                    color = WHITE;
                } else {
                    double ta = a - Math.toRadians(14);
                    double tx = Math.sin(ta) * orbit;
                    double ty = -Math.cos(ta) * orbit;
                    if (Math.hypot(fx - tx, fy - ty) < 0.9) {
                        color = over(color, 0xB0000000 | GLOW_RGB);
                    }
                }
            }
        }

        return color;
    }

    private static int ringColor(double dist, double angle, double inner, int thick, State s, double t, double pulse) {
        if (s.cooldown) {
            return (angle / 360.0 <= s.cooldownProgress) ? CD_DONE : CD_BG;
        }

        if (s.full) {
            // Ganzer Ring leuchtet, Lichtwelle laeuft ringsum
            double shimmer = 0.5 + 0.5 * Math.sin(Math.toRadians(angle) * 2.0 - t / 110.0);
            shimmer = shimmer * shimmer * shimmer;
            float depth = (float) ((dist - inner) / thick);
            int base = lerp(FILL_INNER, FULL_A, 0.45f + 0.55f * depth);
            float mix = (float) (0.1 * pulse + 0.8 * shimmer);
            return lerp(base, FULL_B, mix);
        }

        int max = Math.max(1, s.max);
        double segment = 360.0 / max;
        double gap = max > 1 ? 12.0 : 0.0;
        int seg = (int) (angle / segment);
        double inSeg = angle - seg * segment;
        if (inSeg < gap / 2 || inSeg > segment - gap / 2) {
            return BACKING; // Luecke zwischen den Segmenten
        }

        if (seg < s.charge) {
            float depth = (float) ((dist - inner) / thick);
            int c = lerp(FILL_INNER, FILL_OUTER, depth);
            if (seg == s.flashSegment && s.flashAmount > 0) {
                c = lerp(c, WHITE, s.flashAmount);
            }
            return c;
        }
        return EMPTY;
    }

    // ---------- Farb-Helfer ----------

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int oa = (int) (aa + (ba - aa) * t);
        int or = (int) (ar + (br - ar) * t);
        int og = (int) (ag + (bg - ag) * t);
        int ob = (int) (ab + (bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    /** Legt Farbe 'top' ueber 'bottom' (Alpha-Blending). */
    static int over(int bottom, int top) {
        int ta = top >>> 24;
        if (ta == 255 || bottom == 0) return top;
        if (ta == 0) return bottom;
        float a = ta / 255f;
        int ba = bottom >>> 24;
        int outA = (int) (ta + ba * (1 - a));
        int r = (int) (((top >> 16) & 255) * a + ((bottom >> 16) & 255) * (1 - a));
        int g = (int) (((top >> 8) & 255) * a + ((bottom >> 8) & 255) * (1 - a));
        int b = (int) ((top & 255) * a + (bottom & 255) * (1 - a));
        return (clamp(outA) << 24) | (r << 16) | (g << 8) | b;
    }
}
