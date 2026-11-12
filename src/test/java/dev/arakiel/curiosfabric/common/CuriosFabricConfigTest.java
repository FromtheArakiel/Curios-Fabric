package dev.arakiel.curiosfabric.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.arakiel.curiosfabric.client.CuriosFabricClientConfig;
import dev.arakiel.curiosfabric.client.CuriosFabricClientConfig.ButtonCorner;
import org.junit.jupiter.api.Test;

class CuriosFabricConfigTest {

  @Test
  void commonAndServerDefaultsAreStable() {
    CuriosFabricConfig.Common common = new CuriosFabricConfig.Common();
    CuriosFabricConfig.Server server = new CuriosFabricConfig.Server();

    assertTrue(common.slots.isEmpty());
    assertEquals(CuriosFabricConfig.KeepCurios.DEFAULT, server.keepCurios);
    assertEquals(1, server.minimumColumns);
    assertEquals(48, server.maxSlotsPerPage);
  }

  @Test
  void clientDefaultsAndCornerOffsetsAreStable() {
    CuriosFabricClientConfig.Client client = new CuriosFabricClientConfig.Client();

    assertTrue(client.renderCurios);
    assertTrue(client.enableButton);
    assertEquals(ButtonCorner.TOP_LEFT, client.buttonCorner);
    assertEquals(26, ButtonCorner.TOP_LEFT.getXoffset());
    assertEquals(-75, ButtonCorner.TOP_LEFT.getYoffset());
    assertEquals(73, ButtonCorner.TOP_LEFT.getCreativeXoffset());
    assertEquals(-62, ButtonCorner.TOP_LEFT.getCreativeYoffset());
  }

  @Test
  void keepCuriosHasExactlyTheExpectedModes() {
    assertEquals(3, CuriosFabricConfig.KeepCurios.values().length);
    assertEquals(CuriosFabricConfig.KeepCurios.ON,
        CuriosFabricConfig.KeepCurios.valueOf("ON"));
    assertEquals(CuriosFabricConfig.KeepCurios.OFF,
        CuriosFabricConfig.KeepCurios.valueOf("OFF"));
    assertFalse(CuriosFabricConfig.KeepCurios.DEFAULT.name().isEmpty());
  }
}
