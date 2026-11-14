package dev.arakiel.curiosfabric.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CurioAttributeModifiers;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.CuriosTags;
import dev.arakiel.curiosfabric.api.SlotAttribute;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.SlotResult;
import dev.arakiel.curiosfabric.api.event.CurioAttributeModifierEvent;
import dev.arakiel.curiosfabric.api.event.CuriosEventBus;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.type.capability.ICurioItem;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.common.CuriosFabricRegistry;
import dev.arakiel.curiosfabric.common.capability.CurioInventoryCapability;
import dev.arakiel.curiosfabric.common.capability.ItemizedCurioCapability;
import dev.arakiel.curiosfabric.common.data.CuriosEntityManager;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.common.network.NetworkHandler;
import dev.arakiel.curiosfabric.common.network.server.SPacketBreak;
import dev.arakiel.curiosfabric.compat.api.CompatibilityBridges;
import dev.arakiel.curiosfabric.compat.api.ItemBehaviorBridge;
import dev.arakiel.curiosfabric.compat.api.ItemTagBridge;
import dev.arakiel.curiosfabric.compat.neoforgecurios.NeoforgeCuriosCompat;
import dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat;

/**
 * Holds the actual implementations of the {@link CuriosApi} methods.
 *
 * <p>The class name is a leftover of the multiloader layout, where the API was a stub that these
 * hooks filled in through a mixin. The Fabric build calls them directly, but the name is kept so
 * that third party patches (for example the compatibility layers) keep working.</p>
 */
public class CuriosImplMixinHooks {

  private static final Map<Item, ICurioItem> REGISTRY = new ConcurrentHashMap<>();

  public static void registerCurio(Item item, ICurioItem icurio) {
    REGISTRY.put(item, icurio);
  }

  public static Optional<ICurioItem> getCurioFromRegistry(Item item) {
    return Optional.ofNullable(REGISTRY.get(item));
  }

  public static Map<String, ISlotType> getSlots(boolean isClient) {
    CuriosSlotManager slotManager = isClient ? CuriosSlotManager.CLIENT : CuriosSlotManager.SERVER;
    return slotManager.getSlots();
  }

