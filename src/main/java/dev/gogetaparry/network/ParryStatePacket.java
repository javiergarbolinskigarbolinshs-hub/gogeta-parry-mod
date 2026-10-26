package dev.gogetaparry.network;

import dev.gogetaparry.ClientParryState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: aktueller Stand des Ladekreises (+ Signale fuer Blitz und Impact). */
public class ParryStatePacket {
    public final float charge;
    public final int maxCharge;
    public final int cooldownTicks;
    public final int cooldownMax;
    public final boolean active;
    public final boolean flash;
    public final boolean impact;
    public final float heal;
    public final int buffTicks;

    public ParryStatePacket(float charge, int maxCharge, int cooldownTicks, int cooldownMax,
                            boolean active, boolean flash, boolean impact, float heal, int buffTicks) {
        this.charge = charge;
        this.maxCharge = maxCharge;
        this.cooldownTicks = cooldownTicks;
        this.cooldownMax = cooldownMax;
        this.active = active;
        this.flash = flash;
        this.impact = impact;
        this.heal = heal;
        this.buffTicks = buffTicks;
    }

    public ParryStatePacket(FriendlyByteBuf buf) {
        this.charge = buf.readFloat();
        this.maxCharge = buf.readVarInt();
        this.cooldownTicks = buf.readVarInt();
        this.cooldownMax = buf.readVarInt();
        this.active = buf.readBoolean();
        this.flash = buf.readBoolean();
        this.impact = buf.readBoolean();
        this.heal = buf.readFloat();
        this.buffTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(charge);
        buf.writeVarInt(maxCharge);
        buf.writeVarInt(cooldownTicks);
        buf.writeVarInt(cooldownMax);
        buf.writeBoolean(active);
        buf.writeBoolean(flash);
        buf.writeBoolean(impact);
        buf.writeFloat(heal);
        buf.writeVarInt(buffTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // laeuft dank consumerMainThread schon auf dem Client-Hauptthread
        ClientParryState.apply(this);
    }
}
