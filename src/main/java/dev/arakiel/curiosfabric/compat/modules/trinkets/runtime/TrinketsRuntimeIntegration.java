package dev.arakiel.curiosfabric.compat.modules.trinkets.runtime;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotResult;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.compat.modules.trinkets.TrinketsCompat;
import dev.arakiel.curiosfabric.compat.modules.trinkets.api.TrinketsApiFacade;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/** Strongly typed Trinkets runtime objects behind the optional compatibility boundary. */
@GeminiIntegration("trinkets")
public final class TrinketsRuntimeIntegration {

  private static volatile boolean active;
  private static final Set<ResourceLocation> REGISTERED_PREDICATES =
      ConcurrentHashMap.newKeySet();

  public void apply() {
    active = true;
  }

  @Nullable
  public static SlotReference slotReference(
      String curiosSlot, int index, @Nullable LivingEntity entity, ItemStack stack) {
    if (!active || entity == null) {
      return null;
    }
    TrinketComponent component = TrinketsApi.getTrinketComponent(entity).orElse(null);
    if (component == null) {
      return null;
    }
    boolean client = entity.level() != null && entity.level().isClientSide();
    String path = TrinketsCompat.trinketsPathOf(curiosSlot, client);
    SlotType slotType = path == null ? null : TrinketsCompat.apiSlotTypeForPath(path, client);
    if (slotType == null) {
      int separator = path == null ? -1 : path.indexOf('/');
      String group = separator < 1 ? curiosSlot : path.substring(0, separator);
      String name = separator < 1 ? curiosSlot : path.substring(separator + 1);
      slotType = new SlotType(
          group,
          name,
          0,
          Math.max(1, index + 1),
          ResourceLocation.fromNamespaceAndPath(
              CuriosFabricConstants.MOD_ID, "slot/empty_curio_slot"),
          Set.of(),
          Set.of(),
          Set.of(),
          TrinketEnums.DropRule.DEFAULT);
    } else {
      slotType = withSize(slotType, Math.max(slotType.getAmount(), index + 1));
    }
    TrinketInventory inventory =
        new TrinketInventory(slotType, component, ignored -> {
        });
    if (stack != null && !stack.isEmpty()) {
      inventory.setItem(index, stack);
    }
    return new SlotReference(inventory, index);
  }

  /**
   * Builds the live read view expected by Trinkets API consumers.
   *
   * <p>The Trinkets screen itself is disabled, but third-party mods legitimately read
   * {@code getInventory()} to inspect worn items. A flat Curios inventory is exposed under the
   * corresponding Trinkets group and slot names instead of returning a placeholder empty map.</p>
   */
  public static Map<String, Map<String, TrinketInventory>> inventoryView(
      ICuriosItemHandler ours, TrinketComponent component) {
    Map<String, Map<String, TrinketInventory>> result = new LinkedHashMap<>();
    if (!active || ours == null || component == null || ours.getWearer() == null) {
      return result;
    }
    boolean client = ours.getWearer().level() != null
        && ours.getWearer().level().isClientSide();
    for (Map.Entry<String, ICurioStacksHandler> entry : ours.getCurios().entrySet()) {
      String identifier = entry.getKey();
      String path = TrinketsCompat.trinketsPathOf(identifier, client);
      if (path == null) {
        continue;
      }
      SlotType slotType = TrinketsCompat.apiSlotTypeForPath(path, client);
      if (slotType == null) {
        continue;
      }
      ICurioStacksHandler stacksHandler = entry.getValue();
      IDynamicStackHandler stacks = stacksHandler.getStacks();
      int separator = path.indexOf('/');
      String group = path.substring(0, separator);
      String name = path.substring(separator + 1);
      int size = Math.max(1, stacks.getSlots());
      slotType = withSize(slotType, size);
      TrinketInventory inventory =
          new BridgedTrinketInventory(slotType, component, stacksHandler);
      result.computeIfAbsent(group, ignored -> new LinkedHashMap<>()).put(name, inventory);
    }
    return result;
  }

