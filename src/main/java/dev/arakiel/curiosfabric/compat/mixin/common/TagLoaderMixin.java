package dev.arakiel.curiosfabric.compat.mixin.common;

import dev.arakiel.curiosfabric.compat.CompatibilityRuntime;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends the item-tag load result with compatibility tags before Minecraft binds them. */
@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {

  @Shadow
  @Final
  private String directory;

  @Inject(method = "load", at = @At("TAIL"))
  private void curiosfabric$mirrorCompatibilityTags(
      ResourceManager resourceManager,
      CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
    if (Registries.tagsDirPath(BuiltInRegistries.ITEM.key()).equals(this.directory)) {
      CompatibilityRuntime.mirrorTags(
          cir.getReturnValue(), dev.arakiel.curiosfabric.CuriosFabricConstants.IS_CLIENT);
    }
  }
}
