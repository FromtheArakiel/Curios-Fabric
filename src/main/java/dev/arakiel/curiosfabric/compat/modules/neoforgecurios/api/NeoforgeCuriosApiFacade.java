package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.api;

import com.google.common.collect.Multimap;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.SlotResult;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.compat.contribution.api.ForeignApiFacade;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeContext;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeInvalidationReason;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.duck.NeoforgeCuriosSlotContextDuck;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.asm.NeoforgeCuriosAsm;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import dev.arakiel.curiosfabric.compat.shared.reflection.ForeignProxy;

/**
 * Presents this mod's accessory data through the Neoforge/Neoforge edition's interfaces.
 *
 * <p>Neoforge mods that were written against Curios keep calling {@code CuriosApi} and the capability
 * interfaces of the Neoforge edition. Those interfaces still exist (the Neoforge mod is present), so the
 * bridge hands them {@link java.lang.reflect.Proxy proxies} that answer with <em>our</em> inventory.
 * That is what makes "all accessories live in our Curios" work for the other side's code too.</p>
 */
public final class NeoforgeCuriosApiFacade implements ForeignApiFacade {

  private static final String API = "top.theillusivec4.curios.api.";
  private static final String ITEM_HANDLER = "net.neoforged.neoforge.items.IItemHandlerModifiable";

  /**
   * Renderers taken out of the Neoforge edition's supplier map, keyed by item.
   *
   * <p>The Neoforge edition resolves its registered suppliers into a second map that is only filled by
   * its own client setup - which this mod intentionally cancels - so its {@code getRenderer} always
   * answers empty here. The suppliers themselves are read and instantiated instead.</p>
   */
  private static final Map<Item, Object> FOREIGN_RENDERERS = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.Set<Item> FOREIGN_RENDERERS_MISSING =
      java.util.concurrent.ConcurrentHashMap.newKeySet();
  private static final Map<Object, ICurioStacksHandler> STACKS_HANDLER_DELEGATES =
      Collections.synchronizedMap(new WeakHashMap<>());
  private static volatile Object foreignItemCapability;
  private static volatile Class<?> foreignContextClass;
  private static volatile NeoforgeCuriosAsm.ConstructorInvoker foreignContextConstructor;
  private static volatile Class<?> foreignSlotResultClass;
  private static volatile NeoforgeCuriosAsm.ConstructorInvoker foreignSlotResultConstructor;
  private static volatile Class<?> rendererRegistryClass;
  private static volatile Class<?> dropRuleClass;
  private static volatile Map<String, Object> dropRuleValues;
  private static volatile boolean foreignItemCapabilityUnavailable;

  public NeoforgeCuriosApiFacade() {
  }

  @Override
  public void initialize(BridgeContext context) {
  }

  @Override
  public void invalidate(BridgeInvalidationReason reason) {
    FOREIGN_RENDERERS.clear();
    FOREIGN_RENDERERS_MISSING.clear();
    STACKS_HANDLER_DELEGATES.clear();
  }

  /** The inventory of the entity as a Neoforge {@code ICuriosItemHandler}, or {@code null}. */
  public static Object inventory(LivingEntity entity) {
    ICuriosItemHandler ours = CuriosApi.getCuriosInventory(entity).orElse(null);
    return ours == null ? null : inventory(ours);
  }

