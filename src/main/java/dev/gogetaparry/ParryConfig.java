package dev.gogetaparry;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Einstellungen, landen nach dem ersten Start in: .minecraft/config/gogetaparry-common.toml
 * Damit kannst du das Balancing aendern, ohne neu zu kompilieren.
 */
public final class ParryConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue REQUIRE_AMULET;
    public static final ForgeConfigSpec.DoubleValue CRYSTAL_CHANCE;
    public static final ForgeConfigSpec.IntValue HITS_TO_CHARGE;
    public static final ForgeConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue ENV_CHARGE;
    public static final ForgeConfigSpec.IntValue BUFF_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BUFF_PERCENT;
    public static final ForgeConfigSpec.DoubleValue BUFF_SPEED;
    public static final ForgeConfigSpec.DoubleValue COUNTER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SHOCKWAVE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SHOCKWAVE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue FACE_ATTACKER;
    public static final ForgeConfigSpec.BooleanValue VOICE;
    public static final ForgeConfigSpec.DoubleValue HEAL_ON_FULL;
    public static final ForgeConfigSpec.DoubleValue HEAL_ON_COUNTER;
    public static final ForgeConfigSpec.BooleanValue TELEPORT_COUNTER;
    public static final ForgeConfigSpec.DoubleValue COUNTER_RANGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("parry");

        REQUIRE_AMULET = b
                .comment("true = Parry nur aktiv, wenn das Fusions-Amulett im Inventar ist. false = immer aktiv.")
                .define("requireAmulet", true);
        CRYSTAL_CHANCE = b
                .comment("Chance in Prozent, einen Ki-Kristall in einer Dungeon- oder Netherfestungs-Truhe zu finden.",
                        "Bastion-Schatzkammer: doppelt so viel, andere Bastion-Truhen: 2/3. Gilt fuer neu erzeugte Truhen.")
                .defineInRange("kiCrystalChestChancePercent", 15.0, 0.0, 100.0);
        HITS_TO_CHARGE = b
                .comment("Wie viele Treffer der Kreis braucht, bis er voll ist.")
                .defineInRange("hitsToFill", 5, 1, 20);
        ENV_CHARGE = b
                .comment("Wie viel Schaden ohne Angreifer (Fallen, Explosion, Feuer ...) laedt: 0.5 = ein halber Treffer.",
                        "Zaehlt hoechstens einmal pro Sekunde. 0 = gar nicht.")
                .defineInRange("environmentCharge", 0.5, 0.0, 1.0);
        COOLDOWN_SECONDS = b
                .comment("Abklingzeit nach einem Konter, bevor sich der Kreis wieder aufladen kann.")
                .defineInRange("cooldownAfterCounterSeconds", 20, 0, 600);
        BUFF_SECONDS = b
                .comment("Wie lange du nach dem Konter mehr Schaden machst (Ki-Aura).")
                .defineInRange("powerUpTimeSeconds", 10, 0, 120);
        BUFF_SPEED = b
                .comment("Wie viel Prozent schneller du waehrend des Power-Ups laeufst.")
                .defineInRange("powerUpSpeedPercent", 10.0, 0.0, 200.0);
        BUFF_PERCENT = b
                .comment("Wie viel Prozent mehr Schaden waehrend des Power-Ups.")
                .defineInRange("powerUpDamagePercent", 30.0, 0.0, 500.0);
        COUNTER_DAMAGE = b
                .comment("Gesamtschaden des Rush-Konters (2 = 1 Herz), verteilt auf 4 Schlaege: 15% / 15% / 20% / 50%.",
                        "40 = Zombies und Skelette (20 Leben) werden mit einem Konter besiegt.")
                .defineInRange("counterDamageTotal", 40.0, 0.0, 1000.0);
        SHOCKWAVE_DAMAGE = b
                .comment("Schaden der blauen Energiewelle an Monstern in der Naehe.")
                .defineInRange("shockwaveDamage", 3.0, 0.0, 1000.0);
        SHOCKWAVE_RADIUS = b
                .comment("Radius der blauen Energiewelle in Bloecken.")
                .defineInRange("shockwaveRadius", 4.0, 0.0, 16.0);
        HEAL_ON_FULL = b
                .comment("Wie viele Herzen du zurueckbekommst, sobald der Kreis voll ist (wie Gogetas Unique Gauge).")
                .defineInRange("healOnFullHearts", 5.0, 0.0, 50.0);
        HEAL_ON_COUNTER = b
                .comment("Heilung in Prozent der maximalen Leben, wenn der Konter-Kick trifft.")
                .defineInRange("healOnCounterPercent", 10.0, 0.0, 100.0);
        VOICE = b
                .comment("true = Stimme/Ki-Schrei beim Parry und beim Kick abspielen (sounds/voice/parry.ogg und kick.ogg).")
                .define("voice", true);
        FACE_ATTACKER = b
                .comment("true = beim Parry drehst du dich blitzschnell zum Angreifer (wie Gogeta).")
                .define("faceAttacker", true);
        TELEPORT_COUNTER = b
                .comment("true = beim Konter teleportierst du dich hinter den Angreifer (z. B. zum Skelett).")
                .define("teleportCounter", true);
        COUNTER_RANGE = b
                .comment("Maximale Entfernung zum Angreifer fuer den Konter.")
                .defineInRange("counterRange", 24.0, 1.0, 64.0);

        b.pop();
        SPEC = b.build();
    }

    private ParryConfig() {
    }
}
