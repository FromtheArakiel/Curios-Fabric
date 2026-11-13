/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curiosfabric.common.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import org.apache.commons.lang3.EnumUtils;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.common.CuriosFabricConfig;
import dev.arakiel.curiosfabric.common.util.CuriosConditions;
import dev.arakiel.curiosfabric.common.slottype.NeoforgeCuriosSlotManager;
import dev.arakiel.curiosfabric.common.slottype.SlotType;
import dev.arakiel.curiosfabric.compat.NativeSlotMappings;

public class CuriosSlotManager extends SimpleJsonResourceReloadListener
    implements IdentifiableResourceReloadListener {

  /** The id under which this listener is registered with the Fabric resource loader. */
  public static final ResourceLocation ID =
      ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "slots");

  private static final Gson GSON =
      (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
  public static CuriosSlotManager SERVER = new CuriosSlotManager();
  public static CuriosSlotManager CLIENT = new CuriosSlotManager();
  private Map<String, ISlotType> slots = ImmutableMap.of();
  private Set<String> configSlots = ImmutableSet.of();
  private Map<String, ResourceLocation> icons = ImmutableMap.of();
  private Map<String, Set<String>> idToMods = ImmutableMap.of();

  public CuriosSlotManager() {
    super(GSON, CuriosFabricConstants.MOD_ID + "/slots");
  }

  /** Our own data layout, followed by the layout of the original Curios edition (bridge input). */
  private static List<String> directories() {
    return List.of(CuriosFabricConstants.MOD_ID + "/slots", CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE + "/slots");
  }

  /** Reads a JSON file of the foreign data layout, which the superclass does not hand over. */
  private static JsonElement readJson(IoSupplier<InputStream> supplier) {

    try (Reader reader = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader);
    } catch (Exception exception) {
      CuriosFabricConstants.LOG.error("Failed to read a curio slot data file", exception);
      return null;
    }
  }

  @Override
  public ResourceLocation getFabricId() {
    return ID;
  }

  protected void apply(@Nonnull Map<ResourceLocation, JsonElement> pObject,
                       @Nonnull ResourceManager pResourceManager,
                       @Nonnull ProfilerFiller pProfiler) {
    NeoforgeCuriosSlotManager.buildImcSlotTypes();
    Map<String, SlotType.Builder> map = new HashMap<>();
    Map<String, ImmutableSet.Builder<String>> modMap = new HashMap<>();
    Map<ResourceLocation, JsonElement> sorted = new LinkedHashMap<>();
    Map<ResourceLocation, SlotSource> sourceMap = new HashMap<>();
    for (String directory : directories()) {
      SlotSource source = directory.equals(
          CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE + "/slots")
          ? SlotSource.NEOFORGE_CURIOS : SlotSource.CURIOSFABRIC;
      Map<ResourceLocation, Resource> resources = pResourceManager.listResources(directory,
          resource -> resource.getPath().endsWith(".json"));
      CuriosFabricConstants.LOG.info("Found {} slot resources in {}", resources.size(), directory);
      resources.forEach((resourceLocation, resource) -> {
            String path = resourceLocation.getPath();
            ResourceLocation slot = ResourceLocation.fromNamespaceAndPath(
                resourceLocation.getNamespace(),
                path.substring(directory.length() + 1, path.length() - ".json".length()));
            JsonElement element = readJson(resource::open);
            if (element != null) {
              sorted.put(slot, element);
              sourceMap.put(slot, source);
            }
          });
    }

    for (Map.Entry<ResourceLocation, JsonElement> entry : sorted.entrySet()) {
      ResourceLocation resourcelocation = entry.getKey();

      if (resourcelocation.getNamespace().equals(CuriosFabricConstants.MOD_ID)) {

        try {
          String id = resourcelocation.getPath();
          SlotSource source = sourceMap.getOrDefault(
              resourcelocation, SlotSource.CURIOSFABRIC);
          if (source == SlotSource.NEOFORGE_CURIOS
              && resourcelocation.getNamespace()
                  .equals(CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE)
              && NativeSlotMappings.neoforgeNative(id) != null) {
            id = NativeSlotMappings.neoforgeNative(id);
          }

          if (!CuriosConditions.matches(entry.getValue().getAsJsonObject())) {
            CuriosFabricConstants.LOG.debug("Skipping loading slot {} as its conditions were not met",
                resourcelocation);
            continue;
          }
          SlotType.Builder builder = map.computeIfAbsent(id, (k) -> new SlotType.Builder(id));
          builder.sourceIfAbsent(source);
          fromJson(builder, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
          modMap.computeIfAbsent(id, (k) -> ImmutableSet.builder())
              .add(resourcelocation.getNamespace());
        } catch (IllegalArgumentException | JsonParseException e) {
          CuriosFabricConstants.LOG.error("Parsing error loading curio slot {}", resourcelocation, e);
        }
      }
    }

    // Neoforge IMC slot registrations
    // TODO remove in 1.22
    for (Map.Entry<String, SlotType.Builder> entry : NeoforgeCuriosSlotManager.getImcBuilders()
        .entrySet()) {
      SlotType.Builder builder =
          map.computeIfAbsent(entry.getKey(), (k) -> new SlotType.Builder(entry.getKey()));
      builder.sourceIfAbsent(SlotSource.NEOFORGE_CURIOS);
      builder.apply(entry.getValue());
    }

    for (Map.Entry<String, Set<String>> entry : NeoforgeCuriosSlotManager.getIdsToMods().entrySet()) {
      modMap.computeIfAbsent(entry.getKey(), (k) -> ImmutableSet.builder())
          .addAll(entry.getValue());
    }

    for (Map.Entry<ResourceLocation, JsonElement> entry : sorted.entrySet()) {
      ResourceLocation resourcelocation = entry.getKey();

      if (resourcelocation.getPath().startsWith("_") ||
          resourcelocation.getNamespace().equals(CuriosFabricConstants.MOD_ID)) {
        continue;
      }

      try {
        String id = resourcelocation.getPath();
        SlotSource source = sourceMap.getOrDefault(
            resourcelocation, SlotSource.CURIOSFABRIC);
        if (source == SlotSource.NEOFORGE_CURIOS
            && resourcelocation.getNamespace()
                .equals(CuriosFabricConstants.NEOFORGE_CURIOS_DATA_NAMESPACE)
            && NativeSlotMappings.neoforgeNative(id) != null) {
          id = NativeSlotMappings.neoforgeNative(id);
        }

        if (!CuriosConditions.matches(entry.getValue().getAsJsonObject())) {
          CuriosFabricConstants.LOG.debug("Skipping loading slot {} as its conditions were not met",
              resourcelocation);
          continue;
        }
        SlotType.Builder builder = map.computeIfAbsent(id, (k) -> new SlotType.Builder(id));
        builder.sourceIfAbsent(source);
        fromJson(builder, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
        modMap.computeIfAbsent(id, (k) -> ImmutableSet.builder())
            .add(resourcelocation.getNamespace());
      } catch (IllegalArgumentException | JsonParseException e) {
        CuriosFabricConstants.LOG.error("Parsing error loading curio slot {}", resourcelocation, e);
      }
    }

    try {
      Set<String> configs = fromConfig(map);
      this.configSlots = ImmutableSet.copyOf(configs);

      for (String id : configs) {
        modMap.computeIfAbsent(id, (k) -> ImmutableSet.builder()).add("config");
      }
    } catch (IllegalArgumentException e) {
      CuriosFabricConstants.LOG.error("Config parsing error", e);
    }
    this.slots = map.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().build()));
    this.idToMods = modMap.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> entry.getValue().build()));
    CuriosFabricConstants.LOG.info("Loaded {} curio slots ({})", map.size(),
        Arrays.stream(SlotSource.values())
            .map(source -> source.id() + "=" + this.slots.values().stream()
                .filter(slot -> slot.getSource() == source)
                .count())
            .collect(Collectors.joining(", ")));
  }

  public Map<String, ISlotType> getSlots() {
    return this.slots;
  }

  /** Adds dynamically discovered compatibility slots without replacing data-defined slots. */
  public void mergeSlots(Map<String, ISlotType> additions) {
    mergeSlots(additions, null);
  }

  /**
   * Adds dynamically discovered compatibility slots and records their source for diagnostics.
   */
  public void mergeSlots(Map<String, ISlotType> additions, String sourceModId) {
    if (additions.isEmpty()) {
      return;
    }
    Map<String, ISlotType> merged = new LinkedHashMap<>(this.slots);
    Set<String> added = new HashSet<>();
    additions.forEach((id, slot) -> {
      if (!merged.containsKey(id)) {
        merged.put(id, slot);
        added.add(id);
      }
    });
    this.slots = ImmutableMap.copyOf(merged);
    if (sourceModId == null || added.isEmpty()) {
      return;
    }
    Map<String, Set<String>> addedSources = new LinkedHashMap<>();
    for (String id : added) {
      addedSources.put(id, Set.of(sourceModId));
    }
    mergeSlotSources(addedSources);
  }

  /** Records the provider mods for dynamically discovered slot types. */
  public void mergeSlotSources(Map<String, Set<String>> sources) {
    if (sources.isEmpty()) {
      return;
    }
    Map<String, Set<String>> mergedSources = new LinkedHashMap<>();
    this.idToMods.forEach((id, owners) ->
        mergedSources.put(id, new HashSet<>(owners)));
    sources.forEach((id, owners) ->
        mergedSources.computeIfAbsent(id, ignored -> new HashSet<>()).addAll(owners));
    this.idToMods = mergedSources.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
            entry -> ImmutableSet.copyOf(entry.getValue())));
  }

  public Optional<ISlotType> getSlot(String id) {
    return Optional.ofNullable(this.slots.get(id));
  }

  public static ListTag getSyncPacket() {
    ListTag tag = new ListTag();

    for (Map.Entry<String, ISlotType> entry : SERVER.slots.entrySet()) {
      tag.add(entry.getValue().writeNbt());
    }
    return tag;
  }

  public static void applySyncPacket(ListTag tag) {
    ImmutableMap.Builder<String, ISlotType> map = ImmutableMap.builder();

    for (Tag tag1 : tag) {

      if (tag1 instanceof CompoundTag slotType) {
        ISlotType type = SlotType.from(slotType);
        map.put(type.getIdentifier(), type);
      }
    }
    CLIENT.slots = map.build();
  }

  public void setIcons(Map<String, ResourceLocation> icons) {
    this.icons = ImmutableMap.copyOf(icons);
  }

  public Set<String> getConfigSlots() {
    return this.configSlots;
  }

  public Map<String, ResourceLocation> getIcons() {
    return this.icons;
  }

  public ResourceLocation getIcon(String identifier) {
    return this.icons.getOrDefault(identifier,
        ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "slot/empty_curio_slot"));
  }

  public Map<String, Set<String>> getModsFromSlots() {
    return this.idToMods;
  }

  public static Set<String> fromConfig(Map<String, SlotType.Builder> map)
      throws IllegalArgumentException {
    List<Map<String, String>> parsed = new ArrayList<>();
    List<? extends String> list = CuriosFabricConfig.COMMON.value().slots;
    Set<String> results = new HashSet<>();

    for (String s : list) {
      StringTokenizer tokenizer = new StringTokenizer(s, ";");
      Map<String, String> subMap = new HashMap<>();

      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        String[] keyValue = token.split("=");
        subMap.put(keyValue[0], keyValue[1]);
      }

      if (subMap.containsKey("id")) {
        parsed.add(subMap);
      } else {
        throw new IllegalArgumentException(
            "Cannot load config entry " + s + " due to missing id field");
      }
    }

    for (Map<String, String> entry : parsed) {
      String id = entry.get("id");
      SlotType.Builder builder = map.computeIfAbsent(id, (k) -> new SlotType.Builder(id));
      Integer size = entry.containsKey("size") ? Integer.parseInt(entry.get("size")) : null;

      if (size != null && size < 0) {
        throw new IllegalArgumentException("Size cannot be less than 0!");
      }
      String operation = entry.getOrDefault("operation", "SET");

      if (!operation.equals("SET") && !operation.equals("ADD") && !operation.equals("REMOVE")) {
        throw new IllegalArgumentException(operation + " is not a valid operation!");
      }
      String dropRule = entry.getOrDefault("drop_rule", "");

      if (!dropRule.isEmpty() && !EnumUtils.isValidEnum(ICurio.DropRule.class, dropRule)) {
        throw new IllegalArgumentException(dropRule + " is not a valid drop rule!");
      }
      results.add(id);
      boolean replace = true;
      Integer order = entry.containsKey("order") ? Integer.parseInt(entry.get("order")) : null;
      String icon = entry.getOrDefault("icon", "");
      Boolean toggle =
          entry.containsKey("render_toggle") ? Boolean.parseBoolean(entry.get("render_toggle")) :
              null;
      Boolean cosmetic =
          entry.containsKey("add_cosmetic") ? Boolean.parseBoolean(entry.get("add_cosmetic")) :
              null;
      Boolean nativeGui =
          entry.containsKey("use_native_gui") ? Boolean.parseBoolean(entry.get("use_native_gui")) :
              null;

      if (order != null) {
        builder.order(order, replace);
      }

      if (!icon.isEmpty()) {
        builder.icon(ResourceLocation.parse(icon));
      }

      if (!dropRule.isEmpty()) {
        builder.dropRule(dropRule);
      }

      if (size != null) {
        builder.size(size, operation, replace);
      }

      if (cosmetic != null) {
        builder.hasCosmetic(cosmetic, replace);
      }

      if (nativeGui != null) {
        builder.useNativeGui(nativeGui, replace);
      }

      if (toggle != null) {
        builder.renderToggle(toggle, replace);
      }
    }
    return results;
  }

  public static void fromJson(SlotType.Builder builder, JsonObject jsonObject)
      throws IllegalArgumentException, JsonParseException {
    Integer jsonSize = jsonObject.has("size") ? GsonHelper.getAsInt(jsonObject, "size") : null;

    if (jsonSize != null && jsonSize < 0) {
      throw new IllegalArgumentException("Size cannot be less than 0!");
    }
    String operation = GsonHelper.getAsString(jsonObject, "operation", "SET");

    if (!operation.equals("SET") && !operation.equals("ADD") && !operation.equals("REMOVE")) {
      throw new IllegalArgumentException(operation + " is not a valid operation!");
    }
    String jsonDropRule = GsonHelper.getAsString(jsonObject, "drop_rule", "");

    if (!jsonDropRule.isEmpty() && !EnumUtils.isValidEnum(ICurio.DropRule.class, jsonDropRule)) {
      throw new IllegalArgumentException(jsonDropRule + " is not a valid drop rule!");
    }
    boolean replace = GsonHelper.getAsBoolean(jsonObject, "replace", false);
    Integer jsonOrder = jsonObject.has("order") ? GsonHelper.getAsInt(jsonObject, "order") : null;
    String jsonIcon = GsonHelper.getAsString(jsonObject, "icon", "");
    Boolean jsonToggle =
        jsonObject.has("render_toggle") ? GsonHelper.getAsBoolean(jsonObject, "render_toggle") :
            null;
    Boolean jsonCosmetic =
        jsonObject.has("add_cosmetic") ? GsonHelper.getAsBoolean(jsonObject, "add_cosmetic") : null;
    Boolean jsonNative =
        jsonObject.has("use_native_gui") ? GsonHelper.getAsBoolean(jsonObject, "use_native_gui") :
            null;
    JsonArray jsonSlotResultPredicate = jsonObject.has("validators") ?
        GsonHelper.getAsJsonArray(jsonObject, "validators") : null;

    if (jsonOrder != null) {
      builder.order(jsonOrder, replace);
    }

    if (!jsonIcon.isEmpty()) {
      builder.icon(ResourceLocation.parse(jsonIcon));
    }

    if (!jsonDropRule.isEmpty()) {
      builder.dropRule(jsonDropRule);
    }

    if (jsonSize != null) {
      builder.size(jsonSize, operation, replace);
    }

    if (jsonCosmetic != null) {
      builder.hasCosmetic(jsonCosmetic, replace);
    }

    if (jsonNative != null) {
      builder.useNativeGui(jsonNative, replace);
    }

    if (jsonToggle != null) {
      builder.renderToggle(jsonToggle, replace);
    }

    if (jsonSlotResultPredicate != null) {

      for (JsonElement jsonElement : jsonSlotResultPredicate) {
        builder.validator(ResourceLocation.parse(jsonElement.getAsString()));
      }
    }
  }
}
