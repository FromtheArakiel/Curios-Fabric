package dev.arakiel.curiosfabric.api.type;

import java.util.Locale;
import java.util.Optional;

/** Identifies where a mirrored slot definition originated. */
public enum SlotSource {
  CURIOSFABRIC("curiosfabric", "curiosfabric.identifier.", 10, true, false),
  NEOFORGE_CURIOS("neoforgecurios", "curios.identifier.", 20, true, true),
  TRINKETS("trinkets", null, 30, false, false),
  ACCESSORIES("accessories", "accessories.slot.", 40, false, false);

  private final String id;
  private final String translationPrefix;
  private final int queueRank;
  private final boolean usesOrderAnchors;
  private final boolean usesDirectTagAliases;

  SlotSource(String id, String translationPrefix, int queueRank, boolean usesOrderAnchors,
             boolean usesDirectTagAliases) {
    this.id = id;
    this.translationPrefix = translationPrefix;
    this.queueRank = queueRank;
    this.usesOrderAnchors = usesOrderAnchors;
    this.usesDirectTagAliases = usesDirectTagAliases;
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

  public int queueRank() {
    return queueRank;
  }

  public boolean usesOrderAnchors() {
    return usesOrderAnchors;
  }

  public boolean usesDirectTagAliases() {
    return usesDirectTagAliases;
  }

  public static SlotSource fromId(String id) {
    String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "neoforgecurios", "neoforge_curios", "neoforge" -> NEOFORGE_CURIOS;
      case "trinket", "trinkets" -> TRINKETS;
      case "accessories" -> ACCESSORIES;
      default -> CURIOSFABRIC;
    };
  }
}
