package dev.arakiel.curiosfabric.common.integration;

/**
 * Optional third party integrations.
 *
 * <p>The Neoforge edition hooked EMI through the Neoforge event bus. On Fabric the equivalent EMI
 * entrypoint is available, but EMI is not one of the compile time dependencies of this project, so
 * the hook is not shipped. JEI is still fully supported through the {@code jei_mod_plugin}
 * entrypoint, and REI/EMI users only lose the extra "exclusion area" around the Curios panel.</p>
 */
public class CuriosIntegrations {

  private CuriosIntegrations() {
  }

  /** Called once during mod initialization. */
  public static void setup() {
  }
}
