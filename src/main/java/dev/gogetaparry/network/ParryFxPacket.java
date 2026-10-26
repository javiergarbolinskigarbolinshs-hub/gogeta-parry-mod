package dev.gogetaparry.network;

import dev.gogetaparry.ClientParryFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> alle Clients in der Naehe:
 *  PARRY = "Spieler X parryt gerade" (Hand-Pose + Ki-Welle)
 *  KICK  = "Spieler X startet den Konter-Kick" (Hocke, Sprint, Drehung, Kick)
 */
public class ParryFxPacket {
    public static final byte PARRY = 0;
    public static final byte KICK = 1;
    public static final byte BLAST = 2;       // Ki-Blast laedt auf (targetId = Ziel)
    public static final byte BLAST_FIRE = 3;  // Ki-Blast fliegt los (value = Flugzeit in Ticks)

    public final int entityId;
    public final byte type;
    public final float yawOffset;
    public final int targetId;
    public final int value;

    public ParryFxPacket(int entityId) {
        this(entityId, PARRY, 0f);
    }

    public ParryFxPacket(int entityId, byte type, float yawOffset) {
        this(entityId, type, yawOffset, -1, 0);
    }

    public ParryFxPacket(int entityId, byte type, float yawOffset, int targetId, int value) {
        this.entityId = entityId;
        this.type = type;
        this.yawOffset = yawOffset;
        this.targetId = targetId;
        this.value = value;
    }

    public ParryFxPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.type = buf.readByte();
        this.yawOffset = buf.readFloat();
        this.targetId = buf.readVarInt();
        this.value = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByte(type);
        buf.writeFloat(yawOffset);
        buf.writeVarInt(targetId);
        buf.writeVarInt(value);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if (type == KICK) {
            ClientParryFx.startKick(entityId, yawOffset);
        } else if (type == BLAST) {
            ClientParryFx.startBlast(entityId, yawOffset, targetId);
        } else if (type == BLAST_FIRE) {
            ClientParryFx.fireBlast(entityId, targetId, value);
        } else {
            ClientParryFx.start(entityId);
        }
    }
}
