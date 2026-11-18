package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.fabric.AccessoriesInternalsImpl;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

/** Blocks every Accessories server entry point that can create a second inventory UI. */
@Mixin({Accessories.class, AccessoriesInternalsImpl.class})
public abstract class AccessoriesMenuGuardMixin {

  @Inject(
      method = "openAccessoriesMenu(Lnet/minecraft/world/entity/player/Player;"
          + "Lio/wispforest/accessories/menu/AccessoriesMenuVariant;"
          + "Lnet/minecraft/world/entity/LivingEntity;"
          + "Lnet/minecraft/world/item/ItemStack;)V",
      at = @At("HEAD"),
      cancellable = true)
  private static void curiosfabric$blockAccessoriesMenu(
      Player player,
      AccessoriesMenuVariant variant,
      @Nullable LivingEntity targetEntity,
      @Nullable ItemStack carriedStack,
      CallbackInfo callback) {
    callback.cancel();
  }
}
