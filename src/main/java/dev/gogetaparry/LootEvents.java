package dev.gogetaparry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Legt den Ki-Kristall in Truhen:
 *  - Dungeons (Raeume mit Monster-Spawner) und Netherfestungen: Grund-Chance (Standard 15 %)
 *  - Bastionen: Schatzkammer doppelte Chance, andere Bastion-Truhen 2/3 der Chance
 */
@Mod.EventBusSubscriber(modid = GogetaParry.MODID)
public final class LootEvents {
    /** Truhe -> Faktor auf die Grund-Chance. */
    private static final Map<ResourceLocation, Float> CHESTS = Map.of(
            new ResourceLocation("minecraft", "chests/simple_dungeon"), 1.0f,
            new ResourceLocation("minecraft", "chests/nether_bridge"), 1.0f,
            new ResourceLocation("minecraft", "chests/bastion_treasure"), 2.0f,
            new ResourceLocation("minecraft", "chests/bastion_other"), 0.67f,
            new ResourceLocation("minecraft", "chests/bastion_bridge"), 0.67f,
            new ResourceLocation("minecraft", "chests/bastion_hoglin_stable"), 0.67f
    );

    private LootEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        Float factor = CHESTS.get(event.getName());
        if (factor == null) return;
        float chance = (float) (ParryConfig.CRYSTAL_CHANCE.get() / 100.0) * factor;
        if (chance <= 0f) return;
        chance = Math.min(1f, chance);

        LootPool pool = LootPool.lootPool()
                .name("gogetaparry_ki_crystal")
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModItems.KI_CRYSTAL.get()))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .build();
        event.getTable().addPool(pool);
    }
}