  public static Map<String, ISlotType> getEntitySlots(EntityType<?> type, boolean isClient) {
    CuriosEntityManager entityManager =
        isClient ? CuriosEntityManager.CLIENT : CuriosEntityManager.SERVER;
    return entityManager.getEntitySlots(type);
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack, boolean isClient) {
    Map<String, ISlotType> slots = filteredSlots(slotType -> {
      SlotContext slotContext =
          new SlotContext(slotType.getMappingIdentifier(), null, 0, false, true);
      SlotResult slotResult = new SlotResult(slotContext, stack);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, CuriosApi.getSlots(isClient));
    addCompatSlots(stack, slots, CuriosApi.getSlots(isClient), isClient);
    return slots;
  }

  public static Map<String, ISlotType> getItemStackSlots(ItemStack stack,
                                                         LivingEntity livingEntity) {
    Map<String, ISlotType> allSlots = CuriosApi.getEntitySlots(livingEntity);
    Map<String, ISlotType> slots = filteredSlots(slotType -> {
      SlotContext slotContext =
          new SlotContext(slotType.getMappingIdentifier(), livingEntity, 0, false, true);
      SlotResult slotResult = new SlotResult(slotContext, stack);
      return CuriosApi.testCurioPredicates(slotType.getValidators(), slotResult);
    }, allSlots);
    addCompatSlots(stack, slots, allSlots, livingEntity.level().isClientSide());
    return slots;
  }

  /**
   * Adds the slots that the compatibility bridges map an item to.
   *
   * <p>Items from Trinkets or from the neoforge Neoforge edition are not listed in the
   * {@code #curios:<slot>} item tags, so their slot assignment is inferred from the foreign tags
   * here. Otherwise they would fail the validators of every slot type.</p>
   */
  private static void addCompatSlots(ItemStack stack, Map<String, ISlotType> result,
                                     Map<String, ISlotType> allSlots, boolean client) {

    for (ItemTagBridge bridge : CompatibilityBridges.itemTags()) {
      for (String slotId : bridge.slotsFor(stack, client)) {
        for (Map.Entry<String, ISlotType> entry : allSlots.entrySet()) {
          if (entry.getValue().getMappingIdentifier().equals(slotId)) {
            result.putIfAbsent(entry.getKey(), entry.getValue());
          }
        }
      }
    }
  }

  private static Map<String, ISlotType> filteredSlots(Predicate<ISlotType> filter,
                                                      Map<String, ISlotType> map) {
    Map<String, ISlotType> result = new HashMap<>();

    for (Map.Entry<String, ISlotType> entry : map.entrySet()) {
      ISlotType slotType = entry.getValue();

      if (filter.test(slotType)) {
        result.put(entry.getKey(), slotType);
      }
    }
    return result;
  }

  /**
   * Looks up the curio behaviour of a stack. This replaces the Neoforge item capability: a
   * registered {@link ICurioItem} wins over an {@link ICurioItem} implemented by the item itself.
   */
  public static Optional<ICurio> getCurio(ItemStack stack) {

    if (stack == null || stack.isEmpty()) {
      return Optional.empty();
    }
    ICurioItem curioItem = getCurioFromRegistry(stack.getItem()).orElse(null);

    if (curioItem == null && stack.getItem() instanceof ICurioItem itemCurio) {
      curioItem = itemCurio;
    }

    if (curioItem == null) {
      // Real Trinkets content (Potion Ring and friends) keeps its behavior in Trinket#tick and the
      // equip hooks, so it has to win over the plain data driven bridge.
      if (TrinketsCompat.isLoaded()
          && dev.arakiel.curiosfabric.compat.bridge.TrinketItemAdapter.isTrinket(stack.getItem())) {
        curioItem = new dev.arakiel.curiosfabric.compat.bridge.TrinketItemAdapter(stack.getItem());
      }
    }

    if (curioItem == null) {
      // A Neoforge mod's curio implements the Neoforge edition's ICurioItem, which carries its code
      // hooks: it has to win over the plain data driven bridge, exactly like the Trinkets case.
      if (NeoforgeCuriosCompat.isLoaded()
          && dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosItemAdapter
          .isNeoforgeCuriosItemItem(stack.getItem())) {
        curioItem = new dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosItemAdapter(stack.getItem());
      }
    }

    for (ItemBehaviorBridge bridge : CompatibilityBridges.itemBehaviors()) {
      if (curioItem == null) {
        curioItem = bridge.resolve(stack).orElse(null);
      }
    }

    // A Trinkets item keeps its behavior in Trinket#tick/onEquip/onUnequip: run those hooks too.
    if (curioItem == null && TrinketsCompat.isLoaded()
        && dev.arakiel.curiosfabric.compat.bridge.TrinketItemAdapter.isTrinket(stack.getItem())) {
      curioItem = new dev.arakiel.curiosfabric.compat.bridge.TrinketItemAdapter(stack.getItem());
    }

    if (curioItem != null && curioItem.hasCurioCapability(stack)) {
      return Optional.of(new ItemizedCurioCapability(curioItem, stack));
    }
    return Optional.empty();
  }

  public static Optional<ICuriosItemHandler> getCuriosInventory(LivingEntity livingEntity) {

    if (livingEntity == null) {
      return Optional.empty();
    }

    if (CuriosApi.getEntitySlots(livingEntity).isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new CurioInventoryCapability(livingEntity));
  }

  public static boolean isStackValid(SlotContext slotContext, ItemStack stack) {
    String id = slotContext.identifier();
    LivingEntity entity = slotContext.entity();
    boolean clientSide = entity != null ? entity.level().isClientSide()
        : CuriosFabricConstants.IS_CLIENT;
    ISlotType targetSlot = CuriosApi.getSlot(id, clientSide).orElse(null);
    String mappingId = targetSlot == null ? id : targetSlot.getMappingIdentifier();
    Map<String, ISlotType> map;

    if (entity != null) {
      map = getItemStackSlots(stack, entity);
    } else {
      map = getItemStackSlots(stack, CuriosFabricConstants.IS_CLIENT);
    }
    Set<String> slots = map.keySet();

    if (!slots.isEmpty()) {
      boolean matchingInstance = slots.stream().anyMatch(slot ->
          CuriosApi.getSlot(slot, clientSide)
              .map(type -> type.getMappingIdentifier().equals(mappingId)).orElse(false));
      return mappingId.equals("curio") || matchingInstance || slots.contains("curio");
    } else if (mappingId.equals("curio")) {
      // If there are no slots available to confirm validity for the generic curio slot,
      // perform fallback checks

      // tags
      if (stack.getTags()
          .anyMatch(tagKey -> CuriosFabricConstants.isCuriosNamespace(
              tagKey.location().getNamespace()))) {
        return true;
      }

      // predicates
      Map<String, ISlotType> allSlots = CuriosApi.getSlots(false);
      SlotResult slotResult = new SlotResult(slotContext, stack);

      for (Map.Entry<String, ISlotType> entry : allSlots.entrySet()) {
        ISlotType slotType = entry.getValue();

        for (ResourceLocation validator : slotType.getValidators()) {

          if (CuriosApi.getCurioPredicate(validator).map(val -> val.test(slotResult))
              .orElse(false)) {
            return true;
          }
        }
      }

      // capability
      return CuriosApi.getCurio(stack).isPresent();
    }
    return false;
  }

  public static Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
      SlotContext slotContext, ResourceLocation id, ItemStack stack) {
    Multimap<Holder<Attribute>, AttributeModifier> multimap = LinkedHashMultimap.create();
    CurioAttributeModifiers attributemodifiers =
        stack.getOrDefault(CuriosFabricRegistry.CURIO_ATTRIBUTE_MODIFIERS, CurioAttributeModifiers.EMPTY);

    if (!attributemodifiers.modifiers().isEmpty()) {

      for (CurioAttributeModifiers.Entry modifier : attributemodifiers.modifiers()) {

        if (modifier.slot().equals(slotContext.identifier())) {
          ResourceLocation rl = modifier.attribute();
          AttributeModifier attributeModifier = modifier.modifier();

          if (rl != null) {
            AttributeModifier.Operation operation = attributeModifier.operation();
            double amount = attributeModifier.amount();

            if (CuriosFabricConstants.isCuriosNamespace(rl.getNamespace())) {
              String identifier1 = rl.getPath();
              LivingEntity livingEntity = slotContext.entity();
              boolean clientSide = livingEntity == null || livingEntity.level().isClientSide();

              if (CuriosApi.getSlot(identifier1, clientSide).isPresent()) {
                CuriosApi.addSlotModifier(multimap, identifier1, id, amount, operation);
              }
            } else {
              Holder<Attribute> attribute =
                  BuiltInRegistries.ATTRIBUTE.getHolder(rl).orElse(null);

              if (attribute != null) {
                multimap.put(attribute, new AttributeModifier(id, amount, operation));
              }
            }
          }
        }
      }
    } else {
      multimap = getCurio(stack).map(curio -> curio.getAttributeModifiers(slotContext, id))
          .orElse(multimap);
    }
    CurioAttributeModifierEvent evt =
        new CurioAttributeModifierEvent(stack, slotContext, id, multimap);
    CuriosEventBus.post(evt);
    return LinkedHashMultimap.create(evt.getModifiers());
  }

