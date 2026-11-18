package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Accessories rendering is owned by CuriosFabric's renderer layer. */
@Mixin(AccessoriesRenderLayer.class)
public abstract class AccessoriesRenderLayerMixin<T extends LivingEntity, M extends EntityModel<T>> {

  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$skipForeignRender(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int light,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      CallbackInfo callback) {
    callback.cancel();
  }
}
