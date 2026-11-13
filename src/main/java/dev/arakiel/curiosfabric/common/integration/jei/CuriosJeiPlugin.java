package dev.arakiel.curiosfabric.common.integration.jei;

import javax.annotation.Nonnull;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.client.gui.CuriosScreen;

@JeiPlugin
public class CuriosJeiPlugin implements IModPlugin {

  @Override
  @Nonnull
  public ResourceLocation getPluginUid() {
    return ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "jei");
  }

  @Override
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    registration.addGuiContainerHandler(CuriosScreen.class, new CuriosContainerHandler());
  }
}
