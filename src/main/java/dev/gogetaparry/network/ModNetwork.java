package dev.gogetaparry.network;

import dev.gogetaparry.GogetaParry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "6";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GogetaParry.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ParryStatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ParryStatePacket::encode)
                .decoder(ParryStatePacket::new)
                .consumerMainThread(ParryStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(ParryFxPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ParryFxPacket::encode)
                .decoder(ParryFxPacket::new)
                .consumerMainThread(ParryFxPacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /** An den Spieler selbst und alle, die ihn sehen koennen. */
    public static void sendToTrackingAndSelf(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }
}
