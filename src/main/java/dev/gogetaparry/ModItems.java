package dev.gogetaparry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GogetaParry.MODID);

    /** Solange dieses Amulett irgendwo im Inventar ist, ist die Parry-Faehigkeit aktiv. */
    public static final RegistryObject<Item> FUSION_AMULET = ITEMS.register("fusion_amulet",
            () -> new FusionAmuletItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** Seltener Ki-Kristall: nur in Truhen (Dungeons, Netherfestungen, Bastionen) - daraus craftet man das Amulett. */
    public static final RegistryObject<Item> KI_CRYSTAL = ITEMS.register("ki_crystal",
            () -> new KiCrystalItem(new Item.Properties().rarity(Rarity.RARE)));

    private ModItems() {
    }
}
