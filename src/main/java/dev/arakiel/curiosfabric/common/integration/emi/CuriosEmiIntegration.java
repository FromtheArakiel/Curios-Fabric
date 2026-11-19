package dev.arakiel.curiosfabric.common.integration.emi;

import dev.arakiel.curiosfabric.client.gui.CuriosScreen;
import dev.arakiel.curiosfabric.common.integration.CuriosExclusionAreas;
import dev.emi.emi.api.widget.Bounds;
import java.util.function.Consumer;
import net.minecraft.client.renderer.Rect2i;

/** EMI-specific conversion of Curios layout exclusions. */
public final class CuriosEmiIntegration {

  private CuriosEmiIntegration() {
  }

  public static void addExclusionAreas(
      CuriosScreen screen, Consumer<Bounds> consumer) {
    for (Rect2i area : CuriosExclusionAreas.create(screen)) {
      consumer.accept(new Bounds(
          area.getX(), area.getY(), area.getWidth(), area.getHeight()));
    }
  }
}
