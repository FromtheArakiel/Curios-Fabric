package dev.arakiel.curiosfabric.client.gui;

import dev.arakiel.curiosfabric.common.inventory.container.CuriosTabLayout;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Resource-backed Curios list page button with persistent enabled and disabled states. */
public final class CuriosPageButton extends Button {

  private final boolean next;
  private final Component pageTooltip;
  private boolean available;

  public CuriosPageButton(int x, int y, boolean next, Component pageTooltip, OnPress onPress) {
    super(x, y, CuriosTabLayout.PAGE_BUTTON_WIDTH, CuriosTabLayout.PAGE_BUTTON_HEIGHT,
        Component.literal(next ? ">" : "<"), onPress, DEFAULT_NARRATION);
    this.next = next;
    this.pageTooltip = pageTooltip;
  }

  public void setAvailable(boolean available) {
    this.available = available;
    this.active = available;
  }

  @Override
  protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY,
                              float partialTick) {
    graphics.blit(
        CuriosGuiTextures.pageButton(next, isHoveredOrFocused(), available),
        getX(), getY(), 0, 0, width, height, width, height);
    if (isHovered()) {
      graphics.renderTooltip(
          net.minecraft.client.Minecraft.getInstance().font, pageTooltip, mouseX, mouseY);
    }
  }
}
