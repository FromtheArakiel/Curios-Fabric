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
 * through reflection with a synthesized Trinkets {@code SlotReference}; any failure is caught so a
 * broken foreign hook cannot break the tick loop.</p>
 */
public class TrinketItemAdapter implements ICurioItem {

  private static final String TRINKET = "dev.emi.trinkets.api.Trinket";
  private static final String TRINKET_ITEM = "dev.emi.trinkets.api.TrinketItem";
  private static final String TRINKETS_API = "dev.emi.trinkets.api.TrinketsApi";

  private final Item item;
  private final Object delegate;

  public TrinketItemAdapter(Item item) {
    this.item = item;
    this.delegate = trinket(item);
  }

  /** {@code true} when Trinkets has a non-default behavior registered for the item. */
  public static boolean isTrinket(Item item) {
    return trinket(item) != null;
  }

  private static Object trinket(Item item) {
    Class<?> api = dev.arakiel.curiosfabric.compat.CompatTargets.loadClass(TRINKETS_API);
    if (api != null) {
      try {
        Object resolved = api.getMethod("getTrinket", Item.class).invoke(null, item);
        Object fallback = api.getMethod("getDefaultTrinket").invoke(null);
        if (resolved != null && resolved != fallback) {
          return resolved;
        }
      } catch (Throwable ignored) {
        // Fall through to the item's implemented interface.
      }
    }
    Class<?> trinket = dev.arakiel.curiosfabric.compat.CompatTargets.loadClass(TRINKET);
    if (trinket != null && trinket.isInstance(item)) {
      return item;
    }
    Class<?> trinketItem = dev.arakiel.curiosfabric.compat.CompatTargets.loadClass(TRINKET_ITEM);
    return trinketItem != null && trinketItem.isInstance(item) ? item : null;
  }

  private Object call(String name, Object... args) {

    try {

      Object target = this.delegate == null ? this.item : this.delegate;
      for (Method method : target.getClass().getMethods()) {

        if (method.getName().equals(name) && accepts(method, args)) {
          return method.invoke(target, args);
        }
      }
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Bridged Trinkets hook {} failed for {}", name, this.item,
          throwable);
    }
    return null;
  }

  private static boolean accepts(Method method, Object[] args) {
    if (method.getParameterCount() != args.length) {
      return false;
    }
    Class<?>[] types = method.getParameterTypes();
    for (int index = 0; index < types.length; index++) {
      Object argument = args[index];
      if (argument == null) {
        if (types[index].isPrimitive()) {
          return false;
        }
      } else if (!box(types[index]).isInstance(argument)) {
        return false;
      }
    }
    return true;
  }

  private static Class<?> box(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    return Character.class;
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
    boolean client = slotContext.entity() != null && slotContext.entity().level() != null
        && slotContext.entity().level().isClientSide();
    return convertModifiers(result, client);
  }

  @SuppressWarnings("unchecked")
  private static Multimap<Holder<Attribute>, AttributeModifier> convertModifiers(
      Object raw, boolean client) {
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

      String curiosSlot = TrinketsCompat.curiosSlotOf(trinketsSlot, client);
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
