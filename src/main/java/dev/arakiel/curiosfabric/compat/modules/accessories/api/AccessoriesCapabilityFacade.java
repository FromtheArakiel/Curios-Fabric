package dev.arakiel.curiosfabric.compat.modules.accessories.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.compat.modules.accessories.AccessoriesCompat;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.EquipAction;
import io.wispforest.accessories.api.EquipCheck;
import io.wispforest.accessories.api.EquipmentChecking;
import io.wispforest.accessories.api.caching.ItemStackBasedPredicate;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Transparent Accessories capability view backed by CuriosFabric.
 *
 * <p>No stack or slot storage is copied here. Containers are live adapters over the CuriosFabric
 * stacks handlers.</p>
 */
public final class AccessoriesCapabilityFacade implements AccessoriesCapability {

  private static final Map<LivingEntity, AccessoriesCapabilityFacade> FACADES =
      new WeakHashMap<>();
  private final LivingEntity entity;
  private final Map<String, AccessoriesContainerFacade> containers = new LinkedHashMap<>();

  private AccessoriesCapabilityFacade(LivingEntity entity) {
    this.entity = entity;
  }

  public static AccessoriesCapabilityFacade of(LivingEntity entity) {
    synchronized (FACADES) {
      return FACADES.computeIfAbsent(entity, AccessoriesCapabilityFacade::new);
    }
  }

  public static void clearCache() {
    synchronized (FACADES) {
      FACADES.clear();
    }
  }

  @Override
  public LivingEntity entity() {
    return entity;
  }

  @Override
  public AccessoriesHolder getHolder() {
    return AccessoriesHolder.get(entity);
  }

  @Override
  public void reset(boolean loadedFromTag) {
    containers.clear();
  }

  @Override
  public Map<String, AccessoriesContainer> getContainers() {
    containers.clear();
    ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(entity).orElse(null);
    if (inventory == null) {
      return Map.of();
    }
    boolean client = entity.level().isClientSide();
    for (SlotType slotType : AccessoriesCompat.slots(client)) {
      if (!io.wispforest.accessories.data.EntitySlotLoader
          .getEntitySlots(entity).containsKey(slotType.name())) {
        continue;
      }
      String curiosId = AccessoriesCompat.curiosIdFor(slotType.name(), client);
      if (curiosId == null) {
        continue;
      }
      inventory.getStacksHandler(curiosId).ifPresent(handler ->
          containers.put(
              slotType.name(),
              new AccessoriesContainerFacade(this, slotType.name(), slotType, handler)));
    }
    return Collections.unmodifiableMap(new LinkedHashMap<>(containers));
  }

  @Override
  public void updateContainers() {
    CuriosApi.getCuriosInventory(entity).ifPresent(inventory ->
        inventory.getCurios().values().forEach(handler -> handler.update()));
  }

