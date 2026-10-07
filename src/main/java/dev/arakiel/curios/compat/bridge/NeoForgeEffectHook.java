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

package dev.arakiel.curios.compat.bridge;

import java.lang.reflect.Method;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.compat.CompatTargets;

/**
 * Consults the NeoForge mob effect hook that backs {@code MobEffectEvent.Applicable}.
 *
 * <p>On NeoForge the vanilla {@code canBeAffected} call inside {@code LivingEntity#addEffect} is
 * replaced by this hook, so Forge mods can veto an effect ("immune to poison" and friends). A cross
 * loader layer may only chain it onto the vanilla result instead of replacing it, which leaves the
 * event unposted whenever vanilla happens to allow the effect. Calling the hook here restores the
 * NeoForge behaviour for every accessory mod that relies on it.</p>
 *
 * <p>The hook is invoked reflectively so this class stays loadable without NeoForge.</p>
 */
public final class NeoForgeEffectHook {

  private static final String COMMON_HOOKS = "net.neoforged.neoforge.common.CommonHooks";

  private static volatile Method canBeApplied;
  private static volatile boolean unavailable;

  private NeoForgeEffectHook() {
  }

  /**
   * Asks NeoForge whether the effect may be applied.
   *
   * @return {@code true}/{@code false} according to the hook, or {@code null} when the hook cannot
   *     be consulted and the caller should keep the vanilla behaviour
   */
  public static Boolean canMobEffectBeApplied(LivingEntity entity, MobEffectInstance effectInstance,
                                              Entity source) {

    if (unavailable) {
      return null;
    }

    try {
      Method method = canBeApplied;

      if (method == null) {
        Class<?> hooks = CompatTargets.loadClass(COMMON_HOOKS);

        if (hooks == null) {
          return null;
        }
        method = hooks.getMethod("canMobEffectBeApplied", LivingEntity.class,
            MobEffectInstance.class, Entity.class);
        canBeApplied = method;
      }
      Object result = method.invoke(null, entity, effectInstance, source);
      return result instanceof Boolean value ? value : null;
    } catch (Throwable throwable) {
      unavailable = true;
      CuriosConstants.LOG.warn("Could not consult the NeoForge mob effect hook", throwable);
      return null;
    }
  }
}
