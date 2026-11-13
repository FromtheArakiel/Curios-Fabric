package dev.arakiel.curiosfabric.common.network.server;

import javax.annotation.Nonnull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;

public record SPacketGrabbedItem(ItemStack stack) implements CustomPacketPayload {

  public static final Type<SPacketGrabbedItem> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "grabbed_item"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SPacketGrabbedItem> STREAM_CODEC =
      StreamCodec.composite(ItemStack.STREAM_CODEC, SPacketGrabbedItem::stack,
          SPacketGrabbedItem::new);

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
