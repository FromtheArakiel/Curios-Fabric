package dev.arakiel.curiosfabric.client.gui;

import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.client.ICuriosScreen;
import dev.arakiel.curiosfabric.common.network.NetworkHandler;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenCurios;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenVanilla;

public class CuriosButton extends ImageButton {

  public static final WidgetSprites BIG =
      new WidgetSprites(ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "button"),
          ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "button_highlighted"));
  public static final WidgetSprites SMALL =
      new WidgetSprites(ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "button_small"),
          ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "button_small_highlighted"));
  private final AbstractContainerScreen<?> parentGui;

  CuriosButton(AbstractContainerScreen<?> parentGui, int xIn, int yIn, int widthIn, int heightIn,
               WidgetSprites sprites) {
    super(xIn, yIn, widthIn, heightIn, sprites,
        (button) -> {
          Minecraft mc = Minecraft.getInstance();

          if (mc.player != null) {
            ItemStack stack = mc.player.containerMenu.getCarried();
            mc.player.containerMenu.setCarried(ItemStack.EMPTY);

            if (parentGui instanceof ICuriosScreen) {
              InventoryScreen inventory = new InventoryScreen(mc.player);
              mc.setScreen(inventory);
              mc.player.containerMenu.setCarried(stack);
              NetworkHandler.sendToServer(new CPacketOpenVanilla(stack));
            } else {

              if (parentGui instanceof InventoryScreen inventory) {
                RecipeBookComponent recipeBookGui = inventory.getRecipeBookComponent();

                if (recipeBookGui.isVisible()) {
                  recipeBookGui.toggleVisibility();
                }
              }
              NetworkHandler.sendToServer(new CPacketOpenCurios(stack));
            }
          }
        });
    this.parentGui = parentGui;
  }

  @Override
  public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY,
                           float partialTicks) {
    Tuple<Integer, Integer> offsets =
        CuriosScreen.getButtonOffset(parentGui instanceof CreativeModeInventoryScreen);
    this.setX(parentGui.leftPos + offsets.getA() + 2);
    int yOffset = parentGui instanceof CreativeModeInventoryScreen ? 70 : 85;
    this.setY(parentGui.topPos + offsets.getB() + yOffset);

    if (parentGui instanceof CreativeModeInventoryScreen gui) {
      boolean isInventoryTab = gui.isInventoryOpen();
      this.active = isInventoryTab;

      if (!isInventoryTab) {
        return;
      }
    }
    super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
  }
}
