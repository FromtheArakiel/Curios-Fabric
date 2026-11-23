package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes CuriosFabric's render layer the single owner of Trinkets item rendering.
 *
 * <p>Trinkets installs its own renderer on every living entity renderer. Through the bridged
 * component it sees the same equipped stacks as {@code CuriosLayer}, but it has no access to the
 * Curios per-slot render flags. Leaving it active for players makes a hidden item reappear and also
 * renders visible items twice. Non-player rendering stays with Trinkets because CuriosLayer is
 * currently registered only on player renderers. The registered renderer itself remains available
 * to the bridge.</p>
 */
@Mixin(targets = "dev.emi.trinkets.TrinketFeatureRenderer")
public abstract class TrinketFeatureRendererMixin {

  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$useCuriosRenderLayerForPlayers(
      PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
      float limbAngle, float limbDistance, float tickDelta, float animationProgress,
      float headYaw, float headPitch, CallbackInfo ci) {
    if (entity instanceof Player) {
      ci.cancel();
    }
  }
}
