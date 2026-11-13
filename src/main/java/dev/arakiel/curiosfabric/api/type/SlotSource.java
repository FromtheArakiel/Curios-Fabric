package dev.arakiel.curiosfabric.api.type;

import java.util.Locale;
import java.util.Optional;

/** Identifies where a mirrored slot definition originated. */
public enum SlotSource {
  CURIOSFABRIC("curiosfabric", "curiosfabric.identifier."),
  NEOFORGE_CURIOS("neoforgecurios", "curios.identifier."),
  TRINKETS("trinkets", null);

  private final String id;
  private final String translationPrefix;

  SlotSource(String id, String translationPrefix) {
    this.id = id;
    this.translationPrefix = translationPrefix;
  }

  public String id() {
    return this.id;
  }

  /**
   * The conventional translation key for identifier-based sources.
   *
   * <p>Trinkets keys include the original {@code group/slot} path, so they are resolved by
   * {@code TrinketsCompat} instead of deriving a key from the generated Curios id.</p>
   */
  public Optional<String> translationKey(String slotId) {
    return this.translationPrefix == null
        ? Optional.empty()
        : Optional.of(this.translationPrefix + slotId);
  }

  public static SlotSource fromId(String id) {
    String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "neoforgecurios", "neoforge_curios", "neoforge" -> NEOFORGE_CURIOS;
      case "trinket", "trinkets" -> TRINKETS;
      default -> CURIOSFABRIC;
    };
  }
}
