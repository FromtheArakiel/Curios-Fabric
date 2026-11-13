package dev.arakiel.curiosfabric.mixin.core;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dev.arakiel.curiosfabric.mixin.CuriosUtilMixinHooks;

@Mixin(EnchantedCountIncreaseFunction.class)
public class MixinEnchantedCountIncreaseFunction {

  @Shadow
  @Final
  private Holder<Enchantment> enchantment;

  @ModifyVariable(
      method = "run",
      at = @At(
          value = "INVOKE_ASSIGN",
          target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I",
          ordinal = 0),
      index = 5
  )
  private int curios$applyEnchantBonus(int enchantmentLevel, ItemStack stack,
                                       LootContext lootContext) {

    if (enchantment.is(Enchantments.LOOTING)) {
      return enchantmentLevel + CuriosUtilMixinHooks.getLootingLevel(lootContext);
    }
    return enchantmentLevel;
  }
}
