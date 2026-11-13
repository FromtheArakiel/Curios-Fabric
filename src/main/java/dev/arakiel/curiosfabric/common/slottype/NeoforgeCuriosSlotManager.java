package dev.arakiel.curiosfabric.common.slottype;

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
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosEntrypoint;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.api.SlotTypeMessage;
import dev.arakiel.curiosfabric.api.SlotTypePreset;
import dev.arakiel.curiosfabric.common.slottype.SlotType.Builder;
import dev.arakiel.curiosfabric.compat.CompatTargets;
import dev.arakiel.curiosfabric.compat.neoforgecurios.NeoforgeCuriosCompat;

/**
 * Collects slot type definitions from the {@code curiosfabric} entrypoints.
 *
 * <p>This replaces the NeoForge inter mod communication channels {@code register_curio} and
 * {@code modify_curio}. Both are kept separate because a mod may only contribute a brand new slot
 * type through the first channel, while the second one can only change an existing one.</p>
 */
public class NeoforgeCuriosSlotManager {

  private static final Map<String, Builder> IMC_BUILDERS = new HashMap<>();
  private static final Map<String, Set<String>> IDS_TO_MODS = new HashMap<>();

  private NeoforgeCuriosSlotManager() {
  }

  public static Map<String, Set<String>> getIdsToMods() {
    return ImmutableMap.copyOf(IDS_TO_MODS);
  }

  public static Map<String, Builder> getImcBuilders() {
    return ImmutableMap.copyOf(IMC_BUILDERS);
  }

  /**
   * Materializes the current IMC builders without mutating their reusable size-operation state.
   *
   * <p>NeoForge can finish inter-mod communication after the Fabric data-pack reload has already
   * built {@link dev.arakiel.curiosfabric.common.data.CuriosSlotManager#SERVER}. This snapshot lets
   * the late callback add those slot types without rebuilding and losing the data-pack slots.</p>
   */
  public static Map<String, ISlotType> getImcSlotTypes() {
    Map<String, ISlotType> result = new HashMap<>();
    IMC_BUILDERS.forEach((id, source) -> {
      Builder copy = new Builder(id);
      copy.apply(source);
      result.put(id, copy.build());
    });
    return ImmutableMap.copyOf(result);
  }

  /** Reads the {@code curiosfabric} entrypoints and turns them into slot type blueprints. */
  public static void buildImcSlotTypes() {
    IMC_BUILDERS.clear();
    IDS_TO_MODS.clear();
    List<Pair<String, SlotTypeMessage>> register = new ArrayList<>();
    List<Pair<String, SlotTypeMessage>> modify = new ArrayList<>();

    for (EntrypointContainer<CuriosEntrypoint> container : FabricLoader.getInstance()
        .getEntrypointContainers("curiosfabric", CuriosEntrypoint.class)) {
      String modId = container.getProvider().getMetadata().getId();

      try {
        CuriosEntrypoint entrypoint = container.getEntrypoint();
        entrypoint.createSlots(message -> register.add(Pair.of(modId, message)));
        entrypoint.modifySlots(message -> modify.add(Pair.of(modId, message)));
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.error("Failed to read the curios entrypoint of {}", modId, throwable);
      }
    }
    processImc(register, true);
    processImc(modify, false);
    readNeoforgeCuriosImc();
  }

  private static void readNeoforgeCuriosImc() {
    if (!NeoforgeCuriosCompat.isLoaded()) {
      return;
    }

    try {
      Class<?> manager = CompatTargets.loadClass(
          "top.theillusivec4.curios.common.slottype.LegacySlotManager");
      if (manager == null) {
        return;
      }
      Object rawBuilders = manager.getMethod("getImcBuilders").invoke(null);
      if (rawBuilders instanceof Map<?, ?> builders) {
        for (Map.Entry<?, ?> entry : builders.entrySet()) {
          String id = String.valueOf(entry.getKey());
          Object foreignBuilder = entry.getValue();
          Object slot = foreignBuilder.getClass().getMethod("build").invoke(foreignBuilder);
          Builder builder = IMC_BUILDERS.computeIfAbsent(id, Builder::new);
          builder.sourceIfAbsent(SlotSource.NEOFORGE_CURIOS)
              .order(readInt(slot, "getOrder"), true)
              .size(readInt(slot, "getSize"), "SET", true)
              .useNativeGui(readBoolean(slot, "useNativeGui"), true)
              .hasCosmetic(readBoolean(slot, "hasCosmetic"), true)
              .renderToggle(readBoolean(slot, "canToggleRendering"), true);
          Object icon = slot.getClass().getMethod("getIcon").invoke(slot);
          if (icon instanceof ResourceLocation resourceLocation) {
            builder.icon(resourceLocation);
          }
          Object dropRule = slot.getClass().getMethod("getDropRule").invoke(slot);
          if (dropRule instanceof Enum<?> value) {
            try {
              builder.dropRule(ICurio.DropRule.valueOf(value.name()));
            } catch (IllegalArgumentException ignored) {
              // Unknown foreign drop rules fall back to DEFAULT.
            }
          }
        }
      }

      Object rawMods = manager.getMethod("getIdsToMods").invoke(null);
      if (rawMods instanceof Map<?, ?> mods) {
        for (Map.Entry<?, ?> entry : mods.entrySet()) {
          String id = String.valueOf(entry.getKey());
          if (entry.getValue() instanceof Iterable<?> owners) {
            Set<String> target = IDS_TO_MODS.computeIfAbsent(id, ignored -> new HashSet<>());
            for (Object owner : owners) {
              target.add(String.valueOf(owner));
            }
          }
        }
      }
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug(
          "Could not read NeoForge Curios IMC slot registrations", throwable);
    }
  }

  private static int readInt(Object target, String method) throws ReflectiveOperationException {
    return (Integer) target.getClass().getMethod(method).invoke(target);
  }

  private static boolean readBoolean(Object target, String method)
      throws ReflectiveOperationException {
    return (Boolean) target.getClass().getMethod(method).invoke(target);
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
          builder.sourceIfAbsent(SlotSource.NEOFORGE_CURIOS);
          IMC_BUILDERS.put(id, builder);
          IDS_TO_MODS.computeIfAbsent(id, k -> new HashSet<>()).add(modId);
        }

        if (builder != null) {
          builder.sourceIfAbsent(SlotSource.NEOFORGE_CURIOS);
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
