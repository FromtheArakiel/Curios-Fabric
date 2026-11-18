package dev.arakiel.curiosfabric.compat.modules.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.contribution.render.RendererQuery;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;

/** Strongly typed Accessories renderer adapter. */
final class AccessoriesRendererBackend {

  private AccessoriesRendererBackend() {
  }

  static void reloadRendererCache() {
    AccessoriesRendererRegistry.onReload();
  }

  static void invalidate() {
    // Accessories owns this cache. It is rebuilt by its own client reload hook.
  }

  static boolean render(RendererQuery query) {
    SlotReference reference = AccessoriesCompat.referenceOrFallback(
        query.slotContext(), query.stack());
    if (reference == null) {
      return false;
    }
    AccessoryRenderer renderer = AccessoriesRendererRegistry.getRenderer(query.stack());
    if (!renderer.shouldRender(query.slotContext().visible())) {
      return true;
    }
    render(renderer, query, AccessoriesBridge.rendererArguments(query, reference));
    return true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void render(
      AccessoryRenderer renderer,
      RendererQuery query,
      Object[] arguments) {
    SlotReference reference = (SlotReference) arguments[1];
    EntityModel model = (EntityModel) arguments[3];
    PoseStack poseStack = (PoseStack) arguments[2];
    MultiBufferSource buffer = (MultiBufferSource) arguments[4];
    renderer.render(
        query.stack(),
        reference,
        poseStack,
        model,
        buffer,
        (Integer) arguments[5],
        (Float) arguments[6],
        (Float) arguments[7],
        (Float) arguments[8],
        (Float) arguments[9],
        (Float) arguments[10],
        (Float) arguments[11]);
  }
}
