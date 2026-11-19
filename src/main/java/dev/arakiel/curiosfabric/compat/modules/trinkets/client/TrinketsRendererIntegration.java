package dev.arakiel.curiosfabric.compat.modules.trinkets.client;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.gemini.integration.GeminiIntegration;
import dev.arakiel.gemini.integration.GeminiIntegrationType;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import java.util.Optional;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;

/** Strongly typed, client-only Trinkets renderer integration. */
@GeminiIntegration(value = "trinkets", type = GeminiIntegrationType.CLIENT)
public final class TrinketsRendererIntegration {

  private static volatile boolean active;

  public void applyClient() {
    active = true;
  }

  public static boolean hasRenderer(ItemStack stack) {
    return active
        && stack != null
        && !stack.isEmpty()
        && TrinketRendererRegistry.getRenderer(stack.getItem()).isPresent();
  }

  /**
   * Invokes the public Trinkets renderer interface instead of reflecting into arbitrary renderer
   * implementation classes, which may be package-private.
   */
  @SuppressWarnings("unchecked")
  public static boolean render(Object[] arguments) {
    if (!active || arguments.length < 13 || !(arguments[0] instanceof ItemStack stack)
        || !(arguments[1] instanceof SlotReference reference)
        || !(arguments[2] instanceof EntityModel<?> model)
        || !(arguments[3] instanceof PoseStack poseStack)
        || !(arguments[4] instanceof MultiBufferSource buffers)
        || !(arguments[5] instanceof Integer light)
        || !(arguments[6] instanceof LivingEntity entity)) {
      return false;
    }
    Optional<TrinketRenderer> resolved = TrinketRendererRegistry.getRenderer(stack.getItem());
    if (resolved.isEmpty()) {
      return false;
    }
    try {
      EntityModel<? extends LivingEntity> typedModel =
          (EntityModel<? extends LivingEntity>) model;
      resolved.get().render(
          stack,
          reference,
          typedModel,
          poseStack,
          buffers,
          light,
          entity,
          (Float) arguments[7],
          (Float) arguments[8],
          (Float) arguments[9],
          (Float) arguments[10],
          (Float) arguments[11],
          (Float) arguments[12]);
      return true;
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Failed to invoke a bridged Trinkets renderer", throwable);
      return false;
    }
  }
}
