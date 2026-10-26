package dev.gogetaparry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Merkt sich auf dem Client, welche Spieler gerade parryen / kicken und seit wann.
 * Ohne Client-Imports, damit die Klasse auch auf dem Server geladen werden darf.
 */
public final class ClientParryFx {
    /** Wie lange Arm-Pose und Ki-Welle sichtbar sind. */
    public static final long DURATION_MILLIS = 650;

    /** Ablauf des Konter-Kicks (Millisekunden ab Start), passend zu den Ticks in ParryEvents. */
    public static final long KICK_CROUCH_END = 150;  // Hocke
    public static final long KICK_DASH_END = 350;    // Sprint zum Gegner
    public static final long KICK_SPIN_END = 550;    // Drehung in der Luft
    public static final long KICK_DURATION = 900;    // Kick trifft bei 550, danach kurz halten

    /** Ki-Blast: Aufladen bis 300 ms, dann Flug, danach noch kurz die Pose halten. */
    public static final long BLAST_CHARGE_END = 300;
    public static final long BLAST_AFTER = 500;

    /** Ein laufender Ki-Blast eines Spielers. */
    public static final class Blast {
        public final long start;
        public final float yawOffset;
        public final int targetId;
        public long fireMillis = -1;   // -1 = noch nicht abgefeuert
        public long flightMillis;

        Blast(long start, float yawOffset, int targetId) {
            this.start = start;
            this.yawOffset = yawOffset;
            this.targetId = targetId;
        }

        /** Wann die Animation vorbei ist. */
        public long end() {
            return fireMillis < 0 ? start + 1500 : fireMillis + flightMillis + BLAST_AFTER;
        }
    }

    private static final Map<Integer, Long> STARTS = new HashMap<>();
    private static final Map<Integer, Blast> BLASTS = new HashMap<>();
    private static final Map<Integer, Long> KICKS = new HashMap<>();
    private static final Map<Integer, Float> KICK_YAW = new HashMap<>();

    private ClientParryFx() {
    }

    public static void start(int entityId) {
        STARTS.put(entityId, System.currentTimeMillis());
    }

    public static void startBlast(int entityId, float yawOffset, int targetId) {
        BLASTS.put(entityId, new Blast(System.currentTimeMillis(), yawOffset, targetId));
    }

    public static void fireBlast(int entityId, int targetId, int flightTicks) {
        Blast b = BLASTS.get(entityId);
        if (b == null) {
            b = new Blast(System.currentTimeMillis() - BLAST_CHARGE_END, 0f, targetId);
            BLASTS.put(entityId, b);
        }
        b.fireMillis = System.currentTimeMillis();
        b.flightMillis = flightTicks * 50L;
    }

    /** Der laufende Ki-Blast dieses Spielers, oder null. */
    public static Blast blast(int entityId) {
        Blast b = BLASTS.get(entityId);
        if (b == null) return null;
        if (System.currentTimeMillis() > b.end()) {
            BLASTS.remove(entityId);
            return null;
        }
        return b;
    }

    public static Map<Integer, Blast> blasts() {
        long now = System.currentTimeMillis();
        BLASTS.values().removeIf(b -> now > b.end());
        return BLASTS;
    }

    public static void startKick(int entityId, float yawOffset) {
        KICKS.put(entityId, System.currentTimeMillis());
        KICK_YAW.put(entityId, yawOffset);
    }

    /** Millisekunden seit dem Parry dieses Spielers, oder -1 wenn er gerade nicht parryt. */
    public static long elapsed(int entityId) {
        Long start = STARTS.get(entityId);
        if (start == null) return -1;
        long e = System.currentTimeMillis() - start;
        return (e >= 0 && e < DURATION_MILLIS) ? e : -1;
    }

    /** Millisekunden seit Beginn des Konter-Kicks, oder -1 wenn gerade kein Kick laeuft. */
    public static long kickElapsed(int entityId) {
        Long start = KICKS.get(entityId);
        if (start == null) return -1;
        long e = System.currentTimeMillis() - start;
        return (e >= 0 && e < KICK_DURATION) ? e : -1;
    }

    /** Um wie viel Grad der Spieler beim Kick neben den Gegner schaut (damit die Kamera beide zeigt). */
    public static float kickYawOffset(int entityId) {
        Float f = KICK_YAW.get(entityId);
        return f == null ? 0f : f;
    }

    public static Map<Integer, Long> active() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> it = STARTS.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() >= DURATION_MILLIS) it.remove();
        }
        return STARTS;
    }

    public static void reset() {
        STARTS.clear();
        KICKS.clear();
        KICK_YAW.clear();
        BLASTS.clear();
    }
}
