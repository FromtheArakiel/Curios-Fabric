package dev.arakiel.curios.api.extensions;

import javax.annotation.Nonnull;
import dev.arakiel.curios.api.event.ICuriosEvent;

/**
 * Allows registration of new behavior to various game objects used by Curios. Fired during mod
 * initialization through the {@code curios} entrypoint and on the
 * {@link dev.arakiel.curios.api.event.CuriosEventBus}.
 */
public class RegisterCuriosExtensionsEvent implements ICuriosEvent {

  /**
   * Registers an {@link ICurioSlotExtension} instance to a list of slot identifiers.
   *
   * <p>A slot identifier cannot be associated with more than one slot extension instance.
   * Attempting to register duplicates will throw an error.
   *
   * @param extension The slot extension instance
   * @param slotIds The list of slot identifiers to be associated with the specified slot extension
   */
  public void registerSlotExtension(@Nonnull ICurioSlotExtension extension, String... slotIds) {
    CuriosExtensions.register(extension, slotIds);
  }

  /**
   * Checks if the slot identifier already has a registered slot extension.
   *
   * @param slotId The slot identifier
   * @return True if the slot identifier has a registered slot extension, otherwise false
   */
  public boolean isSlotExtensionRegistered(String slotId) {
    return CuriosExtensions.SLOT_EXTENSIONS.containsKey(slotId);
  }
}
