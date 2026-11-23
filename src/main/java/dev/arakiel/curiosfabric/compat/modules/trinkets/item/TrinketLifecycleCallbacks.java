package dev.arakiel.curiosfabric.compat.modules.trinkets.item;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.compat.modules.trinkets.api.TrinketsApiFacade;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import dev.emi.trinkets.api.event.TrinketEquipCallback;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Re-emits the Trinkets lifecycle events that Curios stores replace.
 *
 * <p>Trinkets fires these events after the item's own equip hook for every equipped stack, including
 * items that are only qualified through slot tags. Mods such as Artifacts subscribe to these events
 * instead of overriding {@code Trinket}, so invoking only the item method leaves stale abilities
 * behind after an item is removed.</p>
 */
final class TrinketLifecycleCallbacks {

  private TrinketLifecycleCallbacks() {
  }

  static void onEquip(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference == null) {
      return;
    }
    try {
      TrinketEquipCallback.EVENT.invoker()
          .onEquip(stack, reference, slotContext.entity());
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug(
          "Bridged Trinkets equip callback failed for {}", stack.getItem(), throwable);
    }
  }

  static void onUnequip(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference == null) {
      return;
    }
    try {
      TrinketUnequipCallback.EVENT.invoker()
          .onUnequip(stack, reference, slotContext.entity());
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug(
          "Bridged Trinkets unequip callback failed for {}", stack.getItem(), throwable);
    }
  }

  static ICurio.DropRule applyDropCallback(
      SlotContext slotContext, ItemStack stack, ICurio.DropRule rule) {
    SlotReference reference = reference(slotContext, stack);
    if (reference == null) {
      return rule;
    }
    try {
      TrinketEnums.DropRule result = TrinketDropCallback.EVENT.invoker()
          .drop(toTrinketsDropRule(rule), stack, reference, slotContext.entity());
      return fromTrinketsDropRule(result);
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug(
          "Bridged Trinkets drop callback failed for {}", stack.getItem(), throwable);
      return rule;
    }
  }

  static ICurio.DropRule mapDropRule(Object raw) {
    return raw instanceof Enum<?> value
        ? TrinketDropRules.toCurios(value.name()) : ICurio.DropRule.DEFAULT;
  }

  private static TrinketEnums.DropRule toTrinketsDropRule(ICurio.DropRule rule) {
    return switch (rule) {
      case ALWAYS_KEEP -> TrinketEnums.DropRule.KEEP;
      case ALWAYS_DROP -> TrinketEnums.DropRule.DROP;
      case DESTROY -> TrinketEnums.DropRule.DESTROY;
      case DEFAULT -> TrinketEnums.DropRule.DEFAULT;
    };
  }

  private static ICurio.DropRule fromTrinketsDropRule(TrinketEnums.DropRule rule) {
    return switch (rule) {
      case KEEP -> ICurio.DropRule.ALWAYS_KEEP;
      case DROP -> ICurio.DropRule.ALWAYS_DROP;
      case DESTROY -> ICurio.DropRule.DESTROY;
      case DEFAULT -> ICurio.DropRule.DEFAULT;
    };
  }

  private static SlotReference reference(SlotContext slotContext, ItemStack stack) {
    LivingEntity entity = slotContext.entity();
    if (entity == null) {
      return null;
    }
    Object raw = TrinketsApiFacade.slotReference(
        slotContext.identifier(), slotContext.index(), entity, stack);
    return raw instanceof SlotReference reference ? reference : null;
  }
}
