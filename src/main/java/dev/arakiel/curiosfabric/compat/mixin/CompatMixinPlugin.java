package dev.arakiel.curiosfabric.compat.mixin;

import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Gates upstream-specific mixins by detected mod presence. Common compatibility mixins always
 * apply and become no-ops when no bridge contributes data.
 */
public class CompatMixinPlugin implements IMixinConfigPlugin {

  private static final String NEOFORGE_CURIOS_PACKAGE =
      "dev.arakiel.curiosfabric.compat.modules.neoforgecurios.mixin.";
  private static final String TRINKETS_PACKAGE =
      "dev.arakiel.curiosfabric.compat.modules.trinkets.mixin.";
  private static final String ACCESSORIES_PACKAGE =
      "dev.arakiel.curiosfabric.compat.modules.accessories.mixin.";
  private static final String CLIENT_PACKAGE =
      "dev.arakiel.curiosfabric.compat.mixin.client.";
  private static final String NEOFORGE_EFFECT_MIXIN =
      "dev.arakiel.curiosfabric.compat.modules.neoforgecurios.mixin."
          + "NeoforgeEffectApplicableMixin";

  @Override
  public void onLoad(String mixinPackage) {
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

    if (mixinClassName.equals(NEOFORGE_EFFECT_MIXIN)) {
      return apply(CompatTargets.isNeoforgeLoaded(), mixinClassName, targetClassName);
    }

    if (mixinClassName.startsWith(NEOFORGE_CURIOS_PACKAGE)) {
      return apply(CompatTargets.isNeoforgeCuriosLoaded(), mixinClassName, targetClassName);
    }

    if (mixinClassName.startsWith(TRINKETS_PACKAGE)) {
      return apply(CompatTargets.isTrinketsLoaded(), mixinClassName, targetClassName);
    }

    if (mixinClassName.startsWith(ACCESSORIES_PACKAGE)) {
      return apply(CompatTargets.isAccessoriesLoaded(), mixinClassName, targetClassName);
    }

    // The screen gate only matters when another accessory UI exists at all.
    if (mixinClassName.startsWith(CLIENT_PACKAGE)) {
      return apply(CompatTargets.isNeoforgeCuriosLoaded()
              || CompatTargets.isTrinketsLoaded()
              || CompatTargets.isAccessoriesLoaded(),
          mixinClassName, targetClassName);
    }
    return true;
  }

  /**
   * Decides whether a compatibility mixin is applied.
   *
   * <p>The check uses mod presence because the plugin runs before upstream classes are reliably
   * reachable.</p>
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
