package dev.arakiel.curiosfabric.client.gui;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import net.minecraft.resources.ResourceLocation;

/** Shared GUI resources used by the original Curios screen and the tabbed slot list. */
public final class CuriosGuiTextures {

  public static final ResourceLocation INVENTORY = id("textures/gui/curios/inventory.png");
  public static final ResourceLocation TAB_ICON = id("tab/curios");
  private static final ResourceLocation PAGE_PREVIOUS =
      id("textures/gui/curios/page/previous.png");
  private static final ResourceLocation PAGE_PREVIOUS_HOVER =
      id("textures/gui/curios/page/previous_hover.png");
  private static final ResourceLocation PAGE_PREVIOUS_DISABLED =
      id("textures/gui/curios/page/previous_disabled.png");
  private static final ResourceLocation PAGE_NEXT =
      id("textures/gui/curios/page/next.png");
  private static final ResourceLocation PAGE_NEXT_HOVER =
      id("textures/gui/curios/page/next_hover.png");
  private static final ResourceLocation PAGE_NEXT_DISABLED =
      id("textures/gui/curios/page/next_disabled.png");

  private static final ResourceLocation[] TAB_BACKGROUNDS = {
      null,
      null,
      null,
      id("textures/gui/container/curios_tab_3.png"),
      id("textures/gui/container/curios_tab_4.png"),
      id("textures/gui/container/curios_tab_5.png"),
      id("textures/gui/container/curios_tab_6.png")
  };

  private CuriosGuiTextures() {
  }

  public static ResourceLocation tabBackground(int rows) {
    return TAB_BACKGROUNDS[Math.max(3, Math.min(6, rows))];
  }

  public static ResourceLocation pageButton(boolean next, boolean hovered, boolean available) {
    if (!available) {
      return next ? PAGE_NEXT_DISABLED : PAGE_PREVIOUS_DISABLED;
    }
    if (hovered) {
      return next ? PAGE_NEXT_HOVER : PAGE_PREVIOUS_HOVER;
    }
    return next ? PAGE_NEXT : PAGE_PREVIOUS;
  }

  private static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, path);
  }
}
