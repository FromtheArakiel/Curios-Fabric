package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.discovery;

import java.util.Map;

/** Semantic mappings for slots owned by NeoForge Curios itself. */
public final class NeoforgeCuriosNativeMappings {

  private static final Map<String, String> NATIVE = Map.ofEntries(
      Map.entry("curio", "curio"),
      Map.entry("necklace", "necklace"),
      Map.entry("ring", "ring"),
      Map.entry("head", "head"),
      Map.entry("back", "back"),
      Map.entry("belt", "belt"),
      Map.entry("body", "body"),
      Map.entry("charm", "charm"),
      Map.entry("hands", "hands"),
      Map.entry("bracelet", "bracelet"));

  private NeoforgeCuriosNativeMappings() {
  }

  public static String nativeId(String id) {
    return NATIVE.get(id);
  }
}
