package dev.gogetaparry;

import dev.gogetaparry.network.ModNetwork;
import dev.gogetaparry.network.ParryFxPacket;
import dev.gogetaparry.network.ParryStatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Die komplette Spiellogik: Aufladen -> Parry -> Energiewelle -> Konter. */
@Mod.EventBusSubscriber(modid = GogetaParry.MODID)
public final class ParryEvents {
    private static final Map<UUID, ParryData> DATA = new HashMap<>();

    /** Ticks zwischen Parry und Konterschlag (kurze dramatische Pause). */
    private static final int COUNTER_DELAY_TICKS = 10;
    /** Ticks Unverwundbarkeit gegen Angriffe nach dem Parry. */
    private static final int GRACE_TICKS = 16;
    /** Anzahl Schritte der sich ausbreitenden Welle. */
    private static final int WAVE_STEPS = 4;

    private ParryEvents() {
    }

    private static ParryData get(Player player) {
        return DATA.computeIfAbsent(player.getUUID(), id -> new ParryData());
    }

    private static boolean isActive(ParryData data) {
        return !ParryConfig.REQUIRE_AMULET.get() || data.hasAmulet;
    }

    private static final boolean CURIOS = ModList.get().isLoaded("curios");

    /**
     * Ist das Amulett aktiv?
     *  - mit Curios: nur wenn es im Halsketten-Platz steckt (Inventar -> links neben dem Spieler)
     *  - ohne Curios: irgendwo im Inventar
     */
    private static boolean hasAmulet(Player player) {
        if (CURIOS) {
            return CuriosCompat.hasAmulet(player);
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.FUSION_AMULET.get())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 1) Parry: faengt den Angriff ab, BEVOR Schaden entsteht
    // ------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == player) return; // nur Angriffe von Mobs/Spielern
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // z. B. /kill

        ParryData data = get(player);

        // Direkt nach einem Parry: alles abblocken (z. B. Ghast-Explosion, mehrere Zombies)
        if (data.grace > 0) {
            event.setCanceled(true);
            return;
        }

        if (!isActive(data)) return;
        if (data.cooldown > 0) return;
        if (data.charge < ParryConfig.HITS_TO_CHARGE.get() - 0.001f) return;
        if (player.invulnerableTime > 10) return; // Treffer wuerde eh ignoriert werden

