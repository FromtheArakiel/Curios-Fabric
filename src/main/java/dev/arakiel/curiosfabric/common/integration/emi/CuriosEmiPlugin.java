package dev.arakiel.curiosfabric.common.integration.emi;

import dev.arakiel.curiosfabric.client.gui.CuriosScreen;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.arakiel.gemini.integration.GeminiIntegrationType;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/** Registers Curios panel exclusions with EMI. */
@EmiEntrypoint
@GeminiIntegration(value = "emi", type = GeminiIntegrationType.CLIENT)
public class CuriosEmiPlugin implements EmiPlugin {

  @Override
  public void register(EmiRegistry registry) {
    registry.addExclusionArea(
        CuriosScreen.class, CuriosEmiIntegration::addExclusionAreas);
  }
}
