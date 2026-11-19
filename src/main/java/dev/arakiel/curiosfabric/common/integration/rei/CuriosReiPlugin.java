package dev.arakiel.curiosfabric.common.integration.rei;

import dev.arakiel.curiosfabric.client.gui.CuriosScreen;
import dev.arakiel.curiosfabric.common.integration.CuriosExclusionAreas;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.arakiel.gemini.integration.GeminiIntegrationType;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.minecraft.client.renderer.Rect2i;

/** Registers Curios panel exclusions with REI. */
@GeminiIntegration(value = "roughlyenoughitems", type = GeminiIntegrationType.CLIENT)
public class CuriosReiPlugin implements REIClientPlugin {

  @Override
  public void registerExclusionZones(ExclusionZones zones) {
    zones.register(CuriosScreen.class, screen -> {
      List<Rectangle> areas = new ArrayList<>();
      for (Rect2i area : CuriosExclusionAreas.create(screen)) {
        areas.add(new Rectangle(
            area.getX(), area.getY(), area.getWidth(), area.getHeight()));
      }
      return areas;
    });
  }
}
