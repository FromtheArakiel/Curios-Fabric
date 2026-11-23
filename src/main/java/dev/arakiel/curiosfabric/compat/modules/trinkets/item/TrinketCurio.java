package dev.arakiel.curiosfabric.compat.modules.trinkets.item;

import com.google.common.collect.Multimap;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.modules.trinkets.TrinketsCompat;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ICurioItem} bridge used for items that were written against Trinkets.
 *
 * <p>This adapter is for items qualified only by Trinkets slot tags. Code-backed items use
 * {@link TrinketItemAdapter}. Tag-only items retain Trinkets' original behavior: they can be put in
 * a slot, but do not automatically equip from use. Global lifecycle and drop callbacks still run.</p>
 */
public enum TrinketCurio implements ICurioItem {
  INSTANCE;

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    return mapsTo(slotContext.identifier(), stack, side(slotContext));
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    return false;
  }

  @Override
  public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    TrinketLifecycleCallbacks.onEquip(slotContext, stack);
  }

  @Override
  public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    TrinketLifecycleCallbacks.onUnequip(slotContext, stack);
  }

  @Override
  public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                     int lootingLevel, boolean recentlyHit, ItemStack stack) {
    return TrinketLifecycleCallbacks.applyDropCallback(
        slotContext, stack, ICurio.DropRule.DEFAULT);
  }

  @Override
  public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    return TrinketItemAdapter.componentModifiers(stack, side(slotContext));
  }

  /** {@code true} when the stack is registered for the given Curios slot through Trinkets' tags. */
  public static boolean mapsTo(String curiosSlot, ItemStack stack) {
    return mapsTo(curiosSlot, stack, false) || mapsTo(curiosSlot, stack, true);
  }

  private static boolean mapsTo(String curiosSlot, ItemStack stack, boolean client) {
    String path = TrinketsCompat.trinketsPathOf(curiosSlot, client);
    return path != null && TrinketsCompat.isTrinketFor(stack, path);
  }

  private static boolean side(SlotContext slotContext) {
    return slotContext.entity() != null && slotContext.entity().level() != null
        && slotContext.entity().level().isClientSide();
  }
}
