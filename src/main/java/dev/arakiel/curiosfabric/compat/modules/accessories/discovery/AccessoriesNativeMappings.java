package dev.arakiel.curiosfabric.compat.modules.accessories.discovery;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Fixed semantic mappings for the slots owned by Accessories itself. */
public final class AccessoriesNativeMappings {

  private static final Map<String, String> NATIVE = Map.ofEntries(
      Map.entry("hat", "head"),
      Map.entry("face", "head"),
      Map.entry("necklace", "necklace"),
      Map.entry("cape", "back"),
      Map.entry("back", "back"),
      Map.entry("hand", "hands"),
      Map.entry("ring", "ring"),
      Map.entry("wrist", "bracelet"),
      Map.entry("belt", "belt"),
      Map.entry("charm", "charm"));

  private AccessoriesNativeMappings() {
  }

  public static String nativeId(String upstreamId) {
    return upstreamId == null ? null : NATIVE.get(upstreamId);
  }

  public static boolean isNative(String upstreamId) {
    return nativeId(upstreamId) != null;
  }

  public static String mappingId(String upstreamId, Set<String> claimedIds) {
    String nativeId = nativeId(upstreamId);
    return nativeId != null
        ? nativeId : AccessoriesSlotIdentity.uniqueId("accessories_", upstreamId, claimedIds);
  }

  public static Set<String> builtInNames() {
    return new LinkedHashSet<>(Set.of(
        "hat", "face", "necklace", "cape", "back",
        "hand", "ring", "wrist", "belt", "charm", "shoes", "anklet"));
  }
}
