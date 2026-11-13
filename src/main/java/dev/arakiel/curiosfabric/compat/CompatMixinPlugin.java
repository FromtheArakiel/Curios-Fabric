package dev.arakiel.curiosfabric.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Gates the compatibility mixins, the same way {@code accessories-compat-layer} does it: a mixin is
 * only applied when its target mod is present <em>and</em> the target class really exists, so a
 * missing or renamed target can never break the game.
 */
public class CompatMixinPlugin implements IMixinConfigPlugin {

  private static final String NEOFORGE_PACKAGE =
      "dev.arakiel.curiosfabric.compat.mixin.neoforge.";
  private static final String NEOFORGE_CURIOS_PACKAGE =
      "dev.arakiel.curiosfabric.compat.mixin.neoforgecurios.";
  private static final String TRINKETS_PACKAGE = "dev.arakiel.curiosfabric.compat.mixin.trinkets.";
  private static final String CLIENT_PACKAGE = "dev.arakiel.curiosfabric.compat.mixin.client.";

  @Override
  public void onLoad(String mixinPackage) {
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

    if (mixinClassName.startsWith(NEOFORGE_CURIOS_PACKAGE)) {
      return apply(CompatTargets.isNeoforgeCuriosLoaded(), mixinClassName, targetClassName);
    }

    if (mixinClassName.startsWith(NEOFORGE_PACKAGE)) {
      return apply(CompatTargets.isNeoforgeLoaded(), mixinClassName, targetClassName);
    }

    if (mixinClassName.startsWith(TRINKETS_PACKAGE)) {
      return apply(CompatTargets.isTrinketsLoaded(), mixinClassName, targetClassName);
    }

    // The screen gate only matters when another accessory UI exists at all.
    if (mixinClassName.startsWith(CLIENT_PACKAGE)) {
      return apply(CompatTargets.isNeoforgeCuriosLoaded() || CompatTargets.isTrinketsLoaded(),
          mixinClassName, targetClassName);
    }
    return true;
  }

  /**
   * Decides whether a compatibility mixin is applied.
   *
   * <p>The check is done on the mod presence only: a class check through this class loader is not
   * reliable, because the plugin runs before the target mod's classes are reachable. The mixin
   * config is not required, so a target that does not exist only logs a warning.</p>
   */
  private static boolean apply(boolean loaded, String mixinClassName, String targetClassName) {

    if (loaded) {
      dev.arakiel.curiosfabric.CuriosFabricConstants.LOG.info("[compat] applying {} on {}",
          mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1), targetClassName);
    }
    return loaded;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
  }

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                       IMixinInfo mixinInfo) {
  }

  @Override
  public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                        IMixinInfo mixinInfo) {
  }
}