  public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map,
                                     String identifier, ResourceLocation id, double amount,
                                     AttributeModifier.Operation operation) {
    map.put(SlotAttribute.getOrCreate(identifier),
        new AttributeModifier(id, amount, operation));
  }

  public static void addSlotModifier(ItemStack stack, String identifier, ResourceLocation id,
                                     double amount, AttributeModifier.Operation operation,
                                     String slot) {
    addModifier(stack, SlotAttribute.getOrCreate(identifier), id, amount, operation, slot);
  }

  public static void addModifier(ItemStack stack, Holder<Attribute> attribute, ResourceLocation id,
                                 double amount, AttributeModifier.Operation operation,
                                 String slot) {
    ResourceLocation rl;

    if (attribute.value() instanceof SlotAttribute wrapper) {
      rl = ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, wrapper.getIdentifier());
    } else {
      rl = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
    }

    CurioAttributeModifiers.Entry entry =
        new CurioAttributeModifiers.Entry(rl, new AttributeModifier(id, amount, operation), slot);
    CurioAttributeModifiers curioAttributeModifiers =
        stack.getOrDefault(CuriosFabricRegistry.CURIO_ATTRIBUTE_MODIFIERS, CurioAttributeModifiers.EMPTY);
    List<CurioAttributeModifiers.Entry> list = new ArrayList<>(curioAttributeModifiers.modifiers());
    list.add(entry);
    stack.set(CuriosFabricRegistry.CURIO_ATTRIBUTE_MODIFIERS,
        new CurioAttributeModifiers(list, curioAttributeModifiers.showInTooltip()));
  }

  public static void broadcastCurioBreakEvent(SlotContext slotContext) {
    LivingEntity livingEntity = slotContext.entity();

    if (livingEntity != null) {
      NetworkHandler.sendToTrackingAndSelf(livingEntity,
          new SPacketBreak(livingEntity.getId(), slotContext.identifier(), slotContext.index()));
    }
  }

  private static final Map<String, UUID> UUIDS = new HashMap<>();

  public static ResourceLocation getSlotId(SlotContext slotContext) {
    String key = slotContext.identifier() + slotContext.index();
    return ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, key);
  }


  private static final Map<ResourceLocation, Predicate<SlotResult>> SLOT_RESULT_PREDICATES =
      new HashMap<>();

  public static void registerCurioPredicate(ResourceLocation resourceLocation,
                                            Predicate<SlotResult> validator) {
    SLOT_RESULT_PREDICATES.putIfAbsent(resourceLocation, validator);
  }

  public static Optional<Predicate<SlotResult>> getCurioPredicate(
      ResourceLocation resourceLocation) {
    return Optional.ofNullable(SLOT_RESULT_PREDICATES.get(resourceLocation));
  }

  public static Map<ResourceLocation, Predicate<SlotResult>> getCurioPredicates() {
    return ImmutableMap.copyOf(SLOT_RESULT_PREDICATES);
  }

  public static boolean testCurioPredicates(Set<ResourceLocation> predicates,
                                            SlotResult slotResult) {

    for (ResourceLocation id : predicates) {

      if (CuriosApi.getCurioPredicate(id).map(
          slotResultPredicate -> slotResultPredicate.test(slotResult)).orElse(false)) {
        return true;
      }
    }
    return false;
  }

  static {
    // Our own namespace. Slot data written for the original Curios uses the neoforge ids, which the
    // compatibility layer also accepts, so both spellings resolve to the same predicate.
    for (String namespace : new String[]{CuriosFabricConstants.MOD_ID,
        CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE}) {
      registerCurioPredicate(ResourceLocation.fromNamespaceAndPath(namespace, "all"),
          (slotResult) -> true);
      registerCurioPredicate(ResourceLocation.fromNamespaceAndPath(namespace, "none"),
          (slotResult) -> false);
      registerCurioPredicate(ResourceLocation.fromNamespaceAndPath(namespace, "tag"),
          (slotResult) -> {
            String id = slotResult.slotContext().identifier();
            ItemStack stack = slotResult.stack();

            if (stack.is(CuriosTags.createItemTag(id)) || stack.is(CuriosTags.CURIO)) {
              return true;
            }
            // Items that a Neoforge mod assigned through the old "#curios:<slot>" tag.
            return NeoforgeCuriosCompat.isLoaded()
                && (stack.is(CuriosTags.neoforgeItemTag(id))
                || stack.is(CuriosTags.neoforgeItemTag("curio")));
          });
    }
  }
}
