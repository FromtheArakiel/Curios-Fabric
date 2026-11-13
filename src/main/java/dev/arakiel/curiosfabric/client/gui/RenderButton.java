package dev.arakiel.curiosfabric.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.common.inventory.CurioSlot;

public class RenderButton extends ImageButton {
  public static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled"),
      ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted")
  );
  private final ResourceLocation resourceLocation;
  private final int yTexStart;
  private final int xTexStart;
  private final CurioSlot slot;

  public RenderButton(CurioSlot slot, int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn,
                      int yTexStartIn, ResourceLocation resourceLocationIn,
                      OnPress onPressIn) {
    super(xIn, yIn, widthIn, heightIn, BUTTON_SPRITES, onPressIn);
    this.resourceLocation = resourceLocationIn;
    this.yTexStart = yTexStartIn;
    this.xTexStart = xTexStartIn;
    this.slot = slot;
  }

  @Override
  public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY,
                           float partialTicks) {
  }

  public void renderButtonOverlay(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY,
                                  float partialTicks) {
    RenderSystem.disableDepthTest();
    int j = this.xTexStart;

    if (!slot.getRenderStatus()) {
      j += 8;
    }
    guiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), (float) j,
        (float) this.yTexStart,
        this.width, this.height, 256, 256);
    RenderSystem.enableDepthTest();
  }
}
