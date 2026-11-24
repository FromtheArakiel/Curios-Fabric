package dev.arakiel.curiosfabric.client.gui;

import dev.arakiel.curiosfabric.common.inventory.container.CuriosTabMenu;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Gemini's automatic Inventory tab only swaps the local screen. Curios menus are server-driven,
 * so the old menu must be closed through the same carried-stack-safe packet used by CuriosButton.
 */
public final class CuriosTabNavigationEvents {

  private CuriosTabNavigationEvents() {
  }

  public static void register() {
    ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
      if (!(screen instanceof InventoryScreen) || client.player == null) {
        return;
      }
      if (client.player.containerMenu instanceof CuriosTabMenu) {
        CuriosScreenNavigator.handoffToVanilla(client.player);
      }
    });
  }
}
