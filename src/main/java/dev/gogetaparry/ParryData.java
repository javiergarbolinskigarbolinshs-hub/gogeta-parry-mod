package dev.gogetaparry;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Parry-Zustand eines Spielers (nur auf dem Server). */
public class ParryData {
    /** Aktuelle Ladung des Kreises (0 .. hitsToCharge). */
    public float charge;
    /** Spielzeit (Ticks) des letzten Treffers, fuer das Leerlaufen des Kreises. */
    public long lastHitTick;
    /** Wann zuletzt Umgebungs-Schaden geladen hat (hoechstens 1x pro Sekunde). */
    public long lastEnvChargeTick = -1000;

    /** Power-Up nach dem Konter: verbleibende Ticks (mehr Schaden + Ki-Aura). */
    public int buffTicks;

    /** Abklingzeit nach dem Konter in Ticks. */
    public int cooldown;
    public int cooldownMax;

    /** Kurze Unverwundbarkeit direkt nach dem Parry (fuer Explosionen, mehrere Mobs gleichzeitig). */
    public int grace;

    /** Ticks bis zum Konterschlag, -1 = kein Konter geplant. */
    public int counterDelay = -1;
    public UUID counterTarget;

    /** Konter-Kick: Ticks seit Start (-1 = kein Kick) und Ziel. */
    public int rushTick = -1;
    public UUID rushTarget;
    /** true = der geparryte Angriff kam aus der Ferne (Pfeil, Feuerball ...) -> Ki-Blast statt Kick. */
    public boolean counterRanged;
    /** Ki-Blast: Ticks seit Start (-1 = keiner), Ziel, Startpunkt und Flugzeit in Ticks. */
    public int blastTick = -1;
    public UUID blastTarget;
    public Vec3 blastFrom;
    public int blastFlight;

    /** Wo der Spieler beim Kick in der Luft steht und wohin er schaut. */
    public Vec3 kickDest;
    public float kickYaw;
    public float kickPitch;

    /** Animationsschritt der blauen Energiewelle, -1 = keine Welle. */
    public int waveStep = -1;
    public Vec3 waveCenter;

    public boolean hasAmulet;

    /** true = Zustand muss an den Client geschickt werden. */
    public boolean dirty = true;
    /** true = Client soll den blauen Bildschirm-Blitz + Kamera-Wackeln zeigen. */
    public boolean flashPending;
    /** true = Client soll den Impact-Moment zeigen (dunkler Bildschirm, weisse Streifen). */
    public boolean impactPending;
    /** Wie viel gerade geheilt wurde (fuer die +X-Anzeige am Kreis), 0 = nichts. */
    public float healPending;
    /** Kreis wurde gerade voll -> im naechsten Tick heilen (nach dem Schaden des Treffers). */
    public boolean fullHealPending;
}
