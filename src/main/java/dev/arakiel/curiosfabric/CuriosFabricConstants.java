package dev.arakiel.curiosfabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuriosFabricConstants {

  /** The namespace of this mod: assets, data ({@code data/curiosfabric/...}), registries, channels. */
  public static final String MOD_ID = "curiosfabric";
  /** Same as {@link #MOD_ID}, kept as an alias for readability. */
  public static final String MODID = MOD_ID;
  /**
   * The data format namespace of the original NeoForge Curios edition:
   * {@code #curios:<slot>} item tags, the {@code data/<namespace>/curios/slots|entities} paths and
   * the {@code curios:<validator>} ids.
   *
   * <p>This mod uses {@link #MOD_ID} for its own data format. Only the NeoForge Curios compatibility
   * module owns behavior associated with this upstream namespace.</p>
   */
  public static final String NEOFORGE_CURIOS_DATA_NAMESPACE = "curios";
  public static final String MOD_NAME = "Curios Fabric";
  public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

  /** True on the physical client. */
  public static final boolean IS_CLIENT = detectClientEnvironment();

  private static boolean detectClientEnvironment() {
    try {
      return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    } catch (Throwable ignored) {
      return false;
    }
  }
}
