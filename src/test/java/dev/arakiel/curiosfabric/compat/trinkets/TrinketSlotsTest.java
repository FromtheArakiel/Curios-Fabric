package dev.arakiel.curiosfabric.compat.trinkets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrinketSlotsTest {

  @Test
  void standardTrinketsSlotsIncludeFaceAndOffhandGlove() {
    assertEquals("head", TrinketSlots.toCurios("head/face"));
    assertEquals("hands", TrinketSlots.toCurios("offhand/glove"));
  }

  @Test
  void unknownThirdPartySlotsAreNotSilentlyAssignedToAStandardSlot() {
    assertEquals(null, TrinketSlots.toCurios("example/custom_slot"));
  }

  @Test
  void thirdPartySlotPathsProduceStableNamespacedCuriosIds() {
    assertEquals("trinkets_example_custom_slot",
        TrinketsCompat.generatedSlotId("example/custom_slot"));
    assertEquals("trinkets_example_custom_slot",
        TrinketsCompat.generatedSlotId("Example//Custom Slot"));
  }
}
