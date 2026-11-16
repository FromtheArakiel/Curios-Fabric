package dev.arakiel.curiosfabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.client.CuriosRendererRegistry;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.compat.api.CompatibilityBridges;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;

public class CuriosLayer<T extends LivingEntity, M extends EntityModel<T>> extends
    RenderLayer<T, M> {

  private static final Set<String> REPORTED_RENDER_CALLS = ConcurrentHashMap.newKeySet();

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
              ISlotType slotType =
                  CuriosApi.getSlot(id, livingEntity.level()).orElse(null);
              SlotSource source =
                  slotType == null ? SlotSource.CURIOSFABRIC : slotType.getSource();

              if (source == SlotSource.CURIOSFABRIC
                  && CuriosRendererRegistry.getRenderer(stack.getItem()).isPresent()) {
                CuriosRendererRegistry.getRenderer(stack.getItem()).ifPresent(
                    renderer -> {
                      renderer.render(finalStack, slotContext, matrixStack, renderLayerParent,
                          renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks,
                          ageInTicks, netHeadYaw, headPitch);
                      reportCall(finalStack, slotContext, SlotSource.CURIOSFABRIC, true);
                    });
              } else {
                Object[] arguments = {
                    renderLayerParent.getModel(), renderLayerParent, matrixStack, renderTypeBuffer,
                    light, livingEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
                    netHeadYaw, headPitch};
                CompatibilityBridges.rendererFor(slotType, finalStack, true)
                    .ifPresent(bridge -> {
                      boolean rendered = bridge.render(finalStack, slotContext, arguments);
                      SlotSource rendererSource =
                          CompatibilityBridges.rendererSourceFor(slotType, finalStack, true)
                              .orElse(source);
                      reportCall(finalStack, slotContext, rendererSource, rendered);
                    });
              }
            }
          }
        }));
    matrixStack.popPose();
  }

  private static void reportCall(ItemStack stack, SlotContext context, SlotSource source,
                                 boolean rendered) {
    String key = stack.getItem() + "|" + context.identifier();
    if (REPORTED_RENDER_CALLS.add(key)) {
      CuriosFabricConstants.LOG.info(
          "CURIOS_RENDER_DEBUG call item={} slot={} index={} source={} rendered={}",
          stack.getItem(), context.identifier(), context.index(), source, rendered);
    }
  }

}
