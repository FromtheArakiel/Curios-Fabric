package dev.arakiel.curiosfabric.common.network.client;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import javax.annotation.Nonnull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Opens the independent 54-slot Curios list at the requested page. */
public record CPacketOpenCuriosTab(int page) implements CustomPacketPayload {

  public static final Type<CPacketOpenCuriosTab> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(
          CuriosFabricConstants.MOD_ID, "open_curios_tab"));
  public static final StreamCodec<RegistryFriendlyByteBuf, CPacketOpenCuriosTab> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.VAR_INT,
          CPacketOpenCuriosTab::page,
          CPacketOpenCuriosTab::new);

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
