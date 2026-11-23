package dev.arakiel.curiosfabric.compat.modules.trinkets.item;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
import dev.arakiel.curiosfabric.compat.modules.trinkets.TrinketsCompat;
import dev.arakiel.curiosfabric.compat.modules.trinkets.asm.TrinketsAsm;
import dev.arakiel.curiosfabric.compat.modules.trinkets.api.TrinketsApiFacade;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import dev.emi.trinkets.api.TrinketsAttributeModifiersComponent;
import net.minecraft.sounds.SoundEvent;

/**
 * Runs a Trinkets item's code hooks through this mod.
 *
 * <p>Trinkets content (for example Potion Ring) puts its behavior in {@code Trinket#tick},
 * {@code onEquip} and {@code onUnequip}. Items tagged for a slot only carry their attributes, so
 * those hooks have to be invoked explicitly while the item lives in a Curios slot. They are called
 * through generated ASM invokers with a synthesized Trinkets {@code SlotReference}; global
 * lifecycle callbacks are re-emitted after the item hook, matching Trinkets' own order.</p>
 */
public class TrinketItemAdapter implements ICurioItem {

  private static final Map<Item, Optional<TrinketItemAdapter>> CACHE =
      new ConcurrentHashMap<>();
  private static final String TRINKET = "dev.emi.trinkets.api.Trinket";
  private static final String TRINKET_ITEM = "dev.emi.trinkets.api.TrinketItem";
  private static final String TRINKETS_API = "dev.emi.trinkets.api.TrinketsApi";

  private final Item item;
  private final Object delegate;
  private final boolean equipFromUse;

  public TrinketItemAdapter(Item item) {
    this.item = item;
    this.delegate = trinket(item);
    this.equipFromUse = trinketItem(item);
  }

  /** Returns the cached adapter, creating it once per item. */
  public static Optional<TrinketItemAdapter> of(Item item) {
    return CACHE.computeIfAbsent(item, key -> {
      TrinketItemAdapter adapter = new TrinketItemAdapter(key);
      return adapter.delegate == null ? Optional.empty() : Optional.of(adapter);
    });
  }

  /** {@code true} when Trinkets has a non-default behavior registered for the item. */
  public static boolean isTrinket(Item item) {
    return of(item).isPresent();
  }

  private static Object trinket(Item item) {
    Class<?> api = CompatTargets.loadClass(TRINKETS_API);
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
    Class<?> trinket = CompatTargets.loadClass(TRINKET);
    if (trinket != null && trinket.isInstance(item)) {
      return item;
    }
    return trinketItem(item) ? item : null;
  }

  private static boolean trinketItem(Item item) {
    Class<?> trinketItem = CompatTargets.loadClass(TRINKET_ITEM);
    return trinketItem != null && trinketItem.isInstance(item);
  }

  private Object call(String name, Object... args) {

    try {

      Object target = this.delegate == null ? this.item : this.delegate;
      return TrinketsAsm.invoker(target.getClass(), name, args).invoke(target, args);
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
    TrinketLifecycleCallbacks.onEquip(slotContext, stack);
  }

  @Override
  public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    call("onUnequip", stack, reference(slotContext, stack), slotContext.entity());
    TrinketLifecycleCallbacks.onUnequip(slotContext, stack);
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
    if (equipFromUse) {
      return true;
    }
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
    ICurio.DropRule rule = TrinketLifecycleCallbacks.mapDropRule(result);
    return TrinketLifecycleCallbacks.applyDropCallback(slotContext, stack, rule);
  }

  @Override
  public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    Object result = call("getModifiers", stack, reference(slotContext, stack),
        slotContext.entity(), id);
    boolean client = slotContext.entity() != null && slotContext.entity().level() != null
        && slotContext.entity().level().isClientSide();
    Multimap<Holder<Attribute>, AttributeModifier> modifiers = convertModifiers(result, client);
    modifiers.putAll(componentModifiers(stack, client));
    return modifiers;
  }

  static Multimap<Holder<Attribute>, AttributeModifier> componentModifiers(
      ItemStack stack, boolean client) {
    Multimap<Holder<Attribute>, AttributeModifier> result = LinkedHashMultimap.create();
    TrinketsAttributeModifiersComponent component =
        stack.get(TrinketsAttributeModifiersComponent.TYPE);
    if (component != null) {
      for (TrinketsAttributeModifiersComponent.Entry entry : component.modifiers()) {
        addModifier(result, entry.attribute(), entry.modifier(), client);
      }
    }
    return result;
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
      addModifier(result, holder, modifier, client);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void addModifier(
      Multimap<Holder<Attribute>, AttributeModifier> result,
      Holder<?> holder,
      AttributeModifier modifier,
      boolean client) {
    String trinketsSlot = slotAttribute(holder.value());
    if (trinketsSlot == null) {
      if (holder.value() instanceof Attribute attribute) {
        result.put((Holder<Attribute>) holder, modifier);
      }
      return;
    }

    String curiosSlot = TrinketsCompat.curiosSlotOf(trinketsSlot, client);
    if (curiosSlot == null && CuriosApi.getSlot(trinketsSlot, false).isPresent()) {
      curiosSlot = trinketsSlot;
    }
    if (curiosSlot == null) {
      CuriosFabricConstants.LOG.warn(
          "Trinkets item requested a slot modifier for unknown path {}", trinketsSlot);
      return;
    }
    result.put(SlotAttribute.getOrCreate(curiosSlot), modifier);
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
    return TrinketsApiFacade.slotReference(slotContext.identifier(), slotContext.index(),
        slotContext.entity(), stack);
  }

  @Override
  public boolean hasCurioCapability(ItemStack stack) {
    return true;
  }
}
