package dev.gogetaparry;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Partikel und Sounds (laufen auf dem Server, alle Spieler in der Naehe sehen/hoeren sie). */
public final class ParryEffects {
    /** Das typische Gogeta-Blau. */
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.30f, 0.75f, 1.00f), 1.6f);
    private static final DustParticleOptions LIGHT_BLUE = new DustParticleOptions(new Vector3f(0.75f, 0.95f, 1.00f), 1.0f);

    private ParryEffects() {
    }

    /** Ein Treffer hat den Kreis um ein Stueck aufgeladen. */
    public static void chargeStep(ServerPlayer player, float charge, int max) {
        ServerLevel level = player.serverLevel();
        float pitch = 0.9f + 0.6f * charge / (float) Math.max(1, max);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, pitch);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.5, 0.4, 0.05);
    }

    /** Der Kreis ist voll – bereit zum Parry. */
    public static void fullyCharged(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.7f);
        ring(level, player.position().add(0, 0.1, 0), 1.0, 24, BLUE);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
    }

    /** Der eigentliche Parry-Moment: Klingen-Klang, Blitz an der Hand. */
    public static void parry(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        // Position der Handflaeche (dort entsteht die Ki-Welle)
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(1.05)).add(0, -0.15, 0);

        // Stimme / Ki-Schrei beim Parry (austauschbar: sounds/voice/parry.ogg)
        if (ParryConfig.VOICE.get()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.VOICE_PARRY.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        // "Fyuu" - blitzschnelle Bewegung
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 2.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.6f);
        // "Kiiin" - die Ki-Welle loest den Angriff auf
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5f, 2.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 2.0f);

        level.sendParticles(ParticleTypes.FLASH, hand.x, hand.y, hand.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 30, 0.2, 0.2, 0.2, 0.6);
        level.sendParticles(ParticleTypes.END_ROD, hand.x, hand.y, hand.z, 12, 0.1, 0.1, 0.1, 0.25);

        // Ki-Funken, die von der Welle nach vorne/aussen wegstieben (wie die blauen Funken in Legends)
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < 45; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 2.2;
            double oy = (level.random.nextDouble() - 0.5) * 1.8;
            double oz = (level.random.nextDouble() - 0.5) * 2.2;
            Vec3 p = hand.add(look.scale(0.3 + level.random.nextDouble() * 0.6)).add(ox, oy, oz);
            level.sendParticles(i % 3 == 0 ? LIGHT_BLUE : BLUE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                hand.x + look.x * 0.6, hand.y + look.y * 0.6, hand.z + look.z * 0.6, 25, 0.9, 0.7, 0.9, 0.3);
    }

    /** Ein Schritt der sich ausbreitenden blauen Energiewelle. */
    public static void waveStep(ServerLevel level, Vec3 center, int step) {
        double radius = 0.8 + step * 1.0;
        int points = 20 + step * 10;
        ring(level, center, radius, points, BLUE);
        ring(level, center.add(0, 0.4, 0), radius * 0.85, points / 2, LIGHT_BLUE);
        if (step == 0) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        }
    }

    /** Blaue Spur beim Teleport (wie die Momentane Teleportation). */
    public static void teleportTrail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 diff = to.subtract(from);
        int steps = (int) Math.max(1, diff.length() / 0.4);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(diff.scale(i / (double) steps));
            level.sendParticles(BLUE, p.x, p.y + 1.0, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    // ---------------- Ki-Blast (Konter gegen Fernkampf) ----------------

    /** Ki-Blast laedt auf: tiefes Summen. */
    public static void blastCharge(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 1.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7f, 0.9f);
    }

    /** Ki sammelt sich zwischen den Haenden: Funken ziehen nach innen. */
    public static void blastGather(ServerLevel level, Vec3 hands) {
        RandomSource r = level.random;
        for (int i = 0; i < 4; i++) {
            Vec3 from = hands.add((r.nextDouble() - 0.5) * 1.6, (r.nextDouble() - 0.5) * 1.2, (r.nextDouble() - 0.5) * 1.6);
            Vec3 v = hands.subtract(from);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 0, v.x, v.y, v.z, 0.25);
        }
        level.sendParticles(LIGHT_BLUE, hands.x, hands.y, hands.z, 3, 0.08, 0.08, 0.08, 0.0);
    }

    /** Abfeuern: lauter Energiestoss. */
    public static void blastFire(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.7f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 0.7f);
        Vec3 hands = player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0, -0.35, 0);
        level.sendParticles(ParticleTypes.FLASH, hands.x, hands.y, hands.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 10, 0.4, 0.05, 0.4, 0.06);
    }

    /** Leuchtspur hinter der fliegenden Ki-Kugel. */
    public static void blastTrail(ServerLevel level, Vec3 pos) {
        level.sendParticles(BLUE, pos.x, pos.y, pos.z, 8, 0.15, 0.15, 0.15, 0.0);
        level.sendParticles(LIGHT_BLUE, pos.x, pos.y, pos.z, 4, 0.1, 0.1, 0.1, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.02);
    }

    /** Einschlag: blau-weisse Ki-Explosion (dazu kommt der normale Finisher). */
    public static void blastHit(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.6, z = target.getZ();
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(BLUE, x, y, z, 60, 0.8, 0.8, 0.8, 0.0);
        level.sendParticles(LIGHT_BLUE, x, y, z, 30, 0.6, 0.6, 0.6, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 40, 0.4, 0.4, 0.4, 0.9);
        ring(level, new Vec3(x, target.getY() + 0.1, z), 2.0, 36, BLUE);
    }

    /** Power-Up beginnt: Ki flammt auf. */
    public static void powerUp(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.5f);
        ring(level, player.position().add(0, 0.1, 0), 1.0, 28, BLUE);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.7, 0.4, 0.2);
    }

    /** Ki-Aura waehrend des Power-Ups: blaue Flammen, die am Koerper hochsteigen. */
    public static void aura(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource r = level.random;
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double rad = 0.35 + r.nextDouble() * 0.2;
            double x = player.getX() + Math.cos(a) * rad;
            double z = player.getZ() + Math.sin(a) * rad;
            double y = player.getY() + r.nextDouble() * 1.8;
            level.sendParticles(i % 3 == 0 ? LIGHT_BLUE : BLUE, x, y, z, 0, 0, 1, 0, 0.12);
        }
        if (r.nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.3, 0.6, 0.3, 0.05);
        }
    }

    /** Heilung: Herzen und gruen-blaues Glitzern. */
    public static void heal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.35f, 1.9f);
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.9, player.getZ(), 5, 0.45, 0.25, 0.45, 0.0);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.4, 0.6, 0.4, 0.0);
    }

    /** Konter-Kick, Phase 1: in die Hocke gehen und Ki sammeln. */
    public static void kickCharge(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.8f);
        // blauer Ki-Ring am Boden, der sich zusammenzieht
        ring(level, player.position().add(0, 0.1, 0), 1.3, 24, BLUE);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 0.6, player.getZ(), 20, 0.4, 0.3, 0.4, 0.15);
    }

    /** Konter-Kick, Phase 2: Absprung Richtung Gegner. */
    public static void dash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.9f, 1.4f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 2.0f);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 12, 0.3, 0.05, 0.3, 0.08);
        level.sendParticles(BLUE, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.5, 0.1, 0.5, 0.0);
    }

    /** Blaue Ki-Spur hinter dem Spieler waehrend des Sprints. */
    public static void dashTrail(ServerLevel level, Vec3 pos) {
        level.sendParticles(BLUE, pos.x, pos.y + 1.0, pos.z, 10, 0.25, 0.45, 0.25, 0.0);
        level.sendParticles(LIGHT_BLUE, pos.x, pos.y + 1.0, pos.z, 4, 0.2, 0.4, 0.2, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.0, pos.z, 4, 0.2, 0.4, 0.2, 0.05);
    }

    /** Konter-Kick, Phase 3: Drehung in der Luft. */
    public static void spin(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
        double y = player.getY() + 1.0;
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX() + Math.cos(a) * 0.9, y, player.getZ() + Math.sin(a) * 0.9, 1, 0, 0, 0, 0);
        }
        ring(level, new Vec3(player.getX(), y, player.getZ()), 1.1, 20, LIGHT_BLUE);
    }

    private static final DustParticleOptions ORANGE = new DustParticleOptions(new Vector3f(1.00f, 0.62f, 0.15f), 2.0f);
    private static final DustParticleOptions YELLOW = new DustParticleOptions(new Vector3f(1.00f, 0.92f, 0.45f), 1.4f);

    /** Ein Schlag aus der Rush-Kombo trifft. */
    public static void comboHit(ServerPlayer player, LivingEntity target, int step) {
        ServerLevel level = player.serverLevel();
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.55;
        double z = target.getZ();

        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.9f + step * 0.15f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7f, 1.2f);

        level.sendParticles(ParticleTypes.CRIT, x, y, z, 15, 0.3, 0.4, 0.3, 0.5);
        level.sendParticles(BLUE, x, y, z, 12, 0.35, 0.4, 0.35, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 10, 0.25, 0.3, 0.25, 0.7);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
    }

    /** Der Abschluss-Kick: gelb-orange Explosion, Gegner fliegt weg. */
    public static void finisher(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        // Stimme / Ki-Schrei beim Kick (austauschbar: sounds/voice/kick.ogg)
        if (ParryConfig.VOICE.get()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.VOICE_KICK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.7f);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.5f);
        level.playSound(null, x, y, z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);

        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 3, 0.4, 0.4, 0.4, 0);
        level.sendParticles(ORANGE, x, y, z, 40, 0.6, 0.6, 0.6, 0.0);
        level.sendParticles(YELLOW, x, y, z, 30, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 25, 0.2, 0.2, 0.2, 0.25);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 30, 0.5, 0.5, 0.5, 0.8);
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, int points, DustParticleOptions dust) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            level.sendParticles(dust, px, center.y, pz, 1, 0, 0.05, 0, 0);
        }
    }
}
