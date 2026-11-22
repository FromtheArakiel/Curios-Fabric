package dev.arakiel.curiosfabric.common.network.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotAttribute;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.common.inventory.container.CuriosContainer;
import dev.arakiel.curiosfabric.common.inventory.container.CuriosContainerProvider;
import dev.arakiel.curiosfabric.common.event.CuriosFabricEventHandler;
import dev.arakiel.curiosfabric.common.inventory.InventoryOperationPolicy;
import dev.arakiel.curiosfabric.common.network.NetworkHandler;
import dev.arakiel.curiosfabric.common.network.client.CPacketDestroy;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenCurios;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenVanilla;
import dev.arakiel.curiosfabric.common.network.client.CPacketPage;
import dev.arakiel.curiosfabric.common.network.client.CPacketToggleCosmetics;
import dev.arakiel.curiosfabric.common.network.client.CPacketToggleRender;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncRender;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncStack;

/**
 * Handles the client to server payloads.
 *
 * <p>Fabric invokes these handlers on the server thread, so the {@code enqueueWork} plumbing of the
 * Neoforge edition is unnecessary and has been removed; the player is passed in directly.</p>
 */
public class CuriosFabricServerPayloadHandler {

  private static final CuriosFabricServerPayloadHandler INSTANCE = new CuriosFabricServerPayloadHandler();

  public static CuriosFabricServerPayloadHandler getInstance() {
    return INSTANCE;
  }

  public void handlerToggleRender(final CPacketToggleRender data, final Player player) {
    CuriosApi.getCuriosInventory(player)
        .flatMap(handler -> handler.getStacksHandler(data.identifier()))
        .ifPresent(stacksHandler -> {
          NonNullList<Boolean> renderStatuses = stacksHandler.getRenders();

          if (renderStatuses.size() > data.index()) {
            boolean value = !renderStatuses.get(data.index());
            renderStatuses.set(data.index(), value);
            NetworkHandler.sendToTrackingAndSelf(player,
                new SPacketSyncRender(player.getId(), data.identifier(), data.index(), value));
          }
        });
  }

  public void handlePage(final CPacketPage data, final Player player) {
    AbstractContainerMenu container = player.containerMenu;

    if (container instanceof CuriosContainer && container.containerId == data.windowId()) {

      if (data.next()) {
        ((CuriosContainer) container).nextPage();
      } else {
        ((CuriosContainer) container).prevPage();
      }
    }
  }

  public void handlerToggleCosmetics(final CPacketToggleCosmetics data, final Player player) {
    AbstractContainerMenu container = player.containerMenu;

    if (container instanceof CuriosContainer && container.containerId == data.windowId()) {
      ((CuriosContainer) container).toggleCosmetics();
    }
  }

  public void handleOpenVanilla(final CPacketOpenVanilla data, final Player player) {

    if (player instanceof ServerPlayer serverPlayer) {
      ItemStack stack = player.isCreative() ? data.carried() : player.containerMenu.getCarried();
      player.containerMenu.setCarried(ItemStack.EMPTY);
      serverPlayer.doCloseContainer();

      if (!stack.isEmpty()) {

        if (!player.isCreative()) {
          player.containerMenu.setCarried(stack);
        }
        NetworkHandler.sendToPlayer(serverPlayer, new SPacketGrabbedItem(stack));
      }
    }
  }

  public void handleOpenCurios(final CPacketOpenCurios data, final Player player) {

    if (player instanceof ServerPlayer serverPlayer) {
      ItemStack stack = player.isCreative() ? data.carried() : player.containerMenu.getCarried();
      player.containerMenu.setCarried(ItemStack.EMPTY);
      // A client-side respawn (for example by a skin refresh mod) can replace LocalPlayer
      // without replacing ServerPlayer or firing AFTER_RESPAWN. Re-sync before opening so the
      // client builds the menu from the same dynamic slot set as the server.
      if (CuriosFabricEventHandler.syncBeforeOpen(serverPlayer)) {
        player.openMenu(new CuriosContainerProvider());
        if (!stack.isEmpty()) {
          player.containerMenu.setCarried(stack);
          NetworkHandler.sendToPlayer(serverPlayer, new SPacketGrabbedItem(stack));
        }
      }
    }
  }

