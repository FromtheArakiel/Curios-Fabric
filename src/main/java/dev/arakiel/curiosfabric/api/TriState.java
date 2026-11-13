package dev.arakiel.curiosfabric.api;

/**
 * A three valued result: {@link #TRUE}, {@link #FALSE} and {@link #DEFAULT}, where {@code DEFAULT}
 * means "let the normal rules decide".
 *
 * <p>The Neoforge edition uses Neoforge's {@code TriState} for its equip/unequip events. Since that
 * class is not available on Fabric, the equivalent enum lives here.</p>
 */
public enum TriState {
  TRUE,
  FALSE,
  DEFAULT
}
