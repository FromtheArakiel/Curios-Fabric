package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.item;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedHashMultimap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.SlotAttribute;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.api.NeoforgeCuriosApiFacade;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import dev.arakiel.curiosfabric.compat.shared.reflection.ForeignProxy;

/**
 * Runs a Neoforge/Neoforge {@code ICurioItem}'s code hooks through this mod.
 *
 * <p>This is the part a data driven bridge cannot cover: a Neoforge mod's curio usually implements the
 * Neoforge {@code ICurioItem} interface and puts its logic in {@code curioTick}, {@code onEquip},
 * {@code canEquip}, drop rules and so on. Those methods are invoked here through reflection, with
 * the slot context translated between the two APIs, so the behavior of such an item keeps working
 * while its data lives in this mod's slots.</p>
 */
public class NeoforgeCuriosItemAdapter implements ICurioItem {

  private static final String FOREIGN = "top.theillusivec4.curios.api.type.capability.ICurioItem";

  private final Item item;

  public NeoforgeCuriosItemAdapter(Item item) {
    this.item = item;
  }

  /** {@code true} when the item implements the Neoforge edition's curio interface. */
  public static boolean isNeoforgeCuriosItemItem(Item item) {
    Class<?> type = CompatTargets.loadClass(FOREIGN);
    return type != null && type.isInstance(item);
  }

