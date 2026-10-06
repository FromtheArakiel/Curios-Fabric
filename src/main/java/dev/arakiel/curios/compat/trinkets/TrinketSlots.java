/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curios.compat.trinkets;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapping table between Trinkets' slot groups and Curios' slot identifiers.
 *
 * <p>Trinkets names its slot groups {@code <group>/<slot>} (for example {@code chest/necklace} or
 * {@code hand/ring}) and assigns items to them through the matching item tag
 * ({@code #trinkets:chest/necklace}). Curios only needs the slot part, so the table below is the
 * whole translation layer.</p>
 */
public final class TrinketSlots {

  /** Trinkets slot group to Curios slot identifier. */
  private static final Map<String, String> GROUPS = new LinkedHashMap<>();
  /** Curios slot identifier to the Trinkets group it was mapped from. */
  private static final Map<String, String> REVERSE = new LinkedHashMap<>();

  static {
    put("head/hat", "head");
    put("head/mask", "head");
    put("chest/necklace", "necklace");
    put("chest/back", "back");
    put("chest/cape", "back");
    put("chest/trinket", "charm");
    put("hand/ring", "ring");
    put("offhand/ring", "ring");
    put("hand/glove", "hands");
    put("legs/belt", "belt");
    put("feet/aglet", "feet");
    put("feet/shoes", "feet");
  }

  private TrinketSlots() {
  }

  private static void put(String trinketsGroup, String curiosSlot) {
    GROUPS.put(trinketsGroup, curiosSlot);
    REVERSE.putIfAbsent(curiosSlot, trinketsGroup);
  }

  /** All Trinkets slot groups that have a Curios counterpart. */
  public static Map<String, String> groups() {
    return GROUPS;
  }

  /** Curios slot identifier for a Trinkets slot group, or {@code null}. */
  public static String toCurios(String trinketsGroup) {
    return GROUPS.get(trinketsGroup);
  }

  /** Trinkets slot group for a Curios slot identifier, or {@code null}. */
  public static String toTrinkets(String curiosSlot) {
    return REVERSE.get(curiosSlot);
  }
}
