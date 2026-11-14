package dev.arakiel.curiosfabric.compat.api;

/** Refreshes slot metadata from one compatibility target. */
public interface SlotMetadataBridge {

  void refresh(String reason, boolean authoritative);
}
