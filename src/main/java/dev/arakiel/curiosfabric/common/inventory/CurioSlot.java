package dev.arakiel.curiosfabric.common.inventory;

import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotContext;
import dev.arakiel.curiosfabric.api.extensions.ICurioSlotExtension;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.client.SlotLanguage;
import dev.arakiel.curiosfabric.mixin.core.AccessorEntity;

public class CurioSlot extends SlotItemHandler {

  private final String identifier;
  private final Player player;
  private final SlotContext slotContext;
  private final ICurioSlotExtension extension;

  private final NonNullList<Boolean> renderStatuses;
  private final boolean canToggleRender;
  private List<Boolean> activeStatuses;
  private boolean showCosmeticToggle;
  private boolean isCosmetic;

  public CurioSlot(Player player, IDynamicStackHandler handler, int index, String identifier,
                   int xPosition, int yPosition, NonNullList<Boolean> renders,
                   List<Boolean> actives,
                   boolean canToggleRender, boolean showCosmeticToggle, boolean isCosmetic) {
    this(player, handler, index, identifier, xPosition, yPosition, renders, canToggleRender,
         showCosmeticToggle, isCosmetic);
    this.activeStatuses = actives;
  }

  public CurioSlot(Player player, IDynamicStackHandler handler, int index, String identifier,
                   int xPosition, int yPosition, NonNullList<Boolean> renders,
                   boolean canToggleRender, boolean showCosmeticToggle, boolean isCosmetic) {
    this(player, handler, index, identifier, xPosition, yPosition, renders, canToggleRender);
    this.showCosmeticToggle = showCosmeticToggle;
    this.isCosmetic = isCosmetic;
  }

  public CurioSlot(
      Player player,
      IDynamicStackHandler handler,
      int index,
      String identifier,
      int xPosition,
      int yPosition,
      NonNullList<Boolean> renders,
      boolean canToggleRender) {
    super(handler, index, xPosition, yPosition);
    this.identifier = identifier;
    this.renderStatuses = renders;
    this.player = player;
    this.canToggleRender = canToggleRender;
    this.slotContext =
        new SlotContext(
            identifier,
            player,
            index,
            this instanceof CosmeticCurioSlot,
            this instanceof CosmeticCurioSlot || renders.get(index));
    CuriosApi.getSlot(identifier, player.level())
        .ifPresent(slotType -> this.setBackground(InventoryMenu.BLOCK_ATLAS, slotType.getIcon()));
    String mappingId = CuriosApi.getSlot(identifier, player.level())
        .map(type -> type.getMappingIdentifier()).orElse(identifier);
    this.extension = ICurioSlotExtension.from(mappingId);
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public boolean canToggleRender() {
    return this.canToggleRender;
  }

  public boolean isActiveState() {
    return this.activeStatuses.size() > this.getSlotIndex() && this.activeStatuses.get(
        this.getSlotIndex());
  }

  public boolean isCosmetic() {
    return this.isCosmetic;
  }

  public boolean showCosmeticToggle() {
    return this.showCosmeticToggle;
  }

  public ICurioSlotExtension getSlotExtension() {
    return this.extension;
  }

  public SlotContext getSlotContext() {
    return this.slotContext;
  }

  public boolean getRenderStatus() {

    if (!this.canToggleRender) {
      return true;
    }
    return this.renderStatuses.size() > this.getSlotIndex()
        && this.renderStatuses.get(this.getSlotIndex());
  }

  public String getSlotName() {
    StringBuilder builder = new StringBuilder();

    if (this.isCosmetic) {
      builder.append(I18n.get("curiosfabric.cosmetic")).append(" ");
    }
    String mappingId = CuriosApi.getSlot(this.identifier, this.player.level())
        .map(type -> type.getMappingIdentifier()).orElse(this.identifier);
    return builder.append(SlotLanguage.name(mappingId)).toString();
  }

  @Override
  public void set(@Nonnull ItemStack stack) {
    ItemStack current = this.getItem();
    boolean flag = current.isEmpty() && stack.isEmpty();
    super.set(stack);

    if (!flag
        && !ItemStack.matches(current, stack)
        && !((AccessorEntity) this.player).getFirstTick()
        && this.isActiveState()) {
      CuriosApi.getCurio(stack).ifPresent(curio -> curio.onEquipFromUse(this.slotContext));
    }
  }

  @Override
  public boolean allowModification(@Nonnull Player pPlayer) {
    return true;
  }
}
