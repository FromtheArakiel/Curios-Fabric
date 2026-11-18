package dev.arakiel.curiosfabric.compat.contribution.slot;

import dev.arakiel.curiosfabric.api.type.SlotSource;

/** A data-directory owned by a upstream-originated slot source. */
public record SlotDataDirectory(String path, SlotSource source, boolean nativeMapping) {

  public SlotDataDirectory {
    if (path == null || path.isBlank() || source == null) {
      throw new IllegalArgumentException("slot data directory fields must not be blank");
    }
  }
}
