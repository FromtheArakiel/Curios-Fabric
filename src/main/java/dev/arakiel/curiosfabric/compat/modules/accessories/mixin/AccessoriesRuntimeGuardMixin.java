package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import io.wispforest.accessories.impl.AccessoriesEventHandler;
import java.util.Collection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents Accessories' own inventory runtime from competing with CuriosFabric. */
@Mixin(AccessoriesEventHandler.class)
public abstract class AccessoriesRuntimeGuardMixin {

  @Inject(method = "onLivingEntityTick", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipAccessoriesTick(
      LivingEntity entity, CallbackInfo callback) {
    callback.cancel();
  }

  @Inject(method = "getTooltipData", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipAccessoriesTooltip(
      LivingEntity entity,
      ItemStack stack,
      List<Component> tooltip,
      Item.TooltipContext context,
      TooltipFlag flag,
      CallbackInfo callback) {
    callback.cancel();
  }

  @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipAccessoriesDeath(
      LivingEntity entity,
      DamageSource source,
      CallbackInfoReturnable<Collection<ItemStack>> callback) {
    callback.setReturnValue(List.of());
  }

  @Inject(method = "attemptEquipFromUse", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipAccessoriesUse(
      Player player,
      InteractionHand hand,
      CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
    callback.setReturnValue(InteractionResultHolder.pass(player.getItemInHand(hand)));
  }

  @Inject(method = "attemptEquipOnEntity", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipAccessoriesEntityUse(
      Player player,
      InteractionHand hand,
      Entity entity,
      CallbackInfoReturnable<InteractionResult> callback) {
    callback.setReturnValue(InteractionResult.PASS);
  }
}
