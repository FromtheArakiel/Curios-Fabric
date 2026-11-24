package dev.arakiel.curiosfabric.client.gui;

import com.mojang.datafixers.util.Pair;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

/** Resolves both atlas sprites and plain PNG icons used by Curios slot types. */
public final class CurioSlotIcons {

  private CurioSlotIcons() {
  }

  public static boolean render(@Nonnull GuiGraphics graphics, @Nonnull Minecraft minecraft,
                               @Nonnull Slot slot) {
    Pair<ResourceLocation, ResourceLocation> icon = slot.getNoItemIcon();
    if (icon == null) {
      return false;
    }

    TextureAtlasSprite sprite =
        minecraft.getTextureAtlas(icon.getFirst()).apply(icon.getSecond());
    if (!sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
      graphics.blit(slot.x, slot.y, 0, 16, 16, sprite);
      return true;
    }

    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
        icon.getSecond().getNamespace(), "textures/" + icon.getSecond().getPath() + ".png");
    if (minecraft.getTextureManager().getTexture(texture)
        == MissingTextureAtlasSprite.getTexture()) {
      return false;
    }

    graphics.blit(texture, slot.x, slot.y, 0, 0, 16, 16, 16, 16);
    return true;
  }
}
