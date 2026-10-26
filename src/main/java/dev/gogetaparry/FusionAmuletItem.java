package dev.gogetaparry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class FusionAmuletItem extends Item {

    public FusionAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Verzauberungs-Glitzer
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gogetaparry.fusion_amulet.desc1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.gogetaparry.fusion_amulet.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gogetaparry.fusion_amulet.desc3").withStyle(ChatFormatting.GRAY));
    }
}
