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

package dev.arakiel.curios.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curios.api.type.data.IEntitiesData;
import dev.arakiel.curios.api.type.data.ISlotData;
import dev.arakiel.curios.common.data.EntitiesData;
import dev.arakiel.curios.common.data.SlotData;

/**
 * Basic data generator for curios slots and entities.
 *
 * <p>The NeoForge edition took an {@code ExistingFileHelper} from NeoForge's data generator API.
 * Fabric's data generation is built on vanilla's {@link PackOutput} only, so the helper parameter
 * was dropped. {@code FabricDataOutput} extends {@link PackOutput}, so this provider can be used
 * from a Fabric data generator unchanged.</p>
 */
public abstract class CuriosDataProvider implements DataProvider {
  private final PackOutput.PathProvider entitiesPathProvider;
  private final PackOutput.PathProvider slotsPathProvider;
  private final CompletableFuture<HolderLookup.Provider> registries;
  private final String modId;
  private final Map<String, ISlotData> slotBuilders = new HashMap<>();
  private final Map<String, IEntitiesData> entitiesBuilders = new HashMap<>();

  public CuriosDataProvider(String modId, PackOutput output,
                            CompletableFuture<HolderLookup.Provider> registries) {
    this.modId = modId;
    this.entitiesPathProvider =
        output.createPathProvider(PackOutput.Target.DATA_PACK, "curios/entities");
    this.slotsPathProvider =
        output.createPathProvider(PackOutput.Target.DATA_PACK, "curios/slots");
    this.registries = registries;
  }

  public abstract void generate(HolderLookup.Provider registries);

  @Nonnull
  public CompletableFuture<?> run(@Nonnull CachedOutput pOutput) {
    return this.registries.thenCompose((registryProvider) -> {
      List<CompletableFuture<?>> list = new ArrayList<>();
      this.generate(registryProvider);
      this.slotBuilders.forEach((slot, slotBuilder) -> {
        Path path =
            this.slotsPathProvider.json(ResourceLocation.fromNamespaceAndPath(this.modId, slot));
        list.add(DataProvider.saveStable(pOutput, slotBuilder.serialize(registryProvider), path));
      });
      this.entitiesBuilders.forEach((entities, entitiesBuilder) -> {
        Path path = this.entitiesPathProvider.json(
            ResourceLocation.fromNamespaceAndPath(this.modId, entities));
        list.add(
            DataProvider.saveStable(pOutput, entitiesBuilder.serialize(registryProvider), path));
      });
      return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
    });
  }

  public final ISlotData createSlot(String id) {
    return this.slotBuilders.computeIfAbsent(id, (k) -> createSlotData());
  }

  public final ISlotData copySlot(String id, String copyId) {

    if (id.equals(copyId)) {
      return createSlot(id);
    }
    return this.slotBuilders.computeIfAbsent(id,
        (k) -> this.slotBuilders.getOrDefault(copyId, createSlotData()));
  }

  public final IEntitiesData createEntities(String id) {
    return this.entitiesBuilders.computeIfAbsent(id, (k) -> createEntitiesData());
  }

  public final IEntitiesData copyEntities(String id, String copyId) {

    if (id.equals(copyId)) {
      return createEntities(id);
    }
    return this.entitiesBuilders.computeIfAbsent(id,
        (k) -> this.entitiesBuilders.getOrDefault(copyId, createEntitiesData()));
  }

  @Nonnull
  public final String getName() {
    return "Curios for " + this.modId;
  }

  private static ISlotData createSlotData() {
    return new SlotData();
  }

  private static IEntitiesData createEntitiesData() {
    return new EntitiesData();
  }
}
