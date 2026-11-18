package dev.arakiel.curiosfabric.compat.modules.trinkets.discovery;

import java.util.Locale;
import java.util.Map;

/** Semantic mappings for slot paths owned by Trinkets itself. */
public final class TrinketsNativeMappings {

  private static final Map<String, String> NATIVE = Map.ofEntries(
      Map.entry("head/hat", "head"),
      Map.entry("head/face", "head"),
      Map.entry("chest/necklace", "necklace"),
      Map.entry("chest/back", "back"),
      Map.entry("chest/cape", "back"),
      Map.entry("hand/ring", "ring"),
      Map.entry("offhand/ring", "ring"),
      Map.entry("hand/glove", "hands"),
      Map.entry("offhand/glove", "hands"),
      Map.entry("legs/belt", "belt"));

  private TrinketsNativeMappings() {
  }

  public static String nativeId(String path) {
    return NATIVE.get(path);
  }

  public static boolean isNative(String path) {
    return NATIVE.containsKey(path);
  }

  public static String appendedMappingId(String path) {
    int separator = path.indexOf('/');
    String slot = separator < 0 ? path : path.substring(separator + 1);
    String normalized = slot.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9_]", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_+|_+$", "");
    if (normalized.isEmpty()) {
      normalized = "slot";
    }
    return "trinkets_" + normalized;
  }
}
