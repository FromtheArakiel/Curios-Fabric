package dev.arakiel.curiosfabric.api;

import dev.arakiel.curiosfabric.api.extensions.RegisterCuriosExtensionsEvent;
import java.util.function.Consumer;

/**
 * Fabric entrypoint of the Curios API, declared with the {@code curiosfabric} key in
 * {@code fabric.mod.json}.
 *
 * <p>It replaces the two mechanisms the Neoforge edition offered: the inter mod communication
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
  }

  /**
   * Called once during mod initialization so existing slot types can be modified.
   *
   * @param registrar Accepts the slot type messages of this mod
   */
  default void modifySlots(Consumer<SlotTypeMessage> registrar) {
  }

  /**
   * Called once during mod initialization so slot extensions can be registered.
   *
   * @param event The extension registration event
   */
  default void registerExtensions(RegisterCuriosExtensionsEvent event) {
  }
}
