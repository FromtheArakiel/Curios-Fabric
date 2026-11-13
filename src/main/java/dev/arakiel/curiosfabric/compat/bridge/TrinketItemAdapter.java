package dev.arakiel.curiosfabric.compat.bridge;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotAttribute;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat;
import net.minecraft.sounds.SoundEvent;

/**
 * Runs a Trinkets item's code hooks through this mod.
 *
 * <p>Trinkets content (for example Potion Ring) puts its behavior in {@code Trinket#tick},
 * {@code onEquip} and {@code onUnequip}. Items tagged for a slot only carry their attributes, so
 * those hooks have to be invoked explicitly while the item lives in a Curios slot. They are called
 * through reflection; the {@code SlotReference} argument is passed as {@code null}, which is fine
 * for the common implementations that only look at the stack and the wearer, and any failure is
 * caught so a broken hook cannot break the tick loop.</p>
 */
public class TrinketItemAdapter implements ICurioItem {

  private static final String TRINKET = "dev.emi.trinkets.api.Trinket";
  private static final String TRINKET_ITEM = "dev.emi.trinkets.api.TrinketItem";

  private final Item item;

  public TrinketItemAdapter(Item item) {
    this.item = item;
  }

  /** {@code true} when the item is a Trinkets trinket. */
  public static boolean isTrinket(Item item) {
    Class<?> trinket = dev.arakiel.curiosfabric.compat.CompatTargets.loadClass(TRINKET);

    if (trinket != null && trinket.isInstance(item)) {
      return true;
    }
    Class<?> trinketItem = dev.arakiel.curiosfabric.compat.CompatTargets.loadClass(TRINKET_ITEM);
    return trinketItem != null && trinketItem.isInstance(item);
  }

  private Object call(String name, Object... args) {

    try {

      for (Method method : this.item.getClass().getMethods()) {

        if (method.getName().equals(name) && method.getParameterCount() == args.length) {
          return method.invoke(this.item, args);
        }
      }
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Bridged Trinkets hook {} failed for {}", name, this.item,
          throwable);
    }
    return null;
  }

  @Override
  public void curioTick(SlotContext slotContext, ItemStack stack) {
    call("tick", stack, reference(slotContext, stack), slotContext.entity());
  }

  @Override
  public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    call("onEquip", stack, reference(slotContext, stack), slotContext.entity());
  }

  @Override
  public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    call("onUnequip", stack, reference(slotContext, stack), slotContext.entity());
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canEquip", stack, reference(slotContext, stack), slotContext.entity());
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canUnequip", stack, reference(slotContext, stack), slotContext.entity());
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    Object result = call("canEquipFromUse", stack, slotContext.entity());
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
    Object result = call("getEquipSound", stack, reference(slotContext, stack),
        slotContext.entity());
    if (result instanceof Holder<?> holder && holder.value() instanceof SoundEvent sound) {
      return new ICurio.SoundInfo(sound, 1.0F, 1.0F);
    }
    return ICurioItem.super.getEquipSound(slotContext, stack);
  }

  @Override
  public void curioBreak(SlotContext slotContext, ItemStack stack) {
    call("onBreak", stack, reference(slotContext, stack), slotContext.entity());
  }

  @Override
  public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                     int lootingLevel, boolean recentlyHit, ItemStack stack) {
    Object result = call("getDropRule", stack, reference(slotContext, stack),
        slotContext.entity());
    if (result instanceof Enum<?> rule) {
      try {
        return ICurio.DropRule.valueOf(rule.name());
      } catch (IllegalArgumentException ignored) {
        // Unknown Trinkets drop rules keep the Curios default.
      }
    }
    return ICurio.DropRule.DEFAULT;
  }

  @Override
  public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    Object result = call("getModifiers", stack, reference(slotContext, stack),
        slotContext.entity(), id);
    return convertModifiers(result);
  }

  @SuppressWarnings("unchecked")
  private static Multimap<Holder<Attribute>, AttributeModifier> convertModifiers(Object raw) {
    Multimap<Holder<Attribute>, AttributeModifier> result = LinkedHashMultimap.create();
    if (!(raw instanceof Multimap<?, ?> multimap)) {
      return result;
    }

    for (Map.Entry<?, ?> entry : multimap.entries()) {
      if (!(entry.getKey() instanceof Holder<?> holder)
          || !(entry.getValue() instanceof AttributeModifier modifier)) {
        continue;
      }

      String trinketsSlot = slotAttribute(holder.value());
      if (trinketsSlot == null) {
        if (holder.value() instanceof Attribute attribute) {
          result.put((Holder<Attribute>) holder, modifier);
        }
        continue;
      }

      String curiosSlot = TrinketsCompat.curiosSlotOf(trinketsSlot);
      if (curiosSlot == null && CuriosApi.getSlot(trinketsSlot, false).isPresent()) {
        curiosSlot = trinketsSlot;
      }
      if (curiosSlot == null) {
        CuriosFabricConstants.LOG.warn(
            "Trinkets item requested a slot modifier for unknown path {}", trinketsSlot);
        continue;
      }
      result.put(SlotAttribute.getOrCreate(curiosSlot), modifier);
    }
    return result;
  }

  private static String slotAttribute(Object attribute) {
    if (attribute == null || !attribute.getClass().getName()
        .equals("dev.emi.trinkets.api.SlotAttributes$SlotEntityAttribute")) {
      return null;
    }
    try {
      Field field = attribute.getClass().getField("slot");
      return (String) field.get(attribute);
    } catch (ReflectiveOperationException exception) {
      CuriosFabricConstants.LOG.debug("Could not read Trinkets slot modifier path", exception);
      return null;
    }
  }

  /** The real Trinkets slot reference of this slot, so hooks that inspect it keep working. */
  private Object reference(SlotContext slotContext, ItemStack stack) {
    return TrinketsApiBridge.slotReference(slotContext.identifier(), slotContext.index(),
        slotContext.entity(), stack);
  }

  @Override
  public boolean hasCurioCapability(ItemStack stack) {
    return true;
  }
}
