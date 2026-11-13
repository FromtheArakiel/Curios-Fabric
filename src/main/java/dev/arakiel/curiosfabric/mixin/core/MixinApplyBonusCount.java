package dev.arakiel.curiosfabric.mixin.core;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dev.arakiel.curiosfabric.mixin.CuriosUtilMixinHooks;

@Mixin(ApplyBonusCount.class)
public class MixinApplyBonusCount {

  @Shadow
  @Final
  private Holder<Enchantment> enchantment;

  @ModifyVariable(
      method = "run",
      at = @At(
          value = "INVOKE_ASSIGN",
          target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I",
          ordinal = 0),
      index = 4
  )
  private int curios$applyEnchantBonus(int enchantmentLevel, ItemStack stack,
                                       LootContext lootContext) {

    if (enchantment.is(Enchantments.FORTUNE)) {
      return enchantmentLevel + CuriosUtilMixinHooks.getFortuneLevel(lootContext);
    }
    return enchantmentLevel;
  }
}
