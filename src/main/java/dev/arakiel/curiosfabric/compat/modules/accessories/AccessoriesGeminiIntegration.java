package dev.arakiel.curiosfabric.compat.modules.accessories;

import dev.arakiel.gemini.integration.GeminiIntegration;

/** Optional Gemini integration marker for Accessories. */
@GeminiIntegration(AccessoriesCompat.MOD_ID)
public final class AccessoriesGeminiIntegration {

  public void apply() {
    // Presence is the integration contract; the bridge remains lazily initialized.
  }
}
