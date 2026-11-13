package dev.arakiel.curiosfabric.common.network.client;

import javax.annotation.Nonnull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;

public record CPacketToggleCosmetics(int windowId) implements CustomPacketPayload {

  public static final Type<CPacketToggleCosmetics> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "toggle_cosmetics"));

  public static final StreamCodec<RegistryFriendlyByteBuf, CPacketToggleCosmetics> STREAM_CODEC =
      StreamCodec.composite(ByteBufCodecs.INT, CPacketToggleCosmetics::windowId,
          CPacketToggleCosmetics::new);

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
