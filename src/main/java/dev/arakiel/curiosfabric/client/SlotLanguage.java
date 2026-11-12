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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat;

/** Resolves slot names through the language namespace of their source implementation. */
public final class SlotLanguage {

  private SlotLanguage() {
  }

  public static String name(String slotId) {
    return translationKey(slotId).map(I18n::get).orElseGet(() -> fallbackName(slotId));
  }

  public static MutableComponent displayName(String slotId) {
    return translationKey(slotId)
        .<MutableComponent>map(Component::translatable)
        .orElseGet(() -> Component.literal(fallbackName(slotId)));
  }

  /** Returns the first available source-specific language key. */
  public static Optional<String> translationKey(String slotId) {
    Optional<ISlotType> slot = CuriosSlotManager.CLIENT.getSlot(slotId);
    SlotSource source = slot.map(ISlotType::getSource).orElse(null);
    return translationKey(slotId, source, I18n::exists);
  }

  static Optional<String> translationKey(String slotId, SlotSource source,
                                         Predicate<String> exists) {
    List<String> candidates = new ArrayList<>();

    if (source == SlotSource.NEOFORGE_CURIOS) {
      SlotSource.NEOFORGE_CURIOS.translationKey(slotId).ifPresent(candidates::add);
      SlotSource.CURIOSFABRIC.translationKey(slotId).ifPresent(candidates::add);
    } else if (source == SlotSource.TRINKETS) {
      TrinketsCompat.translationKeyFor(slotId).ifPresent(candidates::add);
      SlotSource.CURIOSFABRIC.translationKey(slotId).ifPresent(candidates::add);
    } else if (source == SlotSource.CURIOSFABRIC) {
      SlotSource.CURIOSFABRIC.translationKey(slotId).ifPresent(candidates::add);
    } else {
      SlotSource.CURIOSFABRIC.translationKey(slotId).ifPresent(candidates::add);
      SlotSource.NEOFORGE_CURIOS.translationKey(slotId).ifPresent(candidates::add);
      TrinketsCompat.translationKeyFor(slotId).ifPresent(candidates::add);
    }
    return candidates.stream().filter(exists).findFirst();
  }

  private static String fallbackName(String slotId) {
    return Character.toUpperCase(slotId.charAt(0)) + slotId.substring(1).toLowerCase();
  }
}
