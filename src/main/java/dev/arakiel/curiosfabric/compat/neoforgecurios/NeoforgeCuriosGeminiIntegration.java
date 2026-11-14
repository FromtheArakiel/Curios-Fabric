package dev.arakiel.curiosfabric.compat.neoforgecurios;

import dev.arakiel.gemini.integration.GeminiIntegration;

/** Optional integration marker for NeoForge Curios loaded through Connector. */
@GeminiIntegration(NeoforgeCuriosCompat.NEOFORGE_MOD_ID)
public final class NeoforgeCuriosGeminiIntegration {

  public void apply() {
    // Presence is the contract; the bridge itself remains lazily initialized.
  }
}
