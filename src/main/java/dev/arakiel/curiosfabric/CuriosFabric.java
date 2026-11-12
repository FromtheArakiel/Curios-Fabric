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

package dev.arakiel.curiosfabric;

import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.CuriosEntrypoint;
import dev.arakiel.curiosfabric.api.event.CuriosEventBus;
import dev.arakiel.curiosfabric.api.extensions.RegisterCuriosExtensionsEvent;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.common.CuriosFabricConfig;
import dev.arakiel.curiosfabric.common.CuriosHelper;
import dev.arakiel.curiosfabric.common.CuriosFabricRegistry;
import dev.arakiel.curiosfabric.common.data.CuriosEntityManager;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.common.event.CuriosFabricEventHandler;
import dev.arakiel.curiosfabric.common.integration.CuriosIntegrations;
import dev.arakiel.curiosfabric.common.network.NetworkHandler;
import dev.arakiel.curiosfabric.common.slottype.NeoforgeCuriosSlotManager;
import dev.arakiel.curiosfabric.compat.neoforgecurios.NeoforgeCuriosCompat;
import dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat;
import dev.arakiel.curiosfabric.server.SlotHelper;
import dev.arakiel.curiosfabric.server.command.CurioArgumentType;
import dev.arakiel.curiosfabric.server.command.CuriosFabricCommand;
import dev.arakiel.curiosfabric.server.command.CuriosSelectorOptions;
import dev.arakiel.gemini.event.GeminiEvents;
import dev.arakiel.gemini.event.server.ServerLifecycleEvent;
import dev.arakiel.gemini.integration.GeminiIntegrations;

/**
 * Entry point of the Fabric build.
 *
 * <p>Mirrors the responsibilities of the Neoforge entry point: registry bootstrap, the helper and
 * slot helper lifecycles, command registration, the reload listeners for slot/entity data and the
 * optional integrations. Neoforge's capability registration and event bus listeners are replaced by
 * the Fabric implementations in {@code dev.arakiel.curiosfabric.platform}, by the handlers in
 * {@code CuriosFabricEventHandler} and by the mixins in {@code dev.arakiel.curiosfabric.mixin}.</p>
 */
public class CuriosFabric implements ModInitializer {

  public static final String MOD_ID = CuriosFabricConstants.MOD_ID;

  @Override
  public void onInitialize() {
    GeminiEvents.init();
    CuriosFabricRegistry.init();
    CuriosFabricConfig.load();
    CuriosIntegrations.setup();
    CuriosApi.setCuriosHelper(new CuriosHelper());

    NeoforgeCuriosSlotManager.buildImcSlotTypes();
    postExtensionsEvent();
    CuriosSelectorOptions.register();

    CuriosFabricEventHandler.register();
    NetworkHandler.register();

    GeminiEvents.listen(ServerLifecycleEvent.class, event -> {
      if (event.getStage() == ServerLifecycleEvent.Stage.STARTING) {
        TrinketsCompat.register();
        CuriosApi.setSlotHelper(new SlotHelper());
        Set<String> slotIds = new HashSet<>();

        for (ISlotType value : CuriosSlotManager.SERVER.getSlots().values()) {
          CuriosApi.getSlotHelper().addSlotType(value);
          slotIds.add(value.getIdentifier());
        }
        CurioArgumentType.slotIds = slotIds;
        NeoforgeCuriosCompat.register(slotIds);
      } else if (event.getStage() == ServerLifecycleEvent.Stage.STOPPED) {
        CuriosApi.setSlotHelper(null);
      }
    });

    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registry, environment) -> CuriosFabricCommand.register(dispatcher, registry));

    CuriosSlotManager.SERVER = new CuriosSlotManager();
    CuriosEntityManager.SERVER = new CuriosEntityManager();
    ResourceManagerHelper helper = ResourceManagerHelper.get(PackType.SERVER_DATA);
    helper.registerReloadListener(CuriosSlotManager.SERVER);
    helper.registerReloadListener(CuriosEntityManager.SERVER);
    helper.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
      @Override
      public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "post_reload");
      }

      @Override
      public Collection<ResourceLocation> getFabricDependencies() {
        return List.of(CuriosSlotManager.ID, CuriosEntityManager.ID);
      }

      @Override
      public void onResourceManagerReload(ResourceManager manager) {
        CuriosFabricEventHandler.dirtyTags = true;
        refreshCompatBridges();
      }
    });
  }

  /**
   * Re-reads the Trinkets and NeoForge Curios bridges. Called after the data pack tags have been
   * bound, which is the earliest point where the item tags are guaranteed to be available.
   */
  private static void refreshCompatBridges() {
    // Reading the entrypoints again is cheap and makes sure slot types contributed by mods that
    // were initialized after this mod are picked up as well.
    NeoforgeCuriosSlotManager.buildImcSlotTypes();
    TrinketsCompat.register();
    NeoforgeCuriosCompat.register(CuriosSlotManager.SERVER.getSlots().keySet());
  }

  /** Dispatches the extension registration event to the {@code curiosfabric} entrypoints and the bus. */
  private static void postExtensionsEvent() {
    RegisterCuriosExtensionsEvent event = new RegisterCuriosExtensionsEvent();

    for (EntrypointContainer<CuriosEntrypoint> container : FabricLoader.getInstance()
        .getEntrypointContainers("curiosfabric", CuriosEntrypoint.class)) {

      try {
        container.getEntrypoint().registerExtensions(event);
      } catch (Throwable throwable) {
        CuriosFabricConstants.LOG.error("Failed to read the curios entrypoint of {}",
            container.getProvider().getMetadata().getId(), throwable);
      }
    }
    CuriosEventBus.post(event);
  }

  public static String itemCacheKey(ItemStack stack) {
    return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() +
        (!stack.getComponents().isEmpty() ?
            stack.getComponents().stream().map(TypedDataComponent::toString)
                .reduce((s, s2) -> s + s2) : "");
  }

  /** {@code true} when the given mod is present, used for the optional integrations. */
  public static boolean isModLoaded(String modId) {
    return GeminiIntegrations.isModLoaded(modId);
  }
}
