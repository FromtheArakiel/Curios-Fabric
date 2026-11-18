package dev.arakiel.curiosfabric.compat.modules.accessories.discovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Stable CuriosFabric identities for Accessories slot names. */
public final class AccessoriesSlotIdentity {

  private AccessoriesSlotIdentity() {
  }

  public static String uniqueId(String prefix, String upstreamName, Set<String> claimedIds) {
    String base = prefix + normalize(upstreamName);
    if (!claimedIds.contains(base)) {
      return base;
    }
    return base + "_" + hashSuffix(upstreamName);
  }

  public static String normalize(String value) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9_]+", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_+|_+$", "");
    return normalized.isBlank() ? "slot" : normalized;
  }

  public static String normalizeUpstream(String prefix, String upstreamName) {
    return prefix + normalize(upstreamName);
  }

  public static ResourceLocation normalizeIcon(ResourceLocation icon, ResourceLocation fallback) {
    if (icon == null) {
      return fallback;
    }
    String path = icon.getPath();
    if (path.startsWith("textures/")) {
      path = path.substring("textures/".length());
    }
    if (path.endsWith(".png")) {
      path = path.substring(0, path.length() - ".png".length());
    }
    if (path.isBlank()) {
      return fallback;
    }
    return ResourceLocation.fromNamespaceAndPath(icon.getNamespace(), path);
  }

  private static String hashSuffix(String value) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 4);
    } catch (NoSuchAlgorithmException exception) {
      return Integer.toHexString(value.hashCode());
    }
  }
}
