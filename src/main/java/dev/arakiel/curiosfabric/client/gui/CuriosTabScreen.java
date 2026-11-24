package dev.arakiel.curiosfabric.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.arakiel.curiosfabric.client.KeyRegistry;
import dev.arakiel.curiosfabric.common.inventory.CurioSlot;
import dev.arakiel.curiosfabric.common.inventory.container.CuriosTabLayout;
import dev.arakiel.curiosfabric.common.inventory.container.CuriosTabMenu;
import dev.arakiel.curiosfabric.common.network.NetworkHandler;
import dev.arakiel.curiosfabric.common.network.client.CPacketToggleRender;
import dev.arakiel.gemini.tab.TabScreen;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** L2Tabs-style independent Curios slot list. */
public class CuriosTabScreen extends AbstractContainerScreen<CuriosTabMenu>
    implements TabScreen {

  private final List<RenderButton> renderButtons = new ArrayList<>();
  private boolean isRenderButtonHovered;

  public CuriosTabScreen(CuriosTabMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    this.imageWidth = CuriosTabLayout.IMAGE_WIDTH;
    this.imageHeight = CuriosTabLayout.imageHeight(menu.rows());
    this.inventoryLabelY = CuriosTabLayout.playerInventoryLabelY(menu.rows());
  }

  @Override
  protected void init() {
    super.init();
    if (topPos < 28) {
      topPos = 28;
    }
    int buttonY = topPos + CuriosTabLayout.PAGE_BUTTON_Y;
    int previousButtonX =
        leftPos + CuriosTabLayout.previousPageButtonX(font.width(title));
    int nextButtonX =
        leftPos + CuriosTabLayout.nextPageButtonX(font.width(title));
    if (menu.currentPage() > 0) {
      addRenderableWidget(Button.builder(Component.literal("<"),
              button -> CuriosScreenNavigator.openTab(minecraft.player, menu.currentPage() - 1))
          .pos(previousButtonX, buttonY)
          .size(CuriosTabLayout.PAGE_BUTTON_WIDTH, CuriosTabLayout.PAGE_BUTTON_HEIGHT)
          .build());
    }
    if (menu.currentPage() < menu.totalPages() - 1) {
      addRenderableWidget(Button.builder(Component.literal(">"),
              button -> CuriosScreenNavigator.openTab(minecraft.player, menu.currentPage() + 1))
          .pos(nextButtonX, buttonY)
          .size(CuriosTabLayout.PAGE_BUTTON_WIDTH, CuriosTabLayout.PAGE_BUTTON_HEIGHT)
          .build());
    }

    renderButtons.clear();
    for (Slot slot : menu.slots) {
      if (slot instanceof CurioSlot curioSlot && curioSlot.canToggleRender()) {
        RenderButton renderButton = new RenderButton(
            curioSlot,
            leftPos + slot.x + 12,
            topPos + slot.y - 1,
            8,
            8,
            75,
            0,
            CuriosGuiTextures.INVENTORY,
            button -> NetworkHandler.sendToServer(new CPacketToggleRender(
                curioSlot.getIdentifier(), curioSlot.getSlotIndex())));
        renderButtons.add(renderButton);
        addRenderableWidget(renderButton);
      }
    }
  }

  @Override
  public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    boolean hoveredButton = false;
    for (RenderButton renderButton : renderButtons) {
      if (renderButton.isHovered()) {
        hoveredButton = true;
        break;
      }
    }
    isRenderButtonHovered = false;
    super.render(graphics, mouseX, mouseY, partialTick);
    for (RenderButton renderButton : renderButtons) {
      renderButton.renderButtonOverlay(graphics, mouseX, mouseY, partialTick);
    }
    isRenderButtonHovered = hoveredButton;
    if (hoveredButton) {
      renderTooltip(graphics, mouseX, mouseY);
    }
  }

  @Override
  protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick,
                          int mouseX, int mouseY) {
    RenderSystem.enableBlend();
    var background = CuriosGuiTextures.tabBackground(menu.rows());
    graphics.blit(background,
        leftPos, topPos, 0, 0, imageWidth, imageHeight);
    for (Slot slot : menu.slots) {
      if (slot instanceof CurioSlot) {
        graphics.blit(background,
            leftPos + slot.x - 1, topPos + slot.y - 1, 176, 0, 18, 18);
      }
    }
    RenderSystem.disableBlend();
  }

  @Override
  protected void renderSlot(@Nonnull GuiGraphics graphics, @Nonnull Slot slot) {
    if (slot instanceof CurioSlot && slot.getItem().isEmpty() && minecraft != null) {
      CurioSlotIcons.render(graphics, minecraft, slot);
      return;
    }
    super.renderSlot(graphics, slot);
  }

  @Override
  protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
    super.renderLabels(graphics, mouseX, mouseY);
  }

  @Override
  protected void renderTooltip(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
    if (isRenderButtonHovered) {
      graphics.renderTooltip(
          font, Component.translatable("gui.curiosfabric.toggle"), mouseX, mouseY);
      return;
    }
    Slot hovered = findCurioSlotAt(mouseX, mouseY);
    if (hovered == null) {
      hovered = hoveredSlot;
    }
    if (hovered instanceof CurioSlot curioSlot) {
      ItemStack stack = curioSlot.getSlotExtension()
          .getDisplayStack(curioSlot.getSlotContext(), hovered.getItem());
      if (stack.isEmpty()) {
        List<Component> tooltip = new java.util.ArrayList<>(
            curioSlot.getSlotExtension().getSlotTooltip(
                curioSlot.getSlotContext(),
                minecraft != null && minecraft.options.advancedItemTooltips
                    ? TooltipFlag.Default.ADVANCED
                    : TooltipFlag.Default.NORMAL));
        if (tooltip.isEmpty()) {
          tooltip.add(Component.literal(curioSlot.getSlotName()));
        }
        if (!curioSlot.isActiveState()) {
          tooltip.add(Component.translatable("curiosfabric.tooltip.inactive")
              .withStyle(ChatFormatting.RED));
        }
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        return;
      }

      List<Component> tooltip = new java.util.ArrayList<>(
          net.minecraft.client.gui.screens.Screen.getTooltipFromItem(minecraft, stack));
      if (!curioSlot.isActiveState()) {
        tooltip.add(Component.translatable("curiosfabric.tooltip.inactive")
            .withStyle(ChatFormatting.RED));
      }
      graphics.renderTooltip(font, tooltip, stack.getTooltipImage(), mouseX, mouseY);
      return;
    }
    super.renderTooltip(graphics, mouseX, mouseY);
  }

  private Slot findCurioSlotAt(double mouseX, double mouseY) {
    for (Slot slot : menu.slots) {
      if (slot instanceof CurioSlot
          && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
        return slot;
      }
    }
    return null;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (KeyRegistry.openCurios.matches(keyCode, scanCode)) {
      if (minecraft != null && minecraft.player != null) {
        minecraft.player.closeContainer();
      }
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }
}
