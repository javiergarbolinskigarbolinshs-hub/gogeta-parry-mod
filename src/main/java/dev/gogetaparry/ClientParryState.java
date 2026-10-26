package dev.gogetaparry;

import dev.gogetaparry.network.ParryStatePacket;

/**
 * Was der Client ueber den Ladekreis weiss.
 * Absichtlich ohne Client-Imports, damit die Klasse auch auf einem Server geladen werden darf.
 */
public final class ClientParryState {
    public static float charge;
    /** Ende des Power-Ups (Systemzeit) und Gesamtdauer. */
    public static long buffEndMillis;
    public static int maxCharge = 3;
    public static boolean active;

    public static long cooldownEndMillis;
    public static long cooldownTotalMillis = 1;

    /** Zeitpunkt des letzten Parrys (fuer Bildschirm-Blitz und Kamera-Wackeln). */
    public static long flashStartMillis = -100000L;

    /** Letzte Heilung (fuer die gruene +X-Anzeige am Kreis). */
    public static float healAmount;
    public static long healMillis = -100000L;

    /** Zeitpunkt des Konter-Treffers (dunkler Bildschirm mit weissen Streifen). */
    public static long impactStartMillis = -100000L;

    /** Wann zuletzt ein Segment dazukam und welches (fuer das kurze Aufblitzen). */
    public static long lastChargeMillis = -100000L;
    public static int lastChargeSegment = -1;

    /** Wann der Kreis voll wurde (fuer die Lade-Explosion). */
    public static long fullSinceMillis = -100000L;

    private ClientParryState() {
    }

    public static void apply(ParryStatePacket packet) {
        long now = System.currentTimeMillis();
        int newMax = Math.max(1, packet.maxCharge);

        if (packet.charge > charge) {
            lastChargeMillis = now;
            lastChargeSegment = (int) Math.ceil(packet.charge) - 1;
            if (packet.charge >= newMax) {
                fullSinceMillis = now;
            }
        }

        charge = packet.charge;
        buffEndMillis = packet.buffTicks > 0 ? now + packet.buffTicks * 50L : 0;
        maxCharge = newMax;
        active = packet.active;
        cooldownEndMillis = now + packet.cooldownTicks * 50L;
        cooldownTotalMillis = Math.max(1, packet.cooldownMax * 50L);
        if (packet.flash) {
            flashStartMillis = now;
        }
        if (packet.heal > 0) {
            healAmount = packet.heal;
            healMillis = now;
        }
        if (packet.impact) {
            impactStartMillis = now;
        }
    }

    public static void reset() {
        charge = 0;
        active = false;
        cooldownEndMillis = 0;
        flashStartMillis = -100000L;
        impactStartMillis = -100000L;
        lastChargeMillis = -100000L;
        lastChargeSegment = -1;
        fullSinceMillis = -100000L;
    }
}
