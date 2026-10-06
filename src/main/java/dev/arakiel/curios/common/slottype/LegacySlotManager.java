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

package dev.arakiel.curios.common.slottype;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.FabricLoader;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.CuriosEntrypoint;
import dev.arakiel.curios.api.SlotTypeMessage;
import dev.arakiel.curios.api.SlotTypePreset;
import dev.arakiel.curios.common.slottype.SlotType.Builder;

/**
 * Collects slot type definitions from the {@code curios} entrypoints.
 *
 * <p>This replaces the NeoForge inter mod communication channels {@code register_curio} and
 * {@code modify_curio}. Both are kept separate because a mod may only contribute a brand new slot
 * type through the first channel, while the second one can only change an existing one.</p>
 */
public class LegacySlotManager {

  private static final Map<String, Builder> IMC_BUILDERS = new HashMap<>();
  private static final Map<String, Set<String>> IDS_TO_MODS = new HashMap<>();

  private LegacySlotManager() {
  }

  public static Map<String, Set<String>> getIdsToMods() {
    return ImmutableMap.copyOf(IDS_TO_MODS);
  }

  public static Map<String, Builder> getImcBuilders() {
    return ImmutableMap.copyOf(IMC_BUILDERS);
  }

  /** Reads the {@code curios} entrypoints and turns them into slot type blueprints. */
  public static void buildImcSlotTypes() {
    IMC_BUILDERS.clear();
    IDS_TO_MODS.clear();
    List<Pair<String, SlotTypeMessage>> register = new ArrayList<>();
    List<Pair<String, SlotTypeMessage>> modify = new ArrayList<>();

    for (EntrypointContainer<CuriosEntrypoint> container : FabricLoader.getInstance()
        .getEntrypointContainers("curios", CuriosEntrypoint.class)) {
      String modId = container.getProvider().getMetadata().getId();

      try {
        CuriosEntrypoint entrypoint = container.getEntrypoint();
        entrypoint.createSlots(message -> register.add(Pair.of(modId, message)));
        entrypoint.modifySlots(message -> modify.add(Pair.of(modId, message)));
      } catch (Throwable throwable) {
        CuriosConstants.LOG.error("Failed to read the curios entrypoint of {}", modId, throwable);
      }
    }
    processImc(register, true);
    processImc(modify, false);
  }

  private static void processImc(List<Pair<String, SlotTypeMessage>> messages, boolean create) {
    TreeMap<String, List<SlotTypeMessage>> messageMap = new TreeMap<>();

    for (Pair<String, SlotTypeMessage> message : messages) {
      String modId = message.getFirst();
      SlotTypeMessage slotTypeMessage = message.getSecond();

      if (slotTypeMessage == null) {
        continue;
      }
      messageMap.computeIfAbsent(modId, k -> new ArrayList<>()).add(slotTypeMessage);
    }

    for (Map.Entry<String, List<SlotTypeMessage>> entry : messageMap.entrySet()) {
      String modId = entry.getKey();

      for (SlotTypeMessage msg : entry.getValue()) {
        String id = msg.getIdentifier();
        Builder builder = IMC_BUILDERS.get(id);

        if (builder == null && create) {
          builder = new Builder(id);
          IMC_BUILDERS.put(id, builder);
          IDS_TO_MODS.computeIfAbsent(id, k -> new HashSet<>()).add(modId);
        }

        if (builder != null) {
          builder.size(msg.getSize()).useNativeGui(msg.isVisible()).hasCosmetic(msg.hasCosmetic());
          SlotTypeMessage.Builder preset = SlotTypePreset.findPreset(id)
              .map(SlotTypePreset::getMessageBuilder).orElse(null);
          SlotTypeMessage presetMsg = preset != null ? preset.build() : null;

          if (msg.getIcon() == null && presetMsg != null && presetMsg.getIcon() != null) {
            builder.icon(presetMsg.getIcon());
          } else if (msg.getIcon() != null) {
            builder.icon(msg.getIcon());
          }

          if (msg.getPriority() == null && presetMsg != null && presetMsg.getPriority() != null) {
            builder.order(presetMsg.getPriority());
          } else if (msg.getPriority() != null) {
            builder.order(msg.getPriority());
          }
        }
      }
    }
  }
}
