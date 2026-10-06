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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.client.CuriosRendererRegistry;
import dev.arakiel.curios.client.ClientEventHandler;
import dev.arakiel.curios.client.CuriosClientConfig;
import dev.arakiel.curios.client.IconHelper;
import dev.arakiel.curios.client.KeyRegistry;
import dev.arakiel.curios.client.gui.CuriosScreen;
import dev.arakiel.curios.client.gui.GuiEventHandler;
import dev.arakiel.curios.client.render.CuriosLayer;
import dev.arakiel.curios.common.CuriosRegistry;
import dev.arakiel.curios.common.network.client.CuriosClientPackets;

/**
 * Client entry point of the Fabric build.
 *
 * <p>Mirrors the NeoForge {@code ClientProxy}: key bindings, the curio menu screen, the client event
 * handlers and the player render layer. The render layer is added through Fabric's
 * {@code LivingEntityFeatureRendererRegistrationCallback} instead of the NeoForge renderer event.</p>
 */
public class CuriosClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    CuriosClientConfig.load();
    CuriosApi.setIconHelper(new IconHelper());

    KeyBindingHelper.registerKeyBinding(KeyRegistry.openCurios);
    ClientEventHandler.register();
    GuiEventHandler.register();
    CuriosClientPackets.registerReceivers();

    LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
        (entityType, entityRenderer, registrationHelper, context) -> {
          if (entityRenderer instanceof PlayerRenderer playerRenderer) {
            //noinspection unchecked,rawtypes
            registrationHelper.register(new CuriosLayer<>(playerRenderer));
          }
        });

    CuriosRendererRegistry.load();
    MenuScreens.register(CuriosRegistry.CURIO_MENU.get(), CuriosScreen::new);
  }
}
