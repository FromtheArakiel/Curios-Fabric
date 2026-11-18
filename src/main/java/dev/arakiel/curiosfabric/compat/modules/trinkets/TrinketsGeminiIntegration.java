package dev.arakiel.curiosfabric.compat.modules.trinkets;

import dev.arakiel.gemini.integration.GeminiIntegration;

/** Optional integration marker used by Gemini before the bridge is first queried. */
@GeminiIntegration(TrinketsCompat.MOD_ID)
public final class TrinketsGeminiIntegration {

  public void apply() {
    // Presence is the contract; the bridge itself remains lazily initialized.
  }
}
