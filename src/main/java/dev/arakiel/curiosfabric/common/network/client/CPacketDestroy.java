package dev.arakiel.curiosfabric.common.network.client;

import javax.annotation.Nonnull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;

public record CPacketDestroy() implements CustomPacketPayload {

  public static final Type<CPacketDestroy> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "destroy"));

  public static final StreamCodec<RegistryFriendlyByteBuf, CPacketDestroy> STREAM_CODEC =
      new StreamCodec<>() {
        @Nonnull
        @Override
        public CPacketDestroy decode(@Nonnull RegistryFriendlyByteBuf p_320376_) {
          return new CPacketDestroy();
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf p_320158_,
                           @Nonnull CPacketDestroy p_320396_) {

        }
      };

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
