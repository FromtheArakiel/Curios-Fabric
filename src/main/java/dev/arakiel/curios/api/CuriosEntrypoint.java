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

import dev.arakiel.curios.api.extensions.RegisterCuriosExtensionsEvent;
import java.util.function.Consumer;

/**
 * Fabric entrypoint of the Curios API, declared with the {@code curios} key in
 * {@code fabric.mod.json}.
 *
 * <p>It replaces the two mechanisms the NeoForge edition offered: the inter mod communication
 * channels {@code register_curio} / {@code modify_curio} used to create or modify slot types, and
 * the {@link RegisterCuriosExtensionsEvent} used to attach slot extensions.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * public class MyCuriosEntrypoint implements CuriosEntrypoint {
 *   @Override
 *   public void createSlots(Consumer<SlotTypeMessage> registrar) {
 *     registrar.accept(new SlotTypeMessage.Builder("pocket").size(2).build());
 *   }
 * }
 * }</pre>
 */
public interface CuriosEntrypoint {

  /**
   * Called once during mod initialization so new slot types can be contributed.
   *
   * @param registrar Accepts the slot type messages of this mod
   */
  default void createSlots(Consumer<SlotTypeMessage> registrar) {
    // NO-OP
  }

  /**
   * Called once during mod initialization so existing slot types can be modified.
   *
   * @param registrar Accepts the slot type messages of this mod
   */
  default void modifySlots(Consumer<SlotTypeMessage> registrar) {
    // NO-OP
  }

  /**
   * Called once during mod initialization so slot extensions can be registered.
   *
   * @param event The extension registration event
   */
  default void registerExtensions(RegisterCuriosExtensionsEvent event) {
    // NO-OP
  }
}
