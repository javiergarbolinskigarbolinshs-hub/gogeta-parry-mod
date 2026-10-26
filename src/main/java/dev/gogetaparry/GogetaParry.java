package dev.gogetaparry;

import dev.gogetaparry.network.ModNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Gogeta Parry – Hauptklasse der Mod.
 *
 * Ablauf im Spiel:
 *  1. Du wirst von Mobs getroffen (Schlaege, Pfeile, Feuerbaelle ...) -> der Kreis laedt sich auf.
 *  2. Nach 5 Treffern ist der Kreis voll.
 *  3. Der naechste Angriff wird automatisch geparryt: Hand-Animation, blaue Energiewelle, 0 Schaden.
 *  4. Kurz danach konterst du: Teleport hinter den Angreifer + harter Schlag.
 */
@Mod(GogetaParry.MODID)
public class GogetaParry {
    public static final String MODID = "gogetaparry";

    public GogetaParry() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        modBus.addListener(this::addToCreativeTab);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ParryConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModNetwork.register();
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.FUSION_AMULET.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.KI_CRYSTAL.get());
        }
    }
}
