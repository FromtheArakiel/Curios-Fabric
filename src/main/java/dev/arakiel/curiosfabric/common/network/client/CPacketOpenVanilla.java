package dev.arakiel.curiosfabric.common.network.client;

import javax.annotation.Nonnull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;

public record CPacketOpenVanilla(ItemStack carried) implements CustomPacketPayload {

  public static final Type<CPacketOpenVanilla> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "open_vanilla"));

  public static final StreamCodec<RegistryFriendlyByteBuf, CPacketOpenVanilla> STREAM_CODEC =
      StreamCodec.composite(
          ItemStack.OPTIONAL_STREAM_CODEC,
          CPacketOpenVanilla::carried,
          CPacketOpenVanilla::new);

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