  private static SlotType withSize(SlotType source, int size) {
    if (source.getAmount() == size) {
      return source;
    }
    return new SlotType(
        source.getGroup(),
        source.getName(),
        source.getOrder(),
        size,
        source.getIcon(),
        source.getQuickMovePredicates(),
        source.getValidatorPredicates(),
        source.getTooltipPredicates(),
        source.getDropRule());
  }

  /**
   * Registers Curios predicate delegates for custom Trinkets slot predicates.
   *
   * <p>Trinkets exposes arbitrary predicate ids in slot JSON and mod registration. Mapping only
   * the four built-in ids to Curios tags silently accepts or rejects the wrong items.</p>
   */
  public static void registerPredicates(Iterable<ResourceLocation> predicates) {
    for (ResourceLocation id : predicates) {
      if (REGISTERED_PREDICATES.contains(id)) {
        continue;
      }
      if (TrinketsApi.getTrinketPredicate(id).isEmpty()) {
        continue;
      }
      REGISTERED_PREDICATES.add(id);
      CuriosApi.registerCurioPredicate(id, result -> evaluatePredicate(id, result));
    }
  }

  private static boolean evaluatePredicate(ResourceLocation id, SlotResult result) {
    var predicate = TrinketsApi.getTrinketPredicate(id).orElse(null);
    if (predicate == null || result.slotContext().entity() == null) {
      return false;
    }
    Object raw = TrinketsApiFacade.slotReference(
        result.slotContext().identifier(),
        result.slotContext().index(),
        result.slotContext().entity(),
        result.stack());
    if (!(raw instanceof SlotReference reference)) {
      return false;
    }
    try {
      TriState state = predicate.apply(result.stack(), reference,
          result.slotContext().entity());
      return state.get();
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug(
          "Bridged Trinkets slot predicate {} failed", id, throwable);
      return false;
    }
  }

  /**
   * Writable Trinkets inventory view backed directly by a Curios stack handler.
   *
   * <p>Returning a filled copy made reads look correct while replacing a stack through the
   * Trinkets API silently did nothing. Container writes must travel through the Curios handler so
   * normal change detection, synchronization and lifecycle hooks remain authoritative.</p>
   */
  private static final class BridgedTrinketInventory extends TrinketInventory {

    private final IDynamicStackHandler stacks;
    private final ICurioStacksHandler handler;

    private BridgedTrinketInventory(
        SlotType slotType, TrinketComponent component, ICurioStacksHandler handler) {
      super(slotType, component, ignored -> {
      });
      this.handler = handler;
      this.stacks = handler.getStacks();
    }

    @Override
    public int getContainerSize() {
      return stacks.getSlots();
    }

    @Override
    public boolean isEmpty() {
      for (int index = 0; index < stacks.getSlots(); index++) {
        if (!stacks.getStackInSlot(index).isEmpty()) {
          return false;
        }
      }
      return true;
    }

    @Override
    public ItemStack getItem(int slot) {
      return stacks.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
      return stacks.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
      ItemStack stack = stacks.getStackInSlot(slot);
      return stack.isEmpty() ? ItemStack.EMPTY : stacks.extractItem(slot, stack.getCount(), false);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
      stacks.setStackInSlot(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
      return 1;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public Map<ResourceLocation, AttributeModifier> getModifiers() {
      return handler.getModifiers();
    }

    @Override
    public Collection<AttributeModifier> getModifiersByOperation(
        AttributeModifier.Operation operation) {
      return handler.getModifiersByOperation(operation);
    }

    @Override
    public void addModifier(AttributeModifier modifier) {
      handler.addTransientModifier(modifier);
    }

    @Override
    public void addPersistentModifier(AttributeModifier modifier) {
      handler.addPermanentModifier(modifier);
    }

    @Override
    public void removeModifier(ResourceLocation id) {
      handler.removeModifier(id);
    }

    @Override
    public void clearModifiers() {
      handler.clearModifiers();
    }

    @Override
    public void clearCachedModifiers() {
      handler.clearCachedModifiers();
    }
  }
}
