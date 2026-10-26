package dev.gogetaparry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Ki-Kristall: seltener Truhen-Fund, Zutat fuer das Fusions-Amulett. */
public class KiCrystalItem extends Item {

    public KiCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gogetaparry.ki_crystal.desc1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.gogetaparry.ki_crystal.desc2").withStyle(ChatFormatting.GRAY));
    }
}
