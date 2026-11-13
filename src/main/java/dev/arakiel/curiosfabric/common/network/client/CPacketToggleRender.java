package dev.arakiel.curiosfabric.common.network.client;

import javax.annotation.Nonnull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;

public record CPacketToggleRender(String identifier, int index) implements CustomPacketPayload {

  public static final Type<CPacketToggleRender> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "toggle_render"));

  public static final StreamCodec<RegistryFriendlyByteBuf, CPacketToggleRender> STREAM_CODEC =
      StreamCodec.composite(ByteBufCodecs.STRING_UTF8, CPacketToggleRender::identifier,
          ByteBufCodecs.INT, CPacketToggleRender::index, CPacketToggleRender::new);

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
