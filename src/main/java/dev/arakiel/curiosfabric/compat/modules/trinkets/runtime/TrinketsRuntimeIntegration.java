package dev.arakiel.curiosfabric.compat.modules.trinkets.runtime;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
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
}
