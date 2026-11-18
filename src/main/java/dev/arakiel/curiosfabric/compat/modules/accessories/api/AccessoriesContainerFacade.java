package dev.arakiel.curiosfabric.compat.modules.accessories.api;

import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Accessories container view over one CuriosFabric stacks handler. */
final class AccessoriesContainerFacade implements AccessoriesContainer {

  private final AccessoriesCapability capability;
  private final String upstreamName;
  private final SlotType slotType;
  private final ICurioStacksHandler handler;
  private final AccessoriesInventoryView accessories;
  private final AccessoriesInventoryView cosmetics;

  AccessoriesContainerFacade(
      AccessoriesCapability capability,
      String upstreamName,
      SlotType slotType,
      ICurioStacksHandler handler) {
    this.capability = capability;
    this.upstreamName = upstreamName;
    this.slotType = slotType;
    this.handler = handler;
    this.accessories = new AccessoriesInventoryView(
        capability, slotType, handler.getStacks(), "accessories");
    this.cosmetics = new AccessoriesInventoryView(
        capability, slotType, handler.getCosmeticStacks(), "cosmetic_accessories");
  }

  @Override
  public AccessoriesCapability capability() {
    return capability;
  }

  @Override
  public String getSlotName() {
    return upstreamName;
  }

  @Override
  public SlotType slotType() {
    return slotType;
  }

  @Override
  public List<Boolean> renderOptions() {
    return new ArrayList<>(handler.getRenders());
  }

  @Override
  public ExpandedSimpleContainer getAccessories() {
    return accessories;
  }

  @Override
  public ExpandedSimpleContainer getCosmeticAccessories() {
    return cosmetics;
  }

  @Override
  public int getSize() {
    return handler.getSlots();
  }

  @Override
  public void markChanged(boolean resizingUpdate) {
    handler.update();
  }

  @Override
  public boolean hasChanged() {
    return false;
  }

  @Override
  public void update() {
    handler.update();
  }

  @Override
  public Map<ResourceLocation, AttributeModifier> getModifiers() {
    return handler.getModifiers();
  }

  @Override
  public Set<AttributeModifier> getCachedModifiers() {
    return handler.getCachedModifiers();
  }

  @Override
  public Collection<AttributeModifier> getModifiersForOperation(
      AttributeModifier.Operation operation) {
    return handler.getModifiersByOperation(operation);
  }

  @Override
  public void addTransientModifier(AttributeModifier modifier) {
    handler.addTransientModifier(modifier);
  }

  @Override
  public void addPersistentModifier(AttributeModifier modifier) {
    handler.addPermanentModifier(modifier);
  }

  @Override
  public boolean hasModifier(ResourceLocation location) {
    return handler.getModifiers().containsKey(location);
  }

  @Override
  public void removeModifier(ResourceLocation location) {
    handler.removeModifier(location);
  }

  @Override
  public void clearModifiers() {
    handler.clearModifiers();
  }

  @Override
  public void removeCachedModifiers(AttributeModifier modifier) {
    handler.removeModifier(modifier.id());
  }

  @Override
  public void clearCachedModifiers() {
    handler.clearCachedModifiers();
  }
}
