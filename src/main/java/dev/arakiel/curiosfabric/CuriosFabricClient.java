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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.client.CuriosRendererRegistry;
import dev.arakiel.curiosfabric.client.ClientEventHandler;
import dev.arakiel.curiosfabric.client.CuriosFabricClientConfig;
import dev.arakiel.curiosfabric.client.IconHelper;
import dev.arakiel.curiosfabric.client.KeyRegistry;
import dev.arakiel.curiosfabric.client.gui.CuriosScreen;
import dev.arakiel.curiosfabric.client.gui.GuiEventHandler;
import dev.arakiel.curiosfabric.client.render.CuriosLayer;
import dev.arakiel.curiosfabric.common.CuriosFabricRegistry;

/**
 * Client entry point of the Fabric build.
 *
 * <p>Mirrors the Neoforge {@code ClientProxy}: key bindings, the curio menu screen, the client event
 * handlers and the player render layer. The render layer is added through Fabric's
 * {@code LivingEntityFeatureRendererRegistrationCallback} instead of the Neoforge renderer event.</p>
 */
public class CuriosFabricClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    CuriosFabricClientConfig.load();
    CuriosApi.setIconHelper(new IconHelper());

    KeyBindingHelper.registerKeyBinding(KeyRegistry.openCurios);
    ClientEventHandler.register();
    GuiEventHandler.register();
    LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
        (entityType, entityRenderer, registrationHelper, context) -> {
          if (entityRenderer instanceof PlayerRenderer playerRenderer) {
            //noinspection unchecked,rawtypes
            registrationHelper.register(new CuriosLayer<>(playerRenderer));
          }
        });

    CuriosRendererRegistry.load();
    MenuScreens.register(CuriosFabricRegistry.CURIO_MENU.get(), CuriosScreen::new);
  }
}
