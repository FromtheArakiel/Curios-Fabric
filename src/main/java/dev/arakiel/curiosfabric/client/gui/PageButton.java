package dev.arakiel.curiosfabric.client.gui;

import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.common.network.NetworkHandler;
import dev.arakiel.curiosfabric.common.network.client.CPacketPage;

public class PageButton extends Button {

  private final CuriosScreen parentGui;
  private final Type type;
  private static final ResourceLocation CURIO_INVENTORY =
      ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID,
          "textures/gui/curios/inventory.png");

  public PageButton(CuriosScreen parentGui, int xIn, int yIn, int widthIn, int heightIn,
                    Type type) {
    super(xIn, yIn, widthIn, heightIn, CommonComponents.EMPTY,
        (button) -> NetworkHandler.sendToServer(
            new CPacketPage(parentGui.getMenu().containerId, type == Type.NEXT)),
        DEFAULT_NARRATION);
    this.parentGui = parentGui;
    this.type = type;
  }

  @Override
  public void renderWidget(@Nonnull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
    int xText = type == Type.NEXT ? 43 : 32;
    int yText = 25;

    if (type == Type.NEXT) {
        this.setX(this.parentGui.leftPos - 17);
      this.active = this.parentGui.getMenu().currentPage + 1 < this.parentGui.getMenu().totalPages;
    } else {
        this.setX(this.parentGui.leftPos - 28);
      this.active = this.parentGui.getMenu().currentPage > 0;
    }

    if (!this.isActive()) {
      yText += 12;
    } else if (this.isHoveredOrFocused()) {
      xText += 22;
    }

    if (this.isHovered()) {
      guiGraphics.renderTooltip(Minecraft.getInstance().font,
          Component.translatable("gui.curiosfabric.page", this.parentGui.getMenu().currentPage + 1,
              this.parentGui.getMenu().totalPages), x, y);
    }
    guiGraphics.blit(CURIO_INVENTORY, this.getX(), this.getY(), xText, yText, this.width,
        this.height);
  }

  public enum Type {
    NEXT,
    PREVIOUS
  }
}