  private static Object inventory(ICuriosItemHandler ours) {
    return NeoforgeCuriosAsm.proxy(
        new String[]{API + "type.capability.ICuriosItemHandler", ITEM_HANDLER},
        (name, returnType, parameters, args) -> switch (name) {
          case "getCurios" -> {
            Map<String, Object> map = new LinkedHashMap<>();
            ours.getCurios().forEach((id, handler) -> map.put(id, stacksHandler(handler)));
            yield map;
          }
          case "setCurios" -> {
            if (args[0] instanceof Map<?, ?> map) {
              Map<String, ICurioStacksHandler> delegates = new LinkedHashMap<>();
              map.forEach((id, handler) -> {
                ICurioStacksHandler delegate = STACKS_HANDLER_DELEGATES.get(handler);
                if (id instanceof String key && delegate != null) {
                  delegates.put(key, delegate);
                }
              });
              if (delegates.size() == map.size()) {
                ours.setCurios(delegates);
              }
            }
            yield null;
          }
          case "getStacksHandler" -> Optional.ofNullable(
              ours.getStacksHandler((String) args[0]).map(NeoforgeCuriosApiFacade::stacksHandler)
                  .orElse(null));
          case "getSlots" -> ours.getSlots();
          case "getVisibleSlots" -> ours.getVisibleSlots();
          case "getWearer" -> ours.getWearer();
          case "getEquippedCurios" -> itemHandler(ours.getEquippedCurios());
          case "setEquippedCurio" -> {
            ours.setEquippedCurio((String) args[0], (Integer) args[1], (ItemStack) args[2]);
            yield null;
          }
          case "isEquipped" -> args[0] instanceof Item item ? ours.isEquipped(item)
              : ours.isEquipped(predicate(args[0]));
          case "isSlotActive" -> ours.isSlotActive((String) args[0], (Integer) args[1]);
          case "setSlotActive" -> {
            ours.setSlotActive((String) args[0], (Integer) args[1], (Boolean) args[2]);
            yield null;
          }
          case "setSlotsActive" -> {
            ours.setSlotsActive((String) args[0], (Boolean) args[1]);
            yield null;
          }
          case "findFirstCurio" -> slotResult(findFirstCurio(ours, args));
          case "findCurios" -> findCurios(ours, args).stream()
          .map(NeoforgeCuriosApiFacade::slotResult).toList();
          case "findCurio" -> {
            int index = (Integer) args[1];
            yield args.length > 2 && args[2] instanceof Boolean includeInactive
                ? slotResult(ours.findCurio((String) args[0], index, includeInactive))
                : slotResult(ours.findCurio((String) args[0], index));
          }
          // The foreign edition's own loot mixins are still applied (its mixin config does not know
          // that we took over), and they add this value on top of ours. They must therefore see
          // nothing, otherwise every curio fortune/looting bonus would be counted twice.
          case "getFortuneLevel" -> 0;
          case "getLootingLevel" -> 0;
          case "growSlotType" -> {
            ours.growSlotType((String) args[0], (Integer) args[1]);
            yield null;
          }
          case "shrinkSlotType" -> {
            ours.shrinkSlotType((String) args[0], (Integer) args[1]);
            yield null;
          }
          case "reset" -> {
            ours.reset();
            yield null;
          }
          case "clearCachedSlotModifiers" -> {
            ours.clearCachedSlotModifiers();
            yield null;
          }
          case "handleInvalidStacks" -> {
            ours.handleInvalidStacks();
            yield null;
          }
          case "loseInvalidStack" -> {
            ours.loseInvalidStack((ItemStack) args[0]);
            yield null;
          }
          case "getModifiers" -> ours.getModifiers();
          case "addTransientSlotModifier" -> {
            ours.addTransientSlotModifiers(slotModifier(args));
            yield null;
          }
          case "addTransientSlotModifiers" -> {
            ours.addTransientSlotModifiers(modifiers(args[0]));
            yield null;
          }
          case "addPermanentSlotModifier" -> {
            ours.addPermanentSlotModifiers(slotModifier(args));
            yield null;
          }
          case "addPermanentSlotModifiers" -> {
            ours.addPermanentSlotModifiers(modifiers(args[0]));
            yield null;
          }
          case "removeSlotModifier" -> {
            ours.removeSlotModifiers(slotModifier(args));
            yield null;
          }
          case "removeSlotModifiers" -> {
            ours.removeSlotModifiers(modifiers(args[0]));
            yield null;
          }
          case "clearSlotModifiers" -> {
            ours.clearSlotModifiers();
            yield null;
          }
          case "writeTag" -> ours.writeTag();
          case "readTag" -> {
            ours.readTag((Tag) args[0]);
            yield null;
          }
          case "saveInventory" -> ours.saveInventory((Boolean) args[0]);
          case "loadInventory" -> {
            ours.loadInventory((net.minecraft.nbt.ListTag) args[0]);
            yield null;
          }
          case "getLockedSlots" -> ours.getLockedSlots();
          case "getUpdatingInventories" -> {
            Set<Object> handlers = new HashSet<>();
            ours.getUpdatingInventories().forEach(handler -> handlers.add(stacksHandler(handler)));
            yield handlers;
          }
          case "unlockSlotType" -> {
            ours.growSlotType((String) args[0], (Integer) args[1]);
            yield null;
          }
          case "lockSlotType" -> {
            ours.shrinkSlotType((String) args[0], 1);
            yield null;
          }
          case "processSlots" -> null;
          case "getFortuneBonus" -> 0;
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Object stacksHandler(ICurioStacksHandler ours) {
    Object proxy = NeoforgeCuriosAsm.proxy(new String[]{API + "type.inventory.ICurioStacksHandler"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getStacks" -> dynamicStackHandler(ours.getStacks());
          case "getCosmeticStacks" -> dynamicStackHandler(ours.getCosmeticStacks());
          case "getRenders" -> ours.getRenders();
          case "getActiveStates" -> ours.getActiveStates();
          case "updateActiveState" -> {
            ours.updateActiveState((Integer) args[0]);
            yield null;
          }
          case "canToggleRendering" -> ours.canToggleRendering();
          case "getSlots" -> ours.getSlots();
          case "isVisible" -> ours.isVisible();
          case "hasCosmetic" -> ours.hasCosmetic();
          case "getIdentifier" -> ours.getIdentifier();
          case "serializeNBT" -> ours.serializeNBT();
          case "deserializeNBT" -> {
            ours.deserializeNBT((net.minecraft.nbt.CompoundTag) args[0]);
            yield null;
          }
          case "getDropRule" -> dropRule(ours.getDropRule());
          case "getModifiers" -> ours.getModifiers();
          case "getPermanentModifiers" -> ours.getPermanentModifiers();
          case "getCachedModifiers" -> ours.getCachedModifiers();
          case "getModifiersByOperation" -> ours.getModifiersByOperation(operation(args[0]));
          case "addTransientModifier" -> {
            ours.addTransientModifier((AttributeModifier) args[0]);
            yield null;
          }
          case "addPermanentModifier" -> {
            ours.addPermanentModifier((AttributeModifier) args[0]);
            yield null;
          }
          case "removeModifier" -> {
            ours.removeModifier((ResourceLocation) args[0]);
            yield null;
          }
          case "clearModifiers" -> {
            ours.clearModifiers();
            yield null;
          }
          case "clearCachedModifiers" -> {
            ours.clearCachedModifiers();
            yield null;
          }
          case "update" -> {
            ours.update();
            yield null;
          }
          case "getSyncTag" -> ours.getSyncTag();
          case "applySyncTag" -> {
            ours.applySyncTag((net.minecraft.nbt.CompoundTag) args[0]);
            yield null;
          }
          case "getSizeShift" -> ours.getSizeShift();
          case "copyModifiers" -> {
            ICurioStacksHandler delegate = STACKS_HANDLER_DELEGATES.get(args[0]);
            if (delegate != null) {
              ours.copyModifiers(delegate);
            }
            yield null;
          }
          case "grow" -> {
            ours.grow((Integer) args[0]);
            yield null;
          }
          case "shrink" -> {
            ours.shrink((Integer) args[0]);
            yield null;
          }
          default -> ForeignProxy.defaultValue(returnType);
        });
    if (proxy != null) {
      STACKS_HANDLER_DELEGATES.put(proxy, ours);
    }
    return proxy;
  }

  private static Object dynamicStackHandler(IDynamicStackHandler ours) {
    return NeoforgeCuriosAsm.proxy(
        new String[]{API + "type.inventory.IDynamicStackHandler", ITEM_HANDLER},
        (name, returnType, parameters, args) -> switch (name) {
          case "getSlots" -> ours.getSlots();
          case "getStackInSlot" -> ours.getStackInSlot((Integer) args[0]);
          case "setStackInSlot" -> {
            ours.setStackInSlot((Integer) args[0], (ItemStack) args[1]);
            yield null;
          }
          case "getPreviousStackInSlot" -> ours.getPreviousStackInSlot((Integer) args[0]);
          case "setPreviousStackInSlot" -> {
            ours.setPreviousStackInSlot((Integer) args[0], (ItemStack) args[1]);
            yield null;
          }
          case "grow" -> {
            ours.grow((Integer) args[0]);
            yield null;
          }
          case "shrink" -> {
            ours.shrink((Integer) args[0]);
            yield null;
          }
          case "serializeNBT" -> ours.serializeNBT(
              (net.minecraft.core.HolderLookup.Provider) args[0]);
          case "deserializeNBT" -> {
            ours.deserializeNBT((net.minecraft.core.HolderLookup.Provider) args[0],
                (net.minecraft.nbt.CompoundTag) args[1]);
            yield null;
          }
          case "getSlotLimit" -> ours.getSlotLimit((Integer) args[0]);
          case "isItemValid" -> ours.isItemValid((Integer) args[0], (ItemStack) args[1]);
          case "insertItem" -> ours.insertItem((Integer) args[0], (ItemStack) args[1],
              (Boolean) args[2]);
          case "extractItem" -> ours.extractItem((Integer) args[0], (Integer) args[1],
              (Boolean) args[2]);
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Object itemHandler(dev.arakiel.curiosfabric.api.type.inventory.IItemHandlerModifiable ours) {
    return NeoforgeCuriosAsm.proxy(new String[]{ITEM_HANDLER},
        (name, returnType, parameters, args) -> switch (name) {
          case "getSlots" -> ours.getSlots();
          case "getStackInSlot" -> ours.getStackInSlot((Integer) args[0]);
          case "setStackInSlot" -> {
            ours.setStackInSlot((Integer) args[0], (ItemStack) args[1]);
            yield null;
          }
          case "getSlotLimit" -> ours.getSlotLimit((Integer) args[0]);
          case "isItemValid" -> ours.isItemValid((Integer) args[0], (ItemStack) args[1]);
          case "insertItem" -> ours.insertItem((Integer) args[0], (ItemStack) args[1],
              (Boolean) args[2]);
          case "extractItem" -> ours.extractItem((Integer) args[0], (Integer) args[1],
              (Boolean) args[2]);
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  /** The curio behavior of the stack as a Neoforge {@code ICurio}, or {@code null}. */
  public static Object curio(ItemStack stack) {
    Object foreign = foreignItemCapability(stack);
    if (foreign != null) {
      return foreign;
    }
    ICurio ours = CuriosApi.getCurio(stack).orElse(null);

    if (ours == null) {
      return null;
    }
    return NeoforgeCuriosAsm.proxy(new String[]{API + "type.capability.ICurio"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getStack" -> ours.getStack();
          case "curioTick" -> {
            ours.curioTick(context(args[0]));
            yield null;
          }
          case "canEquip" -> ours.canEquip(context(args[0]));
          case "canUnequip" -> ours.canUnequip(context(args[0]));
          case "getSlotsTooltip" -> args.length > 1
              ? ours.getSlotsTooltip(components(args[0]), (Item.TooltipContext) args[1])
              : ours.getSlotsTooltip(components(args[0]));
          case "getAttributeModifiers" -> args[1] instanceof java.util.UUID uuid
              ? ours.getAttributeModifiers(context(args[0]), uuid)
              : ours.getAttributeModifiers(context(args[0]), (ResourceLocation) args[1]);
          case "getAttributesTooltip" -> args.length > 1
              ? ours.getAttributesTooltip(components(args[0]), (Item.TooltipContext) args[1])
              : ours.getAttributesTooltip(components(args[0]));
          case "curioBreak" -> {
            ours.curioBreak(context(args[0]));
            yield null;
          }
          case "canSync" -> ours.canSync(context(args[0]));
          case "writeSyncData" -> ours.writeSyncData(context(args[0]));
          case "readSyncData" -> {
            ours.readSyncData(context(args[0]), (net.minecraft.nbt.CompoundTag) args[1]);
            yield null;
          }
          case "getDropRule" -> args.length == 3
              ? dropRule(ours.getDropRule(context(args[0]),
                  (net.minecraft.world.damagesource.DamageSource) args[1], (Boolean) args[2]))
              : dropRule(ours.getDropRule(context(args[0]),
                  (net.minecraft.world.damagesource.DamageSource) args[1], (Integer) args[2],
                  (Boolean) args[3]));
          case "getFortuneLevel" -> ours.getFortuneLevel(context(args[0]),
              args.length > 1 ? (LootContext) args[1] : null);
          case "getLootingLevel" -> ours.getLootingLevel(context(args[0]),
              args.length > 1 ? (LootContext) args[1] : null);
          case "onEquipFromUse" -> {
            ours.onEquipFromUse(context(args[0]));
            yield null;
          }
          case "getEquipSound" -> soundInfo(ours.getEquipSound(context(args[0])));
          case "canEquipFromUse" -> ours.canEquipFromUse(context(args[0]));
          case "onEquip" -> {
            ours.onEquip(context(args[0]), (ItemStack) args[1]);
            yield null;
          }
          case "onUnequip" -> {
            ours.onUnequip(context(args[0]), (ItemStack) args[1]);
            yield null;
          }
          case "makesPiglinsNeutral" -> ours.makesPiglinsNeutral(context(args[0]));
          case "canWalkOnPowderedSnow" -> ours.canWalkOnPowderedSnow(context(args[0]));
          case "isEnderMask" -> ours.isEnderMask(context(args[0]), (EnderMan) args[1]);
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  /** A Neoforge {@code ISlotType} view of one of our slot types. */
  public static Object slotType(ISlotType ours) {
    return NeoforgeCuriosAsm.proxy(new String[]{API + "type.ISlotType"},
        (name, returnType, parameters, args) -> switch (name) {
          case "getIdentifier" -> ours.getIdentifier();
          case "getIcon" -> ours.getIcon();
          case "getOrder" -> ours.getOrder().orElse(0);
          case "getSize" -> ours.getSize();
          case "useNativeGui" -> ours.useNativeGui();
          case "hasCosmetic" -> ours.hasCosmetic();
          case "canToggleRendering" -> ours.canToggleRendering();
          case "getValidators" -> ours.getValidators();
          case "writeNbt" -> ours.writeNbt();
          case "getDropRule" -> dropRule(ours.getDropRule());
          case "isLocked" -> ours.isLocked();
          case "getPriority" -> ours.getPriority();
          case "isVisible" -> ours.isVisible();
          case "compareTo" -> ours.getIdentifier().compareTo(readIdentifier(args[0]));
          default -> ForeignProxy.defaultValue(returnType);
        });
  }

  private static Optional<SlotResult> findFirstCurio(ICuriosItemHandler ours, Object[] args) {
    Predicate<ItemStack> filter = argumentFilter(args);
    if (args.length >= 3 && args[1] instanceof Boolean includeInactive
        && args[2] instanceof String cacheKey) {
      return ours.findFirstCurio(filter, includeInactive, cacheKey);
    }
    if (args.length >= 2 && args[1] instanceof String cacheKey) {
      return ours.findFirstCurio(filter, cacheKey);
    }
    return ours.findFirstCurio(filter);
  }

  private static List<SlotResult> findCurios(ICuriosItemHandler ours, Object[] args) {
    if (args.length == 1) {
      if (args[0] instanceof Item item) {
        return ours.findCurios(item);
      }
      if (args[0] instanceof Predicate<?> predicate) {
        return ours.findCurios(predicate(predicate));
      }
      if (args[0] instanceof String[] ids) {
        return ours.findCurios(ids);
      }
    }
    if (args.length >= 2 && args[0] instanceof Boolean && args[1] instanceof String[] ids) {
      return ours.findCurios(ids);
    }
    if (args.length >= 3 && args[0] instanceof Predicate<?> predicate
        && args[1] instanceof Boolean includeInactive && args[2] instanceof String cacheKey) {
      return ours.findCurios(predicate(predicate), includeInactive, cacheKey);
    }
    return List.of();
  }

  private static Multimap<String, AttributeModifier> slotModifier(Object[] args) {
    Multimap<String, AttributeModifier> result = com.google.common.collect.HashMultimap.create();
    if (args.length >= 4 && args[0] instanceof String slot
        && args[1] instanceof ResourceLocation id && args[2] instanceof Double amount) {
      result.put(slot, new AttributeModifier(id, amount, operation(args[3])));
    }
    return result;
  }

  private static String readIdentifier(Object foreign) {
    try {
      Object value = NeoforgeCuriosAsm
          .invoker(foreign.getClass(), "getIdentifier", new Object[0])
          .invoke(foreign, new Object[0]);
      return String.valueOf(value);
    } catch (Throwable throwable) {
      return "";
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Component> components(Object value) {
    return value instanceof List<?> list ? (List<Component>) list : new java.util.ArrayList<>();
  }

  private static Object soundInfo(ICurio.SoundInfo ours) {
    try {
      Class<?> type = CompatTargets
          .loadClass(API + "type.capability.ICurio$SoundInfo");
      return type == null ? null
          : NeoforgeCuriosAsm.constructor(
              type, new Class<?>[]{SoundEvent.class, float.class, float.class})
              .create(new Object[]{ours.soundEvent(), ours.volume(), ours.pitch()});
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Failed to build a bridged curio sound", throwable);
      return null;
    }
  }

  private static Object foreignItemCapability(ItemStack stack) {
    if (stack == null || stack.isEmpty() || foreignItemCapabilityUnavailable) {
      return null;
    }
    try {
      Object capability = foreignItemCapability;
      if (capability == null) {
        Class<?> holder = CompatTargets
            .loadClass(API + "CuriosCapability");
        if (holder == null) {
          foreignItemCapabilityUnavailable = true;
          return null;
        }
        Field item = holder.getField("ITEM");
        capability = item.get(null);
        foreignItemCapability = capability;
      }
      return NeoforgeCuriosAsm
          .invoker(stack.getClass(), "getCapability", new Object[]{capability, null})
          .invoke(stack, new Object[]{capability, null});
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Could not read the Neoforge curio item capability",
          throwable);
    }
    return null;
  }

  private static Object slotResult(Optional<SlotResult> result) {
    // Must always be an Optional: the foreign API returns Optional<SlotResult> and callers such as
    // Shulker Box Slot cast the result straight to Optional, so returning the bare SlotResult
    // crashed the client with a ClassCastException.
    return result.flatMap(ourResult -> Optional.ofNullable(slotResult(ourResult)));
  }

  /**
   * Invokes the renderer that the Neoforge edition registered for this item, if it has one.
   *
   * <p>Everything is passed as {@link Object} on purpose: this class is loaded on the dedicated
   * server too, where client-only rendering types do not exist. The argument order matches the
   * Neoforge renderer contract: stack, slot context, pose stack, render layer parent, buffer source,
   * light, limb swing, limb swing amount, partial ticks, age in ticks, net head yaw, head pitch.</p>
   *
   * @param args The render arguments
   * @return {@code true} when a foreign renderer was invoked
   */
  public static boolean renderForeign(Object[] args) {

    try {
      Class<?> registry = rendererRegistry();

      if (registry == null || args.length == 0) {
        return false;
      }
      Item item = ((ItemStack) args[0]).getItem();
      Object renderer = foreignRenderer(registry, item);

      if (renderer == null) {
        return false;
      }
      if (!(args[1] instanceof SlotContext oursContext)) {
        CuriosFabricConstants.LOG.warn(
            "NeoForge Curios renderer received wrong context type item={} type={}",
            BuiltInRegistries.ITEM.getKey(item),
            args[1] == null ? "null" : args[1].getClass().getName());
        return false;
      }
      Object[] call = args.clone();
      call[1] = foreignContext(oursContext);
      if (call[1] == null) {
        CuriosFabricConstants.LOG.warn(
            "NeoForge Curios context conversion failed item={} slot={} index={}",
            BuiltInRegistries.ITEM.getKey(((ItemStack) args[0]).getItem()),
            oursContext.identifier(), oursContext.index());
        return false;
      }

      try {
        NeoforgeCuriosAsm.invoker(renderer.getClass(), "render", call)
            .invoke(renderer, call);
        return true;
      } catch (Throwable lastFailure) {
        CuriosFabricConstants.LOG.warn(
            "NeoForge Curios renderer failed item={} renderer={}",
            BuiltInRegistries.ITEM.getKey(item), renderer.getClass().getName(), lastFailure);
      }
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.warn("NeoForge Curios renderer invocation failed", throwable);
    }
    return false;
  }

  /** {@code true} when the Neoforge registry has a renderer for this item. */
  public static boolean hasRenderer(ItemStack stack) {
    Class<?> registry = rendererRegistry();
    return registry != null && stack != null && !stack.isEmpty()
        && foreignRenderer(registry, stack.getItem()) != null;
  }

  /**
   * Resolves a renderer that a Neoforge mod registered with the Neoforge edition's registry.
   *
   * <p>{@code CuriosRendererRegistry.getRenderer} only reads the cache that its own client setup is
   * supposed to fill. Since that setup is cancelled while this mod owns the accessory system, the
   * suppliers are read straight from the registry map and resolved here instead.</p>
   */
  private static Object foreignRenderer(Class<?> registry, Item item) {
    Object cached = FOREIGN_RENDERERS.get(item);

    if (cached != null) {
      return cached;
    }

    if (FOREIGN_RENDERERS_MISSING.contains(item)) {
      return null;
    }

    try {
      // Prefer the foreign registry's own cache so a renderer that the Neoforge edition did resolve is
      // reused instead of being built a second time.
      Object optional = registry.getMethod("getRenderer", Item.class).invoke(null, item);

      if (optional instanceof Optional<?> found && found.isPresent()) {
        Object renderer = found.get();
        FOREIGN_RENDERERS.put(item, renderer);
        return renderer;
      }

      FOREIGN_RENDERERS_MISSING.add(item);
      return null;
    } catch (Throwable throwable) {
      FOREIGN_RENDERERS_MISSING.add(item);
      return null;
    }
  }

  private static Object slotResult(SlotResult ours) {

    try {
      Class<?> contextClass = foreignContextClass();
      Class<?> resultClass = foreignSlotResultClass();

      if (contextClass == null || resultClass == null) {
        return null;
      }
      return foreignSlotResultConstructor()
          .create(new Object[]{foreignContext(ours.slotContext()), ours.stack()});
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Failed to build a bridged SlotResult", throwable);
      return null;
    }
  }

  /** Builds a Neoforge {@code SlotContext} from ours, or {@code null} when it is unavailable. */
  public static Object foreignContext(SlotContext ours) {

    try {
      Class<?> contextClass = foreignContextClass();
      if (contextClass == null || ours == null) {
        return null;
      }
      return foreignContextConstructor().create(new Object[]{
          ours.identifier(), ours.entity(), ours.index(), ours.cosmetic(), ours.visible()});
    } catch (Throwable throwable) {
      return null;
    }
  }

  private static Object slotResult(Object foreign) {
    return foreign == null ? Optional.empty() : Optional.of(foreign);
  }

  private static Class<?> rendererRegistry() {
    Class<?> cached = rendererRegistryClass;
    if (cached == null) {
      cached = CompatTargets.loadClass(API + "client.CuriosRendererRegistry");
      rendererRegistryClass = cached;
    }
    return cached;
  }

  private static Class<?> foreignContextClass() {
    Class<?> cached = foreignContextClass;
    if (cached == null) {
      cached = CompatTargets.loadClass(API + "SlotContext");
      foreignContextClass = cached;
    }
    return cached;
  }

  private static NeoforgeCuriosAsm.ConstructorInvoker foreignContextConstructor() {
    NeoforgeCuriosAsm.ConstructorInvoker cached = foreignContextConstructor;
    if (cached == null) {
      Class<?> type = foreignContextClass();
      cached = type == null
          ? (args -> null)
          : NeoforgeCuriosAsm.constructor(type, new Class<?>[]{
              String.class, LivingEntity.class, int.class, boolean.class, boolean.class});
      foreignContextConstructor = cached;
    }
    return cached;
  }

  private static Class<?> foreignSlotResultClass() {
    Class<?> cached = foreignSlotResultClass;
    if (cached == null) {
      cached = CompatTargets.loadClass(API + "SlotResult");
      foreignSlotResultClass = cached;
    }
    return cached;
  }

  private static NeoforgeCuriosAsm.ConstructorInvoker foreignSlotResultConstructor() {
    NeoforgeCuriosAsm.ConstructorInvoker cached = foreignSlotResultConstructor;
    if (cached == null) {
      Class<?> context = foreignContextClass();
      Class<?> result = foreignSlotResultClass();
      cached = context == null || result == null
          ? (args -> null)
          : NeoforgeCuriosAsm.constructor(result, new Class<?>[]{context, ItemStack.class});
      foreignSlotResultConstructor = cached;
    }
    return cached;
  }

  private static Object dropRule(ICurio.DropRule ours) {
    Class<?> type = dropRuleClass;
    if (type == null) {
      type = CompatTargets.loadClass(API + "type.capability.ICurio$DropRule");
      dropRuleClass = type;
    }
    if (type == null) {
      return null;
    }
    Map<String, Object> values = dropRuleValues;
    if (values == null) {
      Map<String, Object> discovered = new LinkedHashMap<>();
      for (Object constant : type.getEnumConstants()) {
        discovered.put(((Enum<?>) constant).name(), constant);
      }
      values = Map.copyOf(discovered);
      dropRuleValues = values;
    }
    return values.get(ours.name());
  }

  private static AttributeModifier.Operation operation(Object foreign) {
    return foreign instanceof AttributeModifier.Operation operation ? operation
        : AttributeModifier.Operation.ADD_VALUE;
  }

  @SuppressWarnings("unchecked")
  private static Multimap<String, AttributeModifier> modifiers(Object foreign) {
    return foreign instanceof Multimap<?, ?> multimap
        ? (Multimap<String, AttributeModifier>) multimap : com.google.common.collect.HashMultimap.create();
  }

  /** Converts a Neoforge {@code SlotContext} into ours. */
  private static SlotContext context(Object foreign) {

    if (foreign instanceof SlotContext ours) {
      return ours;
    }
    NeoforgeCuriosSlotContextDuck context = (NeoforgeCuriosSlotContextDuck) (Object) foreign;
    return new SlotContext(
        context.curiosfabric$identifier(),
        context.curiosfabric$entity(),
        context.curiosfabric$index(),
        context.curiosfabric$cosmetic(),
        context.curiosfabric$visible());
  }

  @SuppressWarnings("unchecked")
  private static Predicate<ItemStack> predicate(Object foreign) {
    return foreign instanceof Predicate<?> predicate ? (Predicate<ItemStack>) predicate
        : stack -> false;
  }

  private static Predicate<ItemStack> argumentFilter(Object[] args) {

    if (args.length > 0 && args[0] instanceof Item item) {
      return stack -> stack.getItem() == item;
    }
    return args.length > 0 ? predicate(args[0]) : stack -> false;
  }

}
