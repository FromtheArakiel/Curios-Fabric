package dev.arakiel.curiosfabric.common.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.common.slottype.SlotType;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CompatibilitySlotMergeTest {

  @BeforeAll
  static void bootstrapMinecraftRegistries() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  @Test
  void dynamicSlotTypesAreAddedWithoutReplacingDataPackSlots() {
    CuriosSlotManager manager = new CuriosSlotManager();
    ISlotType dataPack =
        new SlotType.Builder("ring").size(4).build();
    manager.mergeSlots(Map.of(dataPack.getIdentifier(), dataPack));
    ISlotType dynamic =
        new SlotType.Builder("trinkets_example_custom").size(2).build();

    manager.mergeSlots(Map.of(dynamic.getIdentifier(), dynamic));

    assertTrue(manager.getSlot("trinkets_example_custom").isPresent());
    assertSame(dataPack, manager.getSlot("ring").orElseThrow());
  }

  @Test
  void compatibilitySlotsDoNotReplaceAnExistingSlotWithTheSameId() {
    CuriosSlotManager manager = new CuriosSlotManager();
    ISlotType existing =
        new SlotType.Builder("trinkets_example_custom").size(4).build();
    ISlotType duplicate =
        new SlotType.Builder("trinkets_example_custom").size(1).build();
    manager.mergeSlots(Map.of(existing.getIdentifier(), existing));

    manager.mergeSlots(Map.of(duplicate.getIdentifier(), duplicate));

    assertSame(existing, manager.getSlot("trinkets_example_custom").orElseThrow());
  }

  @Test
  void dynamicEntityAssignmentsAreMergedIntoTheServerMap() {
    CuriosEntityManager manager = new CuriosEntityManager();
    ISlotType dynamic = new SlotType.Builder("trinkets_example_custom").size(1).build();

    manager.mergeEntitySlots(Map.of(EntityType.PLAYER, Map.of("trinkets_example_custom", dynamic)));

    assertFalse(manager.getEntitySlots(EntityType.PLAYER).isEmpty());
    assertTrue(manager.getEntitySlots(EntityType.PLAYER).containsKey("trinkets_example_custom"));
  }
}
