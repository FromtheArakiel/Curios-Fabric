package dev.arakiel.curiosfabric.compat.modules.accessories.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.SlotAttribute;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.modules.accessories.AccessoriesCompat;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SoundEventData;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Executes Accessories item hooks while the stack lives in a CuriosFabric slot. */
public final class AccessoriesCurio implements ICurioItem {

  @Override
  public boolean hasCurioCapability(ItemStack stack) {
    return AccessoriesCompat.isAccessoriesItem(stack, side(stack));
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return AccessoriesAPI.getOrDefaultAccessory(stack).maxStackSize(stack);
  }

  @Override
  public void curioTick(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference != null) {
      AccessoriesAPI.getOrDefaultAccessory(stack).tick(stack, reference);
    }
  }

  @Override
  public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference != null) {
      AccessoriesAPI.getOrDefaultAccessory(stack).onEquip(stack, reference);
    }
  }

  @Override
  public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference != null) {
      AccessoriesAPI.getOrDefaultAccessory(stack).onUnequip(stack, reference);
    }
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    return reference != null && AccessoriesAPI.canEquip(stack, reference);
  }

  @Override
  public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    return reference != null && AccessoriesAPI.canUnequip(stack, reference);
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    return reference != null
        && AccessoriesCompat.allowsEquipFromUse(reference)
        && AccessoriesAPI.getOrDefaultAccessory(stack).canEquipFromUse(stack);
  }

  @Override
  public void onEquipFromUse(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference != null) {
      AccessoriesAPI.getOrDefaultAccessory(stack).onEquipFromUse(stack, reference);
    }
  }

  @Override
  public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference == null) {
      return ICurioItem.super.getEquipSound(slotContext, stack);
    }
    SoundEventData sound =
        AccessoriesAPI.getOrDefaultAccessory(stack).getEquipSound(stack, reference);
    return sound == null
        ? ICurioItem.super.getEquipSound(slotContext, stack)
        : new ICurio.SoundInfo(
            sound.event().value(), sound.volume(), sound.pitch());
  }

  @Override
  public void curioBreak(SlotContext slotContext, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference != null) {
      AccessoriesAPI.getOrDefaultAccessory(stack).onBreak(stack, reference);
    }
  }

  @Override
  public ICurio.DropRule getDropRule(
      SlotContext slotContext,
      DamageSource source,
      int lootingLevel,
      boolean recentlyHit,
      ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    if (reference == null) {
      return ICurio.DropRule.DEFAULT;
    }
    DropRule rule =
        AccessoriesAPI.getOrDefaultAccessory(stack).getDropRule(stack, reference, source);
    rule = OnDropCallback.getAlternativeRule(rule, stack, reference, source);
    return AccessoriesCompat.mapDropRule(rule);
  }

  @Override
  public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    SlotReference reference = reference(slotContext, stack);
    Multimap<Holder<Attribute>, AttributeModifier> result = LinkedHashMultimap.create();
    if (reference == null) {
      return result;
    }
    AccessoryAttributeBuilder builder =
        AccessoriesAPI.getAttributeModifiers(stack, reference);
    result.putAll(builder.getAttributeModifiers(true));
    boolean client = side(slotContext, stack);
    builder.getSlotModifiers().forEach((targetSlot, modifier) -> {
      String curiosSlot = AccessoriesCompat.curiosIdFor(targetSlot, client);
      if (curiosSlot != null) {
        result.put(SlotAttribute.getOrCreate(curiosSlot), modifier);
      }
    });
    return result;
  }

  @Override
  public List<Component> getAttributesTooltip(
      List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
    SlotReference reference = referenceFromTooltip(stack);
    if (reference == null) {
      return tooltips;
    }
    SlotType type = reference.type();
    Accessory accessory = AccessoriesAPI.getOrDefaultAccessory(stack);
    if (type != null) {
      accessory.getAttributesTooltip(
          stack, type, tooltips, context, TooltipFlag.Default.NORMAL);
    }
    accessory.getExtraTooltip(
        stack, tooltips, context, TooltipFlag.Default.NORMAL);
    return tooltips;
  }

  @Nullable
  private static SlotReference reference(SlotContext context, ItemStack stack) {
    return AccessoriesCompat.referenceOrFallback(context, stack);
  }

  @Nullable
  private static SlotReference referenceFromTooltip(ItemStack stack) {
    String slotId = AccessoriesCompat.slotsFor(stack, null, CuriosFabricConstants.IS_CLIENT)
        .stream().findFirst().orElse(null);
    if (slotId == null) {
      return null;
    }
    SlotContext context =
        new SlotContext(slotId, null, 0, false, true);
    return AccessoriesCompat.referenceOrFallback(context, stack);
  }

  private static boolean side(ItemStack stack) {
    return CuriosFabricConstants.IS_CLIENT;
  }

  private static boolean side(SlotContext context, ItemStack stack) {
    return context.entity() != null && context.entity().level() != null
        ? context.entity().level().isClientSide() : side(stack);
  }
}
