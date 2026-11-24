package dev.arakiel.curiosfabric.common.inventory.container;

import javax.annotation.Nonnull;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Server-side page-carrying provider for the independent Curios list menu. */
public record CuriosTabMenuProvider(int page) implements ExtendedScreenHandlerFactory<Integer> {

  @Nonnull
  @Override
  public Component getDisplayName() {
    return Component.translatable("menu.tabs.curios");
  }

  @Override
  public Integer getScreenOpeningData(ServerPlayer player) {
    return Math.max(0, page);
  }

  @Nonnull
  @Override
  public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory inventory,
                                          @Nonnull Player player) {
    return new CuriosTabMenu(containerId, inventory, Math.max(0, page));
  }
}
