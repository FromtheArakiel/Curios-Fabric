package dev.arakiel.curiosfabric.common.integration.jei;

import java.util.List;
import javax.annotation.Nonnull;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;
import dev.arakiel.curiosfabric.client.gui.CuriosScreen;
import dev.arakiel.curiosfabric.common.integration.CuriosExclusionAreas;

public class CuriosContainerHandler implements IGuiContainerHandler<CuriosScreen> {

  @Override
  @Nonnull
  public List<Rect2i> getGuiExtraAreas(@Nonnull CuriosScreen screen) {
    return CuriosExclusionAreas.create(screen);
  }
}

