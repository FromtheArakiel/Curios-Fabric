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

package dev.arakiel.curios;

import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.CuriosEntrypoint;
import dev.arakiel.curios.api.event.CuriosEventBus;
import dev.arakiel.curios.api.extensions.RegisterCuriosExtensionsEvent;
import dev.arakiel.curios.api.type.ISlotType;
import dev.arakiel.curios.common.CuriosConfig;
import dev.arakiel.curios.common.CuriosHelper;
import dev.arakiel.curios.common.CuriosRegistry;
import dev.arakiel.curios.common.data.CuriosEntityManager;
import dev.arakiel.curios.common.data.CuriosSlotManager;
import dev.arakiel.curios.common.event.CuriosEventHandler;
import dev.arakiel.curios.common.integration.CuriosIntegrations;
import dev.arakiel.curios.common.network.NetworkHandler;
import dev.arakiel.curios.common.slottype.LegacySlotManager;
import dev.arakiel.curios.compat.forge.ForgeCuriosCompat;
import dev.arakiel.curios.compat.trinkets.TrinketsCompat;
import dev.arakiel.curios.server.SlotHelper;
import dev.arakiel.curios.server.command.CurioArgumentType;
import dev.arakiel.curios.server.command.CuriosCommand;
import dev.arakiel.curios.server.command.CuriosSelectorOptions;

/**
 * Entry point of the Fabric build.
 *
 * <p>Mirrors the responsibilities of the NeoForge entry point: registry bootstrap, the helper and
 * slot helper lifecycles, command registration, the reload listeners for slot/entity data and the
 * optional integrations. NeoForge's capability registration and event bus listeners are replaced by
 * the Fabric implementations in {@code dev.arakiel.curios.platform}, by the handlers in
 * {@code CuriosEventHandler} and by the mixins in {@code dev.arakiel.curios.mixin}.</p>
 */
public class Curios implements ModInitializer {

  public static final String MOD_ID = CuriosConstants.MOD_ID;

  @Override
  public void onInitialize() {
    CuriosRegistry.init();
    CuriosConfig.load();
    CuriosIntegrations.setup();
    CuriosApi.setCuriosHelper(new CuriosHelper());

    LegacySlotManager.buildImcSlotTypes();
    postExtensionsEvent();
    CuriosSelectorOptions.register();

    CuriosEventHandler.register();
    NetworkHandler.register();

    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      CuriosApi.setSlotHelper(new SlotHelper());
      Set<String> slotIds = new HashSet<>();

      for (ISlotType value : CuriosSlotManager.SERVER.getSlots().values()) {
        CuriosApi.getSlotHelper().addSlotType(value);
        slotIds.add(value.getIdentifier());
      }
      CurioArgumentType.slotIds = slotIds;
      TrinketsCompat.register();
      ForgeCuriosCompat.register(slotIds);
    });
    ServerLifecycleEvents.SERVER_STOPPED.register(server -> CuriosApi.setSlotHelper(null));

    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registry, environment) -> CuriosCommand.register(dispatcher, registry));

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
        CuriosEventHandler.dirtyTags = true;
        refreshCompatBridges();
      }
    });
  }

  /**
   * Re-reads the Trinkets and legacy Forge bridges. Called after the data pack tags have been
   * bound, which is the earliest point where the item tags are guaranteed to be available.
   */
  private static void refreshCompatBridges() {
    // Reading the entrypoints again is cheap and makes sure slot types contributed by mods that
    // were initialized after this mod are picked up as well.
    LegacySlotManager.buildImcSlotTypes();
    TrinketsCompat.register();
    ForgeCuriosCompat.register(CuriosSlotManager.SERVER.getSlots().keySet());
  }

  /** Dispatches the extension registration event to the {@code curios} entrypoints and the bus. */
  private static void postExtensionsEvent() {
    RegisterCuriosExtensionsEvent event = new RegisterCuriosExtensionsEvent();

    for (EntrypointContainer<CuriosEntrypoint> container : FabricLoader.getInstance()
        .getEntrypointContainers("curios", CuriosEntrypoint.class)) {

      try {
        container.getEntrypoint().registerExtensions(event);
      } catch (Throwable throwable) {
        CuriosConstants.LOG.error("Failed to read the curios entrypoint of {}",
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
    return FabricLoader.getInstance().isModLoaded(modId);
  }
}
