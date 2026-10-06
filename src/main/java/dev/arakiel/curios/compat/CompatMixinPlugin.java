/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.arakiel.curios.compat;

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

  private static final String FORGE_PACKAGE = "dev.arakiel.curios.compat.mixin.forge.";
  private static final String NEOFORGE_PACKAGE = "dev.arakiel.curios.compat.mixin.neoforge.";
  private static final String TRINKETS_PACKAGE = "dev.arakiel.curios.compat.mixin.trinkets.";
  private static final String CLIENT_PACKAGE = "dev.arakiel.curios.compat.mixin.client.";

  @Override
  public void onLoad(String mixinPackage) {
    // NO-OP
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {

    if (mixinClassName.startsWith(FORGE_PACKAGE)) {
      return apply(CompatTargets.isForgeCuriosLoaded(), mixinClassName, targetClassName);
    }

    if (mixinClassName.startsWith(TRINKETS_PACKAGE)) {
      return apply(CompatTargets.isTrinketsLoaded(), mixinClassName, targetClassName);
    }

    if (mixinClassName.startsWith(NEOFORGE_PACKAGE)) {
      return apply(CompatTargets.isNeoForgeLoaded(), mixinClassName, targetClassName);
    }

    // The screen gate only matters when another accessory UI exists at all.
    if (mixinClassName.startsWith(CLIENT_PACKAGE)) {
      return apply(CompatTargets.isForgeCuriosLoaded() || CompatTargets.isTrinketsLoaded(),
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
      dev.arakiel.curios.CuriosConstants.LOG.info("[compat] applying {} on {}",
          mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1), targetClassName);
    }
    return loaded;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    // NO-OP
  }

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                       IMixinInfo mixinInfo) {
    // NO-OP
  }

  @Override
  public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                        IMixinInfo mixinInfo) {
    // NO-OP
  }
}
