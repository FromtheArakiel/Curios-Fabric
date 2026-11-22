package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.asm.NeoforgeCuriosAsm;
import dev.arakiel.curiosfabric.compat.shared.detection.CompatTargets;

/**
 * Consults the NeoForge mob effect hook that backs {@code MobEffectEvent.Applicable}.
 *
 * <p>On NeoForge the vanilla {@code canBeAffected} call inside {@code LivingEntity#addEffect} is
 * replaced by this hook, so NeoForge mods can veto an effect ("immune to poison" and friends). A cross
 * loader layer may only chain it onto the vanilla result instead of replacing it, which leaves the
 * event unposted whenever vanilla happens to allow the effect. Calling the hook here restores the
 * NeoForge behaviour for every accessory mod that relies on it.</p>
 *
 * <p>The hook is invoked reflectively so this class stays loadable without NeoForge.</p>
 */
public final class NeoforgeEffectHook {

  private static final String COMMON_HOOKS = "net.neoforged.neoforge.common.CommonHooks";

  private static volatile NeoforgeCuriosAsm.Invoker canBeApplied;
  private static volatile boolean unavailable;

  private NeoforgeEffectHook() {
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
      Object[] args = {entity, effectInstance, source};
      NeoforgeCuriosAsm.Invoker invoker = canBeApplied;
      if (invoker == null) {
        Class<?> hooks = CompatTargets.loadClass(COMMON_HOOKS);

        if (hooks == null) {
          return null;
        }
        invoker = NeoforgeCuriosAsm.staticInvoker(
            hooks, "canMobEffectBeApplied", args);
        canBeApplied = invoker;
      }
      Object result = invoker.invoke(null, args);
      return result instanceof Boolean value ? value : null;
    } catch (Throwable throwable) {
      unavailable = true;
      CuriosFabricConstants.LOG.warn("Could not consult the NeoForge mob effect hook", throwable);
      return null;
    }
  }
}
