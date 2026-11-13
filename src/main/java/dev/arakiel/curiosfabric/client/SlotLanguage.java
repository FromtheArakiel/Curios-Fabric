/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.arakiel.curiosfabric.client;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat;

/** Resolves slot names through the language namespace of their source implementation. */
public final class SlotLanguage {

  private static final Set<String> REPORTED_MISSING_KEYS = ConcurrentHashMap.newKeySet();

  private SlotLanguage() {
  }

  public static String name(String slotId) {
    String key = translationKey(slotId);
    reportIfMissing(slotId, key);
    return I18n.get(key);
  }

  public static MutableComponent displayName(String slotId) {
    String key = translationKey(slotId);
    reportIfMissing(slotId, key);
    return Component.translatable(key);
  }

  /**
   * Returns the theoretical language key for the slot's source.
   *
   * <p>No cross-source fallback is attempted: a missing translation intentionally renders as the
   * raw key, matching vanilla's missing-language behaviour.</p>
   */
  public static String translationKey(String slotId) {
    Optional<ISlotType> slot = CuriosSlotManager.CLIENT.getSlot(slotId);
    SlotSource source = slot.map(ISlotType::getSource).orElse(null);
    return theoreticalKey(slotId, source);
  }

  static String theoreticalKey(String slotId, SlotSource source) {
    if (source == SlotSource.TRINKETS) {
      return TrinketsCompat.translationKeyFor(slotId)
          .orElse("trinkets.slot." + slotId);
    }
    return (source == null ? SlotSource.CURIOSFABRIC : source)
        .translationKey(slotId)
        .orElse("curiosfabric.identifier." + slotId);
  }

  private static void reportIfMissing(String slotId, String key) {
    if (!I18n.exists(key) && REPORTED_MISSING_KEYS.add(key)) {
      SlotSource source = CuriosSlotManager.CLIENT.getSlot(slotId)
          .map(ISlotType::getSource)
          .orElse(null);
      CuriosFabricConstants.LOG.warn("Missing slot translation key {} for slot {} (source {})",
          key, slotId, source == null ? "unknown" : source.id());
    }
  }
}
