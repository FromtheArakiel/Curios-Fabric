package dev.arakiel.curiosfabric.server.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import org.junit.jupiter.api.Test;

class CommandContractTest {

  @Test
  void commandAndModIdUseCuriosFabricNamespace() {
    assertEquals("curiosfabric", CuriosFabricCommand.COMMAND_NAME);
    assertEquals("curiosfabric", CuriosApi.MODID);
    assertEquals("curiosfabric", CuriosFabricConstants.MOD_ID);
  }

  @Test
  void publicPackageNoLongerExposesTheOldCuriosRoot() {
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("dev.arakiel.curios.Curios"));
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("dev.arakiel.curios.server.command.CuriosCommand"));
  }

  @Test
  void neoforgeCompatibilityClassesUseTheNeoforgeCuriosPrefix() throws Exception {
    Class.forName("dev.arakiel.curiosfabric.compat.neoforge.NeoforgeCuriosCompat");
    Class.forName("dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosApiBridge");
    Class.forName("dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosItemAdapter");
    Class.forName("dev.arakiel.curiosfabric.compat.neoforge.NeoforgeCuriosItem");
    Class.forName("dev.arakiel.curiosfabric.common.slottype.NeoforgeCuriosSlotManager");
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("dev.arakiel.curiosfabric.compat.forge.ForgeCuriosCompat"));
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("dev.arakiel.curiosfabric.compat.neoforge.LegacyCurio"));
  }
}
