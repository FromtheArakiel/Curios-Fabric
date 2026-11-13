package dev.arakiel.curiosfabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nonnull;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.client.CuriosRendererRegistry;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;

public class CuriosLayer<T extends LivingEntity, M extends EntityModel<T>> extends
    RenderLayer<T, M> {

  private final RenderLayerParent<T, M> renderLayerParent;

  public CuriosLayer(RenderLayerParent<T, M> renderer) {
    super(renderer);
    this.renderLayerParent = renderer;
  }

  @Override
  public void render(@Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource renderTypeBuffer,
                     int light, @Nonnull T livingEntity, float limbSwing, float limbSwingAmount,
                     float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
    matrixStack.pushPose();
    CuriosApi.getCuriosInventory(livingEntity)
        .ifPresent(handler -> handler.getCurios().forEach((id, stacksHandler) -> {
          IDynamicStackHandler stackHandler = stacksHandler.getStacks();
          IDynamicStackHandler cosmeticStacksHandler = stacksHandler.getCosmeticStacks();

          for (int i = 0; i < stackHandler.getSlots(); i++) {
            ItemStack stack = cosmeticStacksHandler.getStackInSlot(i);
            boolean cosmetic = true;
            NonNullList<Boolean> renderStates = stacksHandler.getRenders();
            boolean renderable = renderStates.size() > i && renderStates.get(i);

            if (stack.isEmpty() && renderable) {
              stack = stackHandler.getStackInSlot(i);
              cosmetic = false;
            }

            if (!stack.isEmpty()) {
              SlotContext slotContext = new SlotContext(id, livingEntity, i, cosmetic, renderable);
              ItemStack finalStack = stack;

              if (CuriosRendererRegistry.getRenderer(stack.getItem()).isPresent()) {
                CuriosRendererRegistry.getRenderer(stack.getItem()).ifPresent(
                    renderer -> renderer
                        .render(finalStack, slotContext, matrixStack, renderLayerParent,
                            renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks,
                            ageInTicks, netHeadYaw, headPitch));
              } else if (dev.arakiel.curiosfabric.compat.neoforgecurios.NeoforgeCuriosCompat.isLoaded()) {
                // Neoforge content mods register their renderers in the Neoforge edition's registry: run
                // theirs through the compatibility bridge so the item still shows on the body.
                if (!dev.arakiel.curiosfabric.compat.bridge.NeoforgeCuriosApiBridge.renderForeign(
                    new Object[]{finalStack, slotContext, matrixStack, renderLayerParent,
                        renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks,
                        ageInTicks, netHeadYaw, headPitch})) {
                  renderTrinketsRenderer(id, finalStack, slotContext, i, livingEntity, matrixStack,
                      renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
                      netHeadYaw, headPitch);
                }
              } else {
                renderTrinketsRenderer(id, finalStack, slotContext, i, livingEntity, matrixStack,
                    renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
                    netHeadYaw, headPitch);
              }
            }
          }
        }));
    matrixStack.popPose();
  }

  /**
   * Runs a renderer that was registered with Trinkets, if Trinkets is installed.
   *
   * <p>Trinkets' contract for 1.21.1 is {@code render(stack, slotReference, entityModel,
   * matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta,
   * animationProgress, headYaw, headPitch)}. The slot reference is fabricated by the bridge, so
   * renderers that inspect their slot keep working.</p>
   */
  private void renderTrinketsRenderer(String identifier, ItemStack stack, SlotContext slotContext,
                                      int index, T livingEntity, PoseStack matrixStack,
                                      MultiBufferSource renderTypeBuffer, int light,
                                      float limbSwing, float limbSwingAmount, float partialTicks,
                                      float ageInTicks, float netHeadYaw, float headPitch) {

    if (!dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat.isLoaded()) {
      return;
    }
    dev.arakiel.curiosfabric.compat.bridge.TrinketsApiBridge.renderForeign(
        new Object[]{stack,
            dev.arakiel.curiosfabric.compat.bridge.TrinketsApiBridge.slotReference(identifier, index,
                livingEntity, stack),
            this.renderLayerParent.getModel(), matrixStack, renderTypeBuffer, light, livingEntity,
            limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch});
  }
}
