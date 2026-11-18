package dev.arakiel.curiosfabric.compat.modules.accessories.discovery;

/** Accessories slot translation-key rules. */
public final class AccessoriesTranslation {

  private AccessoriesTranslation() {
  }

  public static String fallbackKey(String upstreamName) {
    if (upstreamName == null || upstreamName.isBlank()) {
      return "accessories.slot.unknown";
    }
    return "accessories.slot." + upstreamName.replace(":", ".");
  }
}
