package dev.arakiel.curiosfabric.client.gui;

import dev.arakiel.curiosfabric.common.network.NetworkHandler;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenCuriosTab;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenVanilla;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared carried-stack-safe transitions between Curios and vanilla inventory screens. */
public final class CuriosScreenNavigator {

  private CuriosScreenNavigator() {
  }

  public static void openTab(Player player, int page) {
    ItemStack carried = player.containerMenu.getCarried();
    player.containerMenu.setCarried(ItemStack.EMPTY);
    NetworkHandler.sendToServer(new CPacketOpenCuriosTab(Math.max(0, page)));
  }

  public static void handoffToVanilla(Player player) {
    ItemStack carried = player.containerMenu.getCarried();
    player.containerMenu.setCarried(ItemStack.EMPTY);
    NetworkHandler.sendToServer(new CPacketOpenVanilla(carried));
  }
}