  public void handleDestroyPacket(final CPacketDestroy data, final Player player) {
    CuriosApi.getCuriosInventory(player)
        .ifPresent(handler -> handler.getCurios().values().forEach(stacksHandler -> {
          IDynamicStackHandler stackHandler = stacksHandler.getStacks();
          IDynamicStackHandler cosmeticStackHandler = stacksHandler.getCosmeticStacks();
          String id = stacksHandler.getIdentifier();

          for (int i = stackHandler.getSlots() - 1; i >= 0; i--) {
            NonNullList<Boolean> renderStates = stacksHandler.getRenders();
            SlotContext slotContext = new SlotContext(id, player, i, false,
                renderStates.size() > i && renderStates.get(i));
            ItemStack stack = stackHandler.getStackInSlot(i);
            ItemStack cosmeticStack = cosmeticStackHandler.getStackInSlot(i);

            // Hand the player whatever fits, and keep the rest equipped. Taking a curio off never
            // destroys it and never drops it on the ground; with no room at all the slot is simply
            // left untouched, exactly like a shift click that cannot move the item.
            ItemStack stackLeft = stack.copy();
            player.getInventory().add(stackLeft);
            ItemStack cosmeticLeft = cosmeticStack.copy();
            player.getInventory().add(cosmeticLeft);
            boolean functionalChanged = stackLeft.getCount() != stack.getCount();
            boolean cosmeticChanged = cosmeticLeft.getCount() != cosmeticStack.getCount();

            if (!functionalChanged && !cosmeticChanged) {
              continue;
            }

            if (functionalChanged) {
              boolean fullyUnequipped =
                  InventoryOperationPolicy.shouldRunUnequip(stack.getCount(), stackLeft.getCount());
              stackHandler.setStackInSlot(i, stackLeft);
              stackHandler.setPreviousStackInSlot(i, stackLeft.copy());

              if (fullyUnequipped && !stack.isEmpty()) {
                removeEquippedEffects(player, handler, slotContext, stack);
              }

              NetworkHandler.sendToTrackingAndSelf(player,
                  new SPacketSyncStack(player.getId(), id, i, stackLeft,
                      SPacketSyncStack.HandlerType.EQUIPMENT.ordinal(), new CompoundTag()));
            }

            if (cosmeticChanged) {
              cosmeticStackHandler.setStackInSlot(i, cosmeticLeft);
              cosmeticStackHandler.setPreviousStackInSlot(i, cosmeticLeft.copy());
              NetworkHandler.sendToTrackingAndSelf(player,
                  new SPacketSyncStack(player.getId(), id, i, cosmeticLeft,
                      SPacketSyncStack.HandlerType.COSMETIC.ordinal(), new CompoundTag()));
            }
          }
        }));
  }

  private static void removeEquippedEffects(Player player, ICuriosItemHandler handler,
                                            SlotContext slotContext, ItemStack stack) {
    try {
      Multimap<Holder<Attribute>, AttributeModifier> map =
          CuriosApi.getAttributeModifiers(slotContext, CuriosApi.getSlotId(slotContext), stack);
      Multimap<String, AttributeModifier> slots = HashMultimap.create();
      Set<Holder<Attribute>> toRemove = new HashSet<>();
      AttributeMap attributeMap = player.getAttributes();

      for (Holder<Attribute> attribute : map.keySet()) {
        if (attribute.value() instanceof SlotAttribute wrapper) {
          slots.putAll(wrapper.getIdentifier(), map.get(attribute));
          toRemove.add(attribute);
        }
      }
      for (Holder<Attribute> attribute : toRemove) {
        map.removeAll(attribute);
      }
      map.forEach((key, value) -> {
        AttributeInstance instance = attributeMap.getInstance(key);
        if (instance != null) {
          instance.removeModifier(value);
        }
      });
      handler.removeSlotModifiers(slots);
      CuriosApi.getCurio(stack).ifPresent(curio -> curio.onUnequip(slotContext, stack));
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.warn("Failed to complete a curio unequip", throwable);
    }
  }
}
