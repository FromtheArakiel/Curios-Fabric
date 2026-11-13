/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curiosfabric.compat;

import java.util.Locale;
import java.util.Map;

/**
 * Semantic mappings for slots that ship with the compatibility targets themselves.
 *
 * <p>Only target-native paths are listed here. Third-party slots are appended by their slot name;
 * Trinkets' body groups are intentionally not part of the Curios identifier.</p>
 */
public final class NativeSlotMappings {

  private static final Map<String, String> TRINKETS_NATIVE = Map.ofEntries(
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

  private static final Map<String, String> NEOFORGE_NATIVE = Map.ofEntries(
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

  private NativeSlotMappings() {
  }

  /** Returns the Curios-native target for a Trinkets-native path, or {@code null}. */
  public static String trinketsNative(String path) {
    return TRINKETS_NATIVE.get(path);
  }

  public static boolean isTrinketsNative(String path) {
    return TRINKETS_NATIVE.containsKey(path);
  }

  /**
   * Appends a third-party Trinkets slot using its slot name only.
   *
   * <p>The group is a body location in Trinkets (for example {@code head}, {@code chest}, or
   * {@code hand}); Curios has no equivalent body-location concept, so
   * {@code example/custom-slot -> trinkets_custom_slot}.</p>
   */
  public static String appendTrinketsSlot(String path) {
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

  /** Keeps the explicit native NeoForge Curios namespace table in one place. */
  public static String neoforgeNative(String id) {
    return NEOFORGE_NATIVE.get(id);
  }
}
