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

package dev.arakiel.curios.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import dev.arakiel.curios.client.CuriosClientConfig;
import dev.arakiel.curios.common.network.client.CPacketDestroy;

/**
 * Adds the Curios button and the shift-destroy shortcut to the vanilla inventory screens.
 *
 * <p>NeoForge's {@code ScreenEvent} family is replaced by Fabric's screen events. The events are
 * global in NeoForge, so the Fabric counterparts are registered per screen once it is initialized,
 * which also keeps the listener count low.</p>
 */
public class GuiEventHandler {

  private GuiEventHandler() {
  }

  public static void register() {
    ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {

      if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
        ScreenEvents.beforeRender(screen).register(
            (renderedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> onRender(renderedScreen,
                mouseX, mouseY));
        ScreenMouseEvents.beforeMouseClick(screen).register(
            (clickedScreen, mouseX, mouseY, button) -> onMouseClick(clickedScreen, mouseX, mouseY));
        onInit(screen);
      }
    });
  }

  private static void onInit(Screen screen) {

    if (!CuriosClientConfig.CLIENT.enableButton.get()) {
      return;
    }
    AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) screen;
    boolean isCreative = screen instanceof CreativeModeInventoryScreen;
    Tuple<Integer, Integer> offsets = CuriosScreen.getButtonOffset(isCreative);
    int x = offsets.getA();
    int y = offsets.getB();
    int size = isCreative ? 8 : 10;
    int yOffset = isCreative ? 67 : 81;
    addWidget(gui,
        new CuriosButton(gui, gui.leftPos + x - 2, gui.topPos + y + yOffset, size, size,
            isCreative ? CuriosButton.SMALL : CuriosButton.BIG));
  }

  private static void addWidget(Screen screen, AbstractWidget widget) {
    screen.children.add(widget);
    screen.renderables.add(widget);
    screen.narratables.add(widget);
  }

  private static void onRender(Screen screen, int mouseX, int mouseY) {

    if (!(screen instanceof InventoryScreen gui)) {
      return;
    }
    gui.xMouse = mouseX;
    gui.yMouse = mouseY;
  }

  private static void onMouseClick(Screen screen, double mouseX, double mouseY) {
    long handle = Minecraft.getInstance().getWindow().getWindow();
    boolean isLeftShiftDown = InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_SHIFT);
    boolean isRightShiftDown = InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
    boolean isShiftDown = isLeftShiftDown || isRightShiftDown;

    if (!(screen instanceof CreativeModeInventoryScreen gui) || !isShiftDown) {
      return;
    }

    if (!gui.isInventoryOpen()) {
      return;
    }
    Slot destroyItemSlot = gui.destroyItemSlot;
    Slot slot = gui.findSlot(mouseX, mouseY);

    if (destroyItemSlot != null && slot == destroyItemSlot) {
      ClientPlayNetworking.send(new CPacketDestroy());
    }
  }
}