  @Nullable
  @Override
  public Pair<SlotReference, EquipAction> canEquipAccessory(
      ItemStack stack, boolean allowSwapping, EquipCheck extraCheck) {
    Map<AccessoriesContainer, Boolean> seen = new IdentityHashMap<>();
    for (AccessoriesContainer container : getContainers().values()) {
      if (seen.put(container, Boolean.TRUE) != null) {
        continue;
      }
      for (int i = 0; i < container.getSize(); i++) {
        SlotReference reference = container.createReference(i);
        ItemStack current = reference.getStack();
        if (current == null) {
          continue;
        }
        boolean empty = current.isEmpty();
        if (!extraCheck.isValid(current, !empty && allowSwapping)) {
          continue;
        }
        if ((empty || allowSwapping)
            && io.wispforest.accessories.api.AccessoriesAPI.canInsertIntoSlot(stack, reference)
            && io.wispforest.accessories.api.AccessoriesAPI.canUnequip(current, reference)) {
          return Pair.of(reference, new EquipAction() {
            @Override
            public Optional<ItemStack> equipStack(ItemStack newStack) {
              ItemStack old = current.copy();
              int max = io.wispforest.accessories.api.AccessoriesAPI
                  .getOrDefaultAccessory(newStack).maxStackSize(newStack);
              ItemStack split = newStack.isEmpty()
                  ? ItemStack.EMPTY : newStack.split(Math.max(1, max));
              reference.setStack(split);
              return allowSwapping && !old.isEmpty()
                  ? Optional.of(old) : Optional.empty();
            }
          });
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public SlotEntryReference getFirstEquipped(
      ItemStackBasedPredicate predicate, EquipmentChecking checking) {
    ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(entity).orElse(null);
    if (inventory == null) {
      return null;
    }
    for (String curiosId : inventory.getCurios().keySet()) {
      List<String> aliases = AccessoriesCompat.upstreamNamesFor(
          curiosId, entity.level().isClientSide());
      if (aliases.isEmpty()) {
        continue;
      }
      var stacksHandler = inventory.getCurios().get(curiosId);
      var stacks = stacksHandler.getStacks();
      var cosmetics = stacksHandler.getCosmeticStacks();
      for (int i = 0; i < stacks.getSlots(); i++) {
        ItemStack stack = stacks.getStackInSlot(i);
        if (checking == EquipmentChecking.COSMETICALLY_OVERRIDABLE) {
          ItemStack cosmetic = cosmetics.getStackInSlot(i);
          if (!cosmetic.isEmpty()) {
            stack = cosmetic;
          }
        }
        if (stack.isEmpty()) {
          continue;
        }
        SlotReference reference = SlotReference.of(entity, aliases.getFirst(), i);
        SlotEntryReference found = AccessoryNestUtils.recursiveStackHandling(
            stack, reference,
            (innerStack, innerReference) -> predicate.test(innerStack)
                ? new SlotEntryReference(innerReference, innerStack) : null);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  @Override
  public List<SlotEntryReference> getEquipped(ItemStackBasedPredicate predicate) {
    List<SlotEntryReference> result = new ArrayList<>();
    for (SlotEntryReference reference : getAllEquipped(true)) {
      if (predicate.test(reference.stack())) {
        result.add(reference);
      }
    }
    return result;
  }

  @Override
  public List<SlotEntryReference> getAllEquipped(boolean recursiveStackLookup) {
    List<SlotEntryReference> result = new ArrayList<>();
    ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(entity).orElse(null);
    if (inventory == null) {
      return result;
    }
    Set<String> visited = new LinkedHashSet<>();
    for (Map.Entry<String, ? extends dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler>
        entry : inventory.getCurios().entrySet()) {
      if (!visited.add(entry.getKey())) {
        continue;
      }
      List<String> aliases = AccessoriesCompat.upstreamNamesFor(
          entry.getKey(), entity.level().isClientSide());
      if (aliases.isEmpty()) {
        continue;
      }
      String upstream = aliases.getFirst();
      var stacks = entry.getValue().getStacks();
      for (int i = 0; i < stacks.getSlots(); i++) {
        ItemStack stack = stacks.getStackInSlot(i);
        if (stack.isEmpty()) {
          continue;
        }
        SlotReference reference = SlotReference.of(entity, upstream, i);
        if (recursiveStackLookup) {
          AccessoryNestUtils.recursiveStackConsumption(
              stack, reference,
              (innerStack, innerReference) -> result.add(
                  new SlotEntryReference(innerReference, innerStack)));
        } else {
          result.add(new SlotEntryReference(reference, stack));
        }
      }
    }
    return result;
  }

  @Override
  public void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
    applyModifiers(modifiers, false);
  }

  @Override
  public void addPersistentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
    applyModifiers(modifiers, true);
  }

  @Override
  public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
    for (Map.Entry<String, Collection<AttributeModifier>> entry : modifiers.asMap().entrySet()) {
      getContainer(entry.getKey()).ifPresent(container ->
          entry.getValue().forEach(modifier -> container.removeModifier(modifier.id())));
    }
  }

  @Override
  public Multimap<String, AttributeModifier> getSlotModifiers() {
    Multimap<String, AttributeModifier> result = HashMultimap.create();
    getContainers().forEach((name, container) ->
        result.putAll(name, container.getModifiers().values()));
    return result;
  }

  @Override
  public void clearSlotModifiers() {
    getContainers().values().forEach(AccessoriesContainer::clearModifiers);
  }

  @Override
  public void clearCachedSlotModifiers() {
    getContainers().values().forEach(AccessoriesContainer::clearCachedModifiers);
  }

  private void applyModifiers(
      Multimap<String, AttributeModifier> modifiers, boolean persistent) {
    for (Map.Entry<String, Collection<AttributeModifier>> entry : modifiers.asMap().entrySet()) {
      getContainer(entry.getKey()).ifPresent(container ->
          entry.getValue().forEach(modifier -> {
            if (persistent) {
              container.addPersistentModifier(modifier);
            } else {
              container.addTransientModifier(modifier);
            }
          }));
    }
  }

  private Optional<AccessoriesContainerFacade> getContainer(String upstreamName) {
    return Optional.ofNullable((AccessoriesContainerFacade) getContainers().get(upstreamName));
  }
}
