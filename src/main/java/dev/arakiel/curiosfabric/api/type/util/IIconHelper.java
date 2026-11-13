package dev.arakiel.curiosfabric.api.type.util;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import dev.arakiel.curiosfabric.api.type.ISlotType;

/**
 * @deprecated Moved to internal code
 */
@Deprecated(forRemoval = true, since = "1.20.1")
@ApiStatus.ScheduledForRemoval(inVersion = "1.22")
public interface IIconHelper {

  /**
   * @deprecated Moved to internal code and removed from the API
   */
  @Deprecated(forRemoval = true, since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  void clearIcons();

  /**
   * @deprecated Moved to internal code and removed from the API
   */
  @Deprecated(forRemoval = true, since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  void addIcon(String identifier, ResourceLocation resourceLocation);

  /**
   * @deprecated See {@link ISlotType#getIcon()}.
   */
  @Deprecated(forRemoval = true, since = "1.20.1")
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  ResourceLocation getIcon(String identifier);
}
