/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curios.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nonnull;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.client.CuriosRendererRegistry;
import dev.arakiel.curios.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curios.compat.Diag;

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
    Diag.once("layer:" + livingEntity.getType(),
        "the curios render layer runs for {}", livingEntity.getType());
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
              String itemId =
                  net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
                      .toString();
              Diag.once("seen:" + id + ":" + itemId,
                  "curio slot {} holds {} (cosmetic={}, renderable={})", id, itemId, cosmetic,
                  renderable);
              boolean rendered = false;

              if (CuriosRendererRegistry.getRenderer(stack.getItem()).isPresent()) {
                CuriosRendererRegistry.getRenderer(stack.getItem()).ifPresent(
                    renderer -> renderer
                        .render(finalStack, slotContext, matrixStack, renderLayerParent,
                            renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks,
                            ageInTicks, netHeadYaw, headPitch));
                rendered = true;
                Diag.once("own-renderer:" + itemId,
                    "{} is drawn by a renderer registered with this mod", itemId);
              } else if (dev.arakiel.curios.compat.forge.ForgeCuriosCompat.isLoaded()) {
                // Forge content mods register their renderers in the Forge edition's registry: run
                // theirs through the compatibility bridge so the item still shows on the body.
                rendered = dev.arakiel.curios.compat.bridge.ForgeApiBridge.renderForeign(
                    new Object[]{finalStack, slotContext, matrixStack, renderLayerParent,
                        renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks,
                        ageInTicks, netHeadYaw, headPitch});

                if (rendered) {
                  Diag.once("forge-renderer:" + itemId,
                      "{} is drawn through the Forge renderer bridge", itemId);
                } else {
                  rendered = renderTrinketsRenderer(id, finalStack, slotContext, i, livingEntity,
                      matrixStack, renderTypeBuffer, light, limbSwing, limbSwingAmount,
                      partialTicks, ageInTicks, netHeadYaw, headPitch);
                }
              } else {
                rendered = renderTrinketsRenderer(id, finalStack, slotContext, i, livingEntity,
                    matrixStack, renderTypeBuffer, light, limbSwing, limbSwingAmount, partialTicks,
                    ageInTicks, netHeadYaw, headPitch);
              }

              if (!rendered) {
                Diag.once("no-renderer:" + itemId,
                    "{} has no renderer in this mod's, the Forge or the Trinkets registry", itemId);
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
  private boolean renderTrinketsRenderer(String identifier, ItemStack stack,
                                         SlotContext slotContext, int index, T livingEntity,
                                         PoseStack matrixStack, MultiBufferSource renderTypeBuffer,
                                         int light, float limbSwing, float limbSwingAmount,
                                         float partialTicks, float ageInTicks, float netHeadYaw,
                                         float headPitch) {

    if (!dev.arakiel.curios.compat.trinkets.TrinketsCompat.isLoaded()) {
      return false;
    }
    boolean rendered = dev.arakiel.curios.compat.bridge.TrinketsApiBridge.renderForeign(
        new Object[]{stack,
            dev.arakiel.curios.compat.bridge.TrinketsApiBridge.slotReference(identifier, index,
                livingEntity),
            this.renderLayerParent.getModel(), matrixStack, renderTypeBuffer, light, livingEntity,
            limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch});

    if (rendered) {
      Diag.once("trinkets-renderer:"
              + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()),
          "{} is drawn through the Trinkets renderer bridge",
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
    return rendered;
  }
}
