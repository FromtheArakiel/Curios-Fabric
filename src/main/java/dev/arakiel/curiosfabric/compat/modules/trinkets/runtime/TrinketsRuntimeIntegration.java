package dev.arakiel.curiosfabric.compat.modules.trinkets.runtime;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.compat.modules.trinkets.TrinketsCompat;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Map;

/** Strongly typed Trinkets runtime objects behind the optional compatibility boundary. */
@GeminiIntegration("trinkets")
public final class TrinketsRuntimeIntegration {

  private static volatile boolean active;

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
    int separator = path == null ? -1 : path.indexOf('/');
    String group = separator < 1 ? curiosSlot : path.substring(0, separator);
    String name = separator < 1 ? curiosSlot : path.substring(separator + 1);
    int size = Math.max(1, index + 1);
    SlotType slotType = new SlotType(
        group,
        name,
        0,
        size,
        ResourceLocation.fromNamespaceAndPath(
            CuriosFabricConstants.MOD_ID, "slot/empty_curio_slot"),
        Set.of(),
        Set.of(),
        Set.of(),
        TrinketEnums.DropRule.DEFAULT);
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
      int separator = path.indexOf('/');
      if (separator < 1 || separator == path.length() - 1) {
        continue;
      }
      String group = path.substring(0, separator);
      String name = path.substring(separator + 1);
      ICurioStacksHandler stacksHandler = entry.getValue();
      IDynamicStackHandler stacks = stacksHandler.getStacks();
      ISlotType registered = CuriosApi.getSlot(identifier, client).orElse(null);
      int size = Math.max(1, stacks.getSlots());
      SlotType slotType = new SlotType(
          group,
          name,
          registered == null ? 0 : registered.getOrder().orElse(0),
          size,
          registered == null
              ? ResourceLocation.fromNamespaceAndPath(
                  CuriosFabricConstants.MOD_ID, "slot/empty_curio_slot")
              : registered.getIcon(),
          Set.of(),
          Set.of(),
          Set.of(),
          TrinketEnums.DropRule.DEFAULT);
      TrinketInventory inventory = new TrinketInventory(
          slotType, component, ignored -> {
          });
      for (int index = 0; index < size; index++) {
        inventory.setItem(index, stacks.getStackInSlot(index));
      }
      result.computeIfAbsent(group, ignored -> new LinkedHashMap<>()).put(name, inventory);
    }
    return result;
  }
}
