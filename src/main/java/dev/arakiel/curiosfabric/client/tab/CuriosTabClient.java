package dev.arakiel.curiosfabric.client.tab;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.client.CuriosFabricClientConfig;
import dev.arakiel.curiosfabric.client.gui.CuriosGuiTextures;
import dev.arakiel.curiosfabric.client.gui.CuriosScreenNavigator;
import dev.arakiel.curiosfabric.client.gui.CuriosTabScreen;
import dev.arakiel.gemini.tab.GeminiTabs;
import dev.arakiel.gemini.tab.TabDefinition;
import dev.arakiel.gemini.tab.TabIcon;
import dev.arakiel.gemini.tab.TabOrder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Registers the Curios entry in Gemini's shared inventory tab bar. */
public final class CuriosTabClient {

  private CuriosTabClient() {
  }

  public static void register() {
    GeminiTabs.registry().register(TabDefinition.builder(
            ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "curios"))
        .owner(CuriosFabricConstants.MOD_ID)
        .title(Component.translatable("menu.tabs.curios"))
        .icon(TabIcon.sprite(CuriosGuiTextures.TAB_ICON))
        .order(TabOrder.anchored(2000))
        .visibleWhen(context -> context.player() != null
            && CuriosFabricClientConfig.CLIENT.value().enableTab)
        .selectedWhen(context -> context.screen() instanceof CuriosTabScreen)
        .action(context -> {
          if (!(context.screen() instanceof CuriosTabScreen)
              && context.player() != null) {
            CuriosScreenNavigator.openTab(context.player(), 0);
          }
        })
        .build());
  }
}