        event.setCanceled(true);
        performParry(player, data, attacker, isRanged(source, player, attacker));
    }

    /** Kam der Angriff aus der Ferne? (Pfeil, Feuerball, Dreizack ... oder Angreifer weit weg) */
    private static boolean isRanged(DamageSource source, ServerPlayer player, Entity attacker) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return true;
        if (source.getDirectEntity() instanceof Projectile) return true;
        return attacker.distanceTo(player) > 6.0f;
    }

    private static void performParry(ServerPlayer player, ParryData data, Entity attacker, boolean ranged) {
        ServerLevel level = player.serverLevel();

        data.charge = 0;
        data.grace = GRACE_TICKS;
        data.counterDelay = COUNTER_DELAY_TICKS;
        data.counterTarget = attacker instanceof LivingEntity ? attacker.getUUID() : null;
        data.counterRanged = ranged;
        data.waveStep = 0;
        data.waveCenter = player.position().add(0, player.getBbHeight() * 0.45, 0);
        data.flashPending = true;
        data.dirty = true;

        // Wie Gogeta: blitzschnell zum Angreifer drehen
        if (ParryConfig.FACE_ATTACKER.get()) {
            float[] rot = lookAt(player.getEyePosition(), attacker.getEyePosition());
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), rot[0], rot[1]);
            player.setYHeadRot(rot[0]);
            player.setYBodyRot(rot[0]);
        }

        // Hand vor + blaue Ki-Welle: alle Spieler in der Naehe bekommen das Signal
        ModNetwork.sendToTrackingAndSelf(player, new ParryFxPacket(player.getId()));
        ParryEffects.parry(player);

        // Blaue Energiewelle: stoesst alle Monster in der Naehe weg
        double radius = ParryConfig.SHOCKWAVE_RADIUS.get();
        float waveDamage = ParryConfig.SHOCKWAVE_DAMAGE.get().floatValue();
        if (radius > 0) {
            AABB box = player.getBoundingBox().inflate(radius, 1.5, radius);
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && e instanceof Enemy)) {
                if (mob.distanceTo(player) > radius + 0.5) continue;
                if (waveDamage > 0) {
                    mob.hurt(player.damageSources().playerAttack(player), waveDamage);
                }
                mob.knockback(1.2, player.getX() - mob.getX(), player.getZ() - mob.getZ());
            }
        }
    }

    // ------------------------------------------------------------------
    // 2) Aufladen: jeder echte Treffer fuellt den Kreis
    // ------------------------------------------------------------------
    // LivingHurtEvent kommt VOR Rüstung und gelben Extra-Herzen (goldener Apfel) -
    // so zaehlt jeder Treffer, auch wenn er nur die Absorption-Herzen trifft.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled() || event.getAmount() <= 0) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == player) return;

        ParryData data = get(player);
        if (!isActive(data) || data.cooldown > 0 || data.grace > 0) return;

        int max = ParryConfig.HITS_TO_CHARGE.get();
        if (data.charge >= max - 0.001f) return;

        // Treffer von Monstern/Spielern = 1, Umgebung (Fallen, Explosion, Feuer ...) = ein bisschen
        boolean environment = attacker == null || event.getSource().is(DamageTypeTags.IS_EXPLOSION);
        long now = player.level().getGameTime();
        float points = 1f;
        if (environment) {
            points = ParryConfig.ENV_CHARGE.get().floatValue();
            if (points <= 0f || now - data.lastEnvChargeTick < 20) return;
            data.lastEnvChargeTick = now;
        }

        data.charge = Math.min(max, data.charge + points);
        data.lastHitTick = now;
        data.dirty = true;

        ParryEffects.chargeStep(player, data.charge, max);
        if (data.charge >= max - 0.001f) {
            data.charge = max;
            ParryEffects.fullyCharged(player);
            data.fullHealPending = true; // erst nach dem Schaden heilen, sonst verpufft es bei vollen Herzen
        }
    }

    // ------------------------------------------------------------------
    // Power-Up nach dem Konter: mehr Schaden
    // ------------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerAttacks(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (event.getEntity() == attacker) return;
        ParryData data = DATA.get(attacker.getUUID());
        if (data == null || data.buffTicks <= 0) return;
        float bonus = 1f + ParryConfig.BUFF_PERCENT.get().floatValue() / 100f;
        event.setAmount(event.getAmount() * bonus);
    }

    private static final UUID SPEED_ID = UUID.fromString("6b1f3c2e-9a4d-4c7e-8f21-0d5e7a9b3c11");

    /** Power-Up-Tempo an/aus (nur voruebergehend, wird nicht gespeichert). */
    private static void setSpeedBoost(ServerPlayer player, boolean on) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(SPEED_ID);
        double pct = ParryConfig.BUFF_SPEED.get();
        if (on && pct > 0) {
            speed.addTransientModifier(new AttributeModifier(SPEED_ID, "Gogeta Power-Up",
                    pct / 100.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void startPowerUp(ServerPlayer player, ParryData data) {
        int ticks = ParryConfig.BUFF_SECONDS.get() * 20;
        if (ticks <= 0) return;
        data.buffTicks = ticks;
        data.dirty = true;
        setSpeedBoost(player, true);
        ParryEffects.powerUp(player);
    }

    // ------------------------------------------------------------------
    // 3) Pro Tick: Welle animieren, Konter ausfuehren, Timer, Sync
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ParryData data = get(player);
        ServerLevel level = player.serverLevel();

        // Amulett-Check (2x pro Sekunde reicht)
        if (player.tickCount % 10 == 0) {
            boolean has = hasAmulet(player);
            if (has != data.hasAmulet) {
                data.hasAmulet = has; // Ladung bleibt erhalten, auch wenn das Amulett kurz weg ist
                data.dirty = true;
            }
        }

        if (data.fullHealPending) {
            data.fullHealPending = false;
            healHp(player, data, ParryConfig.HEAL_ON_FULL.get().floatValue() * 2f); // Herzen -> Lebenspunkte
        }

        if (data.grace > 0) data.grace--;

        if (data.buffTicks > 0) {
            data.buffTicks--;
            if (data.buffTicks == 0) setSpeedBoost(player, false);
            if (data.buffTicks % 4 == 0) ParryEffects.aura(player);
            if (data.buffTicks == 0 || data.buffTicks % 20 == 0) data.dirty = true;
        }

        if (data.cooldown > 0) {
            data.cooldown--;
            if (data.cooldown == 0) data.dirty = true;
        }

        // Blaue Welle breitet sich ueber ein paar Ticks aus
        if (data.waveStep >= 0 && data.waveCenter != null) {
            ParryEffects.waveStep(level, data.waveCenter, data.waveStep);
            data.waveStep++;
            if (data.waveStep >= WAVE_STEPS) data.waveStep = -1;
        }

        // Konter
        if (data.counterDelay >= 0) {
            if (data.counterDelay == 0) {
                data.counterDelay = -1;
                performCounter(player, data);
            } else {
                data.counterDelay--;
            }
        }

        // Konter-Kick (Hocke -> Sprint -> Drehung -> Kick)
        if (data.rushTick >= 0) {
            tickKick(player, data);
        }

        // Ki-Blast (Aufladen -> Abfeuern -> Flug -> Einschlag)
        if (data.blastTick >= 0) {
            tickBlast(player, data);
        }

        if (data.dirty) {
            sync(player, data);
        }
    }

    private static void performCounter(ServerPlayer player, ParryData data) {
        ServerLevel level = player.serverLevel();

        int cd = ParryConfig.COOLDOWN_SECONDS.get() * 20;
        data.cooldown = cd;
        data.cooldownMax = cd;
        data.dirty = true;

        Entity entity = data.counterTarget == null ? null : level.getEntity(data.counterTarget);
        UUID targetId = data.counterTarget;
        data.counterTarget = null;

        // Fernkampf-Angriff -> Ki-Blast zurueck zum Schuetzen
        if (data.counterRanged && entity instanceof LivingEntity shooter && shooter.isAlive()
                && player.distanceTo(shooter) <= ParryConfig.COUNTER_RANGE.get() * 2) {
            startBlast(player, data, shooter);
            return;
        }

        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || player.distanceTo(target) > ParryConfig.COUNTER_RANGE.get()) {
            startPowerUp(player, data); // kein Kick moeglich -> Power-Up gibt es trotzdem
            return;
        }

        // Konter-Kick starten (wie Gogeta in Legends)
        data.rushTarget = targetId;
        data.rushTick = 0;
        data.kickDest = null;
        data.grace = Math.max(data.grace, KICK_END + 6);
        ModNetwork.sendToTrackingAndSelf(player,
                new ParryFxPacket(player.getId(), ParryFxPacket.KICK, KICK_YAW_OFFSET));
        ParryEffects.kickCharge(player);
    }

    // Ablauf in Ticks ab Kick-Start (1 Tick = 50 ms), passend zu ClientParryFx.KICK_*
    private static final int KICK_DASH = 3;    // 150 ms: Absprung
    private static final int KICK_ARRIVE = 7;  // 350 ms: beim Gegner angekommen, Drehung
    private static final int KICK_HIT = 11;    // 550 ms: Kick trifft
    private static final int KICK_END = 16;    // 800 ms: fertig
    /** Der Spieler schaut beim Kick etwas neben den Gegner, damit die Kamera von vorne beide zeigt. */
    private static final float KICK_YAW_OFFSET = 35f;

    private static void tickKick(ServerPlayer player, ParryData data) {
        ServerLevel level = player.serverLevel();
        Entity entity = data.rushTarget == null ? null : level.getEntity(data.rushTarget);
        boolean targetOk = entity instanceof LivingEntity t && t.isAlive()
                && player.distanceTo(t) <= ParryConfig.COUNTER_RANGE.get() + 6;
        int tick = data.rushTick;

        if (!targetOk && tick <= KICK_HIT) {
            data.rushTick = -1;
            data.rushTarget = null;
            return;
        }

        if (tick == KICK_DASH && entity instanceof LivingEntity target) {
            // Ziel: knapp vor dem Gegner, leicht in der Luft
            Vec3 away = new Vec3(player.getX() - target.getX(), 0, player.getZ() - target.getZ());
            away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle().scale(-1).multiply(1, 0, 1) : away.normalize();
            double gap = target.getBbWidth() / 2.0 + 1.0;
            Vec3 dest = target.position().add(away.scale(gap)).add(0, 0.5, 0);
            if (!ParryConfig.TELEPORT_COUNTER.get() || !fits(player, dest)) {
                dest = fits(player, dest.add(0, -0.5, 0)) && ParryConfig.TELEPORT_COUNTER.get()
                        ? dest.add(0, -0.5, 0) : player.position();
            }
            data.kickDest = dest;
            float[] rot = lookAt(dest.add(0, player.getEyeHeight(), 0), target.getEyePosition());
            data.kickYaw = rot[0] + KICK_YAW_OFFSET;
            data.kickPitch = Mth.clamp(rot[1], -30f, 30f);

            // Absprung: Geschwindigkeit so, dass er in ~4 Ticks ankommt (Luftreibung 0.91 pro Tick)
            Vec3 d = dest.subtract(player.position());
            player.setDeltaMovement(d.x / 3.49, 0.35 + d.y / 4.0, d.z / 3.49);
            player.hurtMarked = true;
            ParryEffects.dash(player);
        } else if (tick > KICK_DASH && tick < KICK_ARRIVE) {
            ParryEffects.dashTrail(level, player.position());
        } else if (tick >= KICK_ARRIVE && tick <= KICK_HIT && data.kickDest != null) {
            // in der Luft halten (gleiche Stelle -> kein sichtbares Ruckeln), drehen, kicken
            player.connection.teleport(data.kickDest.x, data.kickDest.y, data.kickDest.z, data.kickYaw, data.kickPitch);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            if (tick == KICK_ARRIVE) ParryEffects.spin(player);
        }

        if (tick == KICK_HIT && entity instanceof LivingEntity target) {
            player.swing(InteractionHand.MAIN_HAND, true);
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), ParryConfig.COUNTER_DAMAGE.get().floatValue());

            ParryEffects.finisher(player, target);
            // Impact-Moment beim Spieler: dunkler Bildschirm mit weissen Streifen, dann oranger Knall
            data.impactPending = true;
            data.dirty = true;
            heal(player, data, ParryConfig.HEAL_ON_COUNTER.get());
            startPowerUp(player, data);
            Vec3 dir = new Vec3(target.getX() - player.getX(), 0, target.getZ() - player.getZ());
            dir = dir.lengthSqr() < 1.0E-4 ? player.getLookAngle() : dir.normalize();
            target.setDeltaMovement(dir.x * 1.3, 0.7, dir.z * 1.3); // wegschleudern
            target.hurtMarked = true;
        }

        player.fallDistance = 0;
        data.rushTick++;
        if (data.rushTick > KICK_END) {
            data.rushTick = -1;
            data.rushTarget = null;
            data.kickDest = null;
        }
    }

    /** Heilt den Spieler um einen Prozentsatz seiner maximalen Leben (wie Gogeta in Legends). */
    private static void heal(ServerPlayer player, ParryData data, double percent) {
        healHp(player, data, (float) (player.getMaxHealth() * percent / 100.0));
    }

    /** Heilt den Spieler um eine feste Anzahl Lebenspunkte (2 = 1 Herz). */
    private static void healHp(ServerPlayer player, ParryData data, float amount) {
        if (amount <= 0 || player.getHealth() >= player.getMaxHealth()) return;
        float before = player.getHealth();
        player.heal(amount);
        float healed = player.getHealth() - before;
        if (healed <= 0) return;
        data.healPending += healed;
        data.dirty = true;
        ParryEffects.heal(player);
    }

    // ------------------------------------------------------------------
    // Ki-Blast gegen Fernkampf-Gegner
    // ------------------------------------------------------------------
    private static final int BLAST_FIRE = 6;          // 300 ms aufladen, dann abfeuern
    private static final float BLAST_SPEED = 1.5f;    // Bloecke pro Tick
    private static final float BLAST_YAW_OFFSET = 22f; // Blick etwas neben das Ziel -> Kamera von hinten sieht beides

    private static void startBlast(ServerPlayer player, ParryData data, LivingEntity shooter) {
        data.blastTick = 0;
        data.blastTarget = shooter.getUUID();
        data.blastFrom = null;
        data.blastFlight = 0;
        float[] rot = lookAt(player.getEyePosition(), shooter.getEyePosition());
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), rot[0] + BLAST_YAW_OFFSET, rot[1]);
        data.grace = Math.max(data.grace, BLAST_FIRE + 30);
        ModNetwork.sendToTrackingAndSelf(player,
                new ParryFxPacket(player.getId(), ParryFxPacket.BLAST, BLAST_YAW_OFFSET, shooter.getId(), 0));
        ParryEffects.blastCharge(player);
    }

    private static void tickBlast(ServerPlayer player, ParryData data) {
        ServerLevel level = player.serverLevel();
        Entity entity = data.blastTarget == null ? null : level.getEntity(data.blastTarget);
        int tick = data.blastTick;
        LivingEntity target = (entity instanceof LivingEntity living && living.isAlive()) ? living : null;
        if (target == null && data.blastFrom == null) { // Ziel weg, bevor geschossen wurde -> abbrechen
            data.blastTick = -1;
            startPowerUp(player, data);
            return;
        }

        Vec3 hands = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.35, 0);
        if (tick < BLAST_FIRE) {
            ParryEffects.blastGather(level, hands);
        } else if (tick == BLAST_FIRE && target != null) {
            data.blastFrom = hands;
            double dist = hands.distanceTo(target.getEyePosition());
            data.blastFlight = Mth.clamp((int) Math.ceil(dist / BLAST_SPEED), 2, 20);
            ModNetwork.sendToTrackingAndSelf(player,
                    new ParryFxPacket(player.getId(), ParryFxPacket.BLAST_FIRE, BLAST_YAW_OFFSET, target.getId(), data.blastFlight));
            player.swing(InteractionHand.MAIN_HAND, true);
            ParryEffects.blastFire(player);
        } else if (data.blastFrom != null) {
            int flown = tick - BLAST_FIRE;
            Vec3 aim = target != null ? target.position().add(0, target.getBbHeight() * 0.6, 0) : data.blastFrom;
            if (flown < data.blastFlight) {
                double t = flown / (double) data.blastFlight;
                ParryEffects.blastTrail(level, data.blastFrom.lerp(aim, t));
            } else if (flown == data.blastFlight && target != null) {
                // Einschlag!
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().playerAttack(player), ParryConfig.COUNTER_DAMAGE.get().floatValue());
                ParryEffects.blastHit(player, target);
                ParryEffects.finisher(player, target);
                data.impactPending = true;
                data.dirty = true;
                Vec3 dir = new Vec3(target.getX() - player.getX(), 0, target.getZ() - player.getZ());
                dir = dir.lengthSqr() < 1.0E-4 ? player.getLookAngle() : dir.normalize();
                target.setDeltaMovement(dir.x * 1.1, 0.6, dir.z * 1.1);
                target.hurtMarked = true;
                heal(player, data, ParryConfig.HEAL_ON_COUNTER.get());
                startPowerUp(player, data);
            }
        }

        data.blastTick++;
        if (data.blastFrom != null && data.blastTick > BLAST_FIRE + data.blastFlight + 10) {
            data.blastTick = -1;
            data.blastTarget = null;
            data.blastFrom = null;
        }
    }

    /** Passt der Spieler an diese Stelle, ohne in Bloecken zu stecken? */
    private static boolean fits(ServerPlayer player, Vec3 pos) {
        AABB box = player.getBoundingBox().move(pos.subtract(player.position()));
        return player.serverLevel().noCollision(player, box);
    }

    /** Blickrichtung (yaw, pitch) von 'from' nach 'to'. */
    private static float[] lookAt(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)));
        return new float[]{yaw, pitch};
    }

    private static void sync(ServerPlayer player, ParryData data) {
        ModNetwork.sendTo(player, new ParryStatePacket(
                data.charge,
                ParryConfig.HITS_TO_CHARGE.get(),
                data.cooldown,
                data.cooldownMax,
                isActive(data),
                data.flashPending,
                data.impactPending,
                data.healPending,
                data.buffTicks));
        data.flashPending = false;
        data.impactPending = false;
        data.healPending = 0f;
        data.dirty = false;
    }

    // ------------------------------------------------------------------
    // Login / Logout / Respawn
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ParryData data = get(event.getEntity());
        data.hasAmulet = hasAmulet(event.getEntity());
        data.dirty = true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DATA.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ParryData fresh = new ParryData();
        fresh.hasAmulet = hasAmulet(event.getEntity());
        DATA.put(event.getEntity().getUUID(), fresh);
    }
}
