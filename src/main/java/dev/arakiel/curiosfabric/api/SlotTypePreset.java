package dev.arakiel.curiosfabric.api;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.SlotTypeMessage.Builder;

/**
 * These slot type presets should be used whenever applicable by calling {@link
 * SlotTypePreset#getMessageBuilder()} and building off that in the IMC message
 *
 * @see <a href="https://docs.illusivesoulworks.com/category/curios">Curios Documentation</a>
 * @deprecated Use the datapack-based approach to slot registration instead of IMC
 */
@Deprecated(forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.22")
public enum SlotTypePreset {
  HEAD("head", 40), NECKLACE("necklace", 60), BACK("back", 80), BODY("body", 100), BRACELET(
      "bracelet", 120), HANDS("hands", 140), RING("ring", 160), BELT("belt", 180), CHARM("charm",
      200), CURIO("curio", 20);

  final String id;
  final int priority;

  SlotTypePreset(String id, int priority) {
    this.id = id;
    this.priority = priority;
  }

  /**
   * Attempts to find a valid preset for the given identifier and returns {@link Optional#empty()}
   * if none is found.
   *
   * @param id The {@link dev.arakiel.curiosfabric.api.type.ISlotType} identifier
   * @return {@link Optional} wrapper for the found preset or {@link Optional#empty()}
   */
  @Deprecated(forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public static Optional<SlotTypePreset> findPreset(String id) {
    try {
      return Optional.of(SlotTypePreset.valueOf(id.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Deprecated(forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public String getIdentifier() {
    return this.id;
  }

  @Deprecated(forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  public Builder getMessageBuilder() {
    return new Builder(this.id).priority(this.priority).icon(
        ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID,
            "slot/empty_" + this.getIdentifier() + "_slot"));
  }
}
