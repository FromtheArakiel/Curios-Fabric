package dev.arakiel.curiosfabric.common.slottype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SlotTypeTest {

  @Test
  void defaultsMatchCuriosFabricContract() {
    SlotType slot = new SlotType.Builder("ring").build();

    assertEquals("ring", slot.getIdentifier());
    assertEquals(1, slot.getSize());
    assertEquals(1000, slot.getOrder());
    assertTrue(slot.useNativeGui());
    assertFalse(slot.hasCosmetic());
    assertTrue(slot.canToggleRendering());
    assertEquals(ICurio.DropRule.DEFAULT, slot.getDropRule());
    assertEquals(
        ResourceLocation.fromNamespaceAndPath("curiosfabric", "tag"),
        slot.getValidators().iterator().next());
  }

  @Test
  void additiveAndRemovalSizingIsAppliedAfterTheBaseSize() {
    SlotType additive = new SlotType.Builder("ring")
        .size(2)
        .size(3, "ADD")
        .build();
    SlotType removed = new SlotType.Builder("belt")
        .size(5)
        .size(2, "REMOVE")
        .build();

    assertEquals(5, additive.getSize());
    assertEquals(3, removed.getSize());
  }

  @Test
  void replaceUsesTheLastExplicitlyReplacingValue() {
    SlotType slot = new SlotType.Builder("necklace")
        .order(80)
        .order(120, true)
        .size(2)
        .size(4, "SET", true)
        .build();

    assertEquals(120, slot.getOrder());
    assertEquals(4, slot.getSize());
  }

  @Test
  void nbtRoundTripPreservesDefinitions() {
    SlotType original = new SlotType.Builder("charm")
        .order(15)
        .size(3)
        .hasCosmetic(true, true)
        .useNativeGui(false, true)
        .renderToggle(false, true)
        .dropRule(ICurio.DropRule.ALWAYS_KEEP)
        .validator(ResourceLocation.fromNamespaceAndPath("example", "charm"))
        .build();

    ISlotType restored = SlotType.from(original.writeNbt());

    assertEquals(original, restored);
    assertEquals(original.getOrder(), restored.getOrder());
    assertEquals(original.getSize(), restored.getSize());
    assertEquals(original.hasCosmetic(), restored.hasCosmetic());
    assertEquals(original.useNativeGui(), restored.useNativeGui());
    assertEquals(original.canToggleRendering(), restored.canToggleRendering());
    assertEquals(original.getDropRule(), restored.getDropRule());
    assertEquals(original.getValidators(), restored.getValidators());
  }
}
