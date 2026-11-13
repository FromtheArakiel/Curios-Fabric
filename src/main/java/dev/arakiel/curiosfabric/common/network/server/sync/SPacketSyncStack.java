package dev.arakiel.curiosfabric.common.network.server.sync;

import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;

public record SPacketSyncStack(int entityId, String curioId, int slotId, ItemStack stack,
                               int handlerType, CompoundTag compoundTag) implements
    CustomPacketPayload {

  public static final Type<SPacketSyncStack> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "sync_stack"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SPacketSyncStack> STREAM_CODEC =
      StreamCodec.composite(ByteBufCodecs.INT, SPacketSyncStack::entityId,
          ByteBufCodecs.STRING_UTF8, SPacketSyncStack::curioId, ByteBufCodecs.INT,
          SPacketSyncStack::slotId, ItemStack.OPTIONAL_STREAM_CODEC, SPacketSyncStack::stack,
          ByteBufCodecs.INT, SPacketSyncStack::handlerType, ByteBufCodecs.COMPOUND_TAG,
          SPacketSyncStack::compoundTag, SPacketSyncStack::new);

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public enum HandlerType {
    EQUIPMENT, COSMETIC;

    public static HandlerType fromValue(int value) {
      try {
        return HandlerType.values()[value];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalArgumentException("Unknown handler value: " + value);
      }
    }
  }
}