  private Object call(String name, int parameterCount, Object... args) {

    try {

      for (Method method : this.item.getClass().getMethods()) {

        if (method.getName().equals(name) && accepts(method, parameterCount, args)) {
          return method.invoke(this.item, args);
        }
      }
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Bridged curio hook {} failed for {}",
          name, this.item, throwable);
    }
    return null;
  }

  private static boolean accepts(Method method, int parameterCount, Object[] args) {
    if (method.getParameterCount() != parameterCount) {
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

  private boolean overrides(String name, int parameterCount) {
    for (Method method : this.item.getClass().getMethods()) {
      if (method.getName().equals(name) && method.getParameterCount() == parameterCount
          && !method.getDeclaringClass().isInterface()) {
        return true;
      }
    }
    return false;
  }

  private Object context(SlotContext slotContext) {
    return NeoforgeCuriosApiFacade.foreignContext(slotContext);
  }

  @Override
  public boolean hasCurioCapability(ItemStack stack) {
    Object result = call("hasCurioCapability", 1, stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public void curioTick(SlotContext slotContext, ItemStack stack) {
    call("curioTick", 2, context(slotContext), stack);
  }

  @Override
  public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    call("onEquip", 3, context(slotContext), prevStack, stack);
  }

  @Override
  public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    call("onUnequip", 3, context(slotContext), newStack, stack);
  }

  @Override
  public boolean canEquip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canEquip", 2, context(slotContext), stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
    Object result = call("canUnequip", 2, context(slotContext), stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public void onEquipFromUse(SlotContext slotContext, ItemStack stack) {
    call("onEquipFromUse", 2, context(slotContext), stack);
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    Object result = call("canEquipFromUse", 2, context(slotContext), stack);
    return !(result instanceof Boolean value) || value;
  }

  @Override
  public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
    Object result = call("getEquipSound", 2, context(slotContext), stack);
    Object sound = ForeignProxy.read(result, "soundEvent",
        ForeignProxy.read(result, "getSoundEvent", null));
    Object volume = ForeignProxy.read(result, "volume",
        ForeignProxy.read(result, "getVolume", null));
    Object pitch = ForeignProxy.read(result, "pitch",
        ForeignProxy.read(result, "getPitch", null));
    if (sound instanceof SoundEvent event && volume instanceof Number volumeValue
        && pitch instanceof Number pitchValue) {
      return new ICurio.SoundInfo(event, volumeValue.floatValue(), pitchValue.floatValue());
    }
    return ICurioItem.super.getEquipSound(slotContext, stack);
  }

  @Override
  public void curioBreak(SlotContext slotContext, ItemStack stack) {
    call("curioBreak", 2, context(slotContext), stack);
  }

  @Override
  public boolean canSync(SlotContext slotContext, ItemStack stack) {
    return Boolean.TRUE.equals(call("canSync", 2, context(slotContext), stack));
  }

  @Override
  public CompoundTag writeSyncData(SlotContext slotContext, ItemStack stack) {
    Object result = call("writeSyncData", 2, context(slotContext), stack);
    return result instanceof CompoundTag tag ? tag : new CompoundTag();
  }

  @Override
  public void readSyncData(SlotContext slotContext, CompoundTag compound, ItemStack stack) {
    call("readSyncData", 3, context(slotContext), compound, stack);
  }

  @Override
  public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                     int lootingLevel, boolean recentlyHit, ItemStack stack) {
    Object result = overrides("getDropRule", 4) || !overrides("getDropRule", 5)
        ? call("getDropRule", 4, context(slotContext), source, recentlyHit, stack)
        : call("getDropRule", 5, context(slotContext), source, lootingLevel, recentlyHit, stack);

    if (result instanceof Enum<?> value) {

      try {
        return ICurio.DropRule.valueOf(value.name());
      } catch (IllegalArgumentException exception) {
        return ICurio.DropRule.DEFAULT;
      }
    }
    return ICurio.DropRule.DEFAULT;
  }

  @Override
  public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
    Object result = call("getFortuneLevel", 3, context(slotContext), lootContext, stack);
    return result instanceof Integer value ? value : 0;
  }

  @Override
  public int getLootingLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
    Object result = call("getLootingLevel", 3, context(slotContext), lootContext, stack);
    return result instanceof Integer value ? value : 0;
  }

  @Override
  public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    Object result = call("getAttributeModifiers", 3, context(slotContext), id, stack);

    if (result instanceof Multimap<?, ?> multimap) {
      Multimap<Holder<Attribute>, AttributeModifier> converted = LinkedHashMultimap.create();
      for (Map.Entry<?, ?> entry : multimap.entries()) {
        if (!(entry.getKey() instanceof Holder<?> holder)
            || !(entry.getValue() instanceof AttributeModifier modifier)) {
          continue;
        }
        String inheritedSlot = foreignSlotAttribute(holder.value());
        if (inheritedSlot != null) {
          converted.put(SlotAttribute.getOrCreate(inheritedSlot), modifier);
        } else if (holder.value() instanceof Attribute attribute) {
          @SuppressWarnings("unchecked")
          Holder<Attribute> cast = (Holder<Attribute>) holder;
          converted.put(cast, modifier);
        }
      }
      return converted;
    }
    return LinkedHashMultimap.create();
  }

  private static String foreignSlotAttribute(Object attribute) {
    if (attribute == null || !attribute.getClass().getName()
        .equals("top.theillusivec4.curios.api.SlotAttribute")) {
      return null;
    }
    try {
      return (String) attribute.getClass().getMethod("getIdentifier").invoke(attribute);
    } catch (ReflectiveOperationException exception) {
      CuriosFabricConstants.LOG.debug("Could not read NeoForge Curios slot modifier", exception);
      return null;
    }
  }

  @Override
  public boolean makesPiglinsNeutral(SlotContext slotContext, ItemStack stack) {
    return Boolean.TRUE.equals(call("makesPiglinsNeutral", 2, context(slotContext), stack));
  }

  @Override
  public boolean canWalkOnPowderedSnow(SlotContext slotContext, ItemStack stack) {
    return Boolean.TRUE.equals(call("canWalkOnPowderedSnow", 2, context(slotContext), stack));
  }

  @Override
  public boolean isEnderMask(SlotContext slotContext, EnderMan enderMan, ItemStack stack) {
    return Boolean.TRUE.equals(call("isEnderMask", 3, context(slotContext), enderMan, stack));
  }

  @Override
  public List<net.minecraft.network.chat.Component> getSlotsTooltip(
      List<net.minecraft.network.chat.Component> tooltips, TooltipContext context,
      ItemStack stack) {
    Object result = !overrides("getSlotsTooltip", 3) && overrides("getSlotsTooltip", 2)
        ? call("getSlotsTooltip", 2, tooltips, stack)
        : call("getSlotsTooltip", 3, tooltips, context, stack);
    return result instanceof List<?> list ? castComponents(list) : tooltips;
  }

  @Override
  public List<net.minecraft.network.chat.Component> getSlotsTooltip(
      List<net.minecraft.network.chat.Component> tooltips, ItemStack stack) {
    Object result = call("getSlotsTooltip", 2, tooltips, stack);
    return result instanceof List<?> list ? castComponents(list) : tooltips;
  }

  @Override
  public List<net.minecraft.network.chat.Component> getAttributesTooltip(
      List<net.minecraft.network.chat.Component> tooltips, TooltipContext context,
      ItemStack stack) {
    Object result = !overrides("getAttributesTooltip", 3)
        && overrides("getAttributesTooltip", 2)
        ? call("getAttributesTooltip", 2, tooltips, stack)
        : call("getAttributesTooltip", 3, tooltips, context, stack);
    return result instanceof List<?> list ? castComponents(list) : tooltips;
  }

  @Override
  public List<net.minecraft.network.chat.Component> getAttributesTooltip(
      List<net.minecraft.network.chat.Component> tooltips, ItemStack stack) {
    Object result = call("getAttributesTooltip", 2, tooltips, stack);
    return result instanceof List<?> list ? castComponents(list) : tooltips;
  }

  private static List<net.minecraft.network.chat.Component> castComponents(List<?> list) {
    List<net.minecraft.network.chat.Component> result = new ArrayList<>();

    for (Object element : list) {

      if (element instanceof net.minecraft.network.chat.Component component) {
        result.add(component);
      }
    }
    return result;
  }
}
