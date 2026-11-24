package dev.arakiel.curiosfabric.common.inventory.container;

/** Fixed geometry and pagination semantics for the tabbed 54-slot Curios list. */
public final class CuriosTabLayout {

  public static final int COLUMNS = 9;
  public static final int MIN_ROWS = 3;
  public static final int MAX_ROWS = 6;
  public static final int PAGE_SIZE = COLUMNS * MAX_ROWS;
  public static final int IMAGE_WIDTH = 176;
  public static final int SLOT_START_X = 8;
  public static final int SLOT_START_Y = 17;
  public static final int SLOT_SPACING = 18;
  public static final int PLAYER_INVENTORY_SLOTS = 36;
  public static final int PLAYER_INVENTORY_HEIGHT = 82;
  public static final int PLAYER_HOTBAR_OFFSET = 58;
  public static final int TITLE_X = 8;
  public static final int PAGE_BUTTON_Y = 4;
  public static final int PAGE_BUTTON_WIDTH = 10;
  public static final int PAGE_BUTTON_HEIGHT = 11;
  public static final int PAGE_BUTTON_AFTER_TITLE = 14;
  private static final int TOP_AREA_HEIGHT = 112;

  private CuriosTabLayout() {
  }

  public static int pageCount(int slotCount) {
    return Math.max(1, (slotCount + PAGE_SIZE - 1) / PAGE_SIZE);
  }

  public static int pageStart(int page) {
    return Math.max(0, page) * PAGE_SIZE;
  }

  public static int rows(int pageSlotCount) {
    return Math.max(MIN_ROWS,
        Math.min(MAX_ROWS, (Math.max(0, pageSlotCount) + COLUMNS - 1) / COLUMNS));
  }

  public static int imageHeight(int rows) {
    return TOP_AREA_HEIGHT + rows(rows * COLUMNS) * SLOT_SPACING;
  }

  public static int playerInventoryY(int rows) {
    return imageHeight(rows) - PLAYER_INVENTORY_HEIGHT;
  }

  public static int playerHotbarY(int rows) {
    return playerInventoryY(rows) + PLAYER_HOTBAR_OFFSET;
  }

  public static int playerInventoryLabelY(int rows) {
    return playerInventoryY(rows) - 11;
  }

  public static int nextPageButtonX(int titleWidth) {
    return TITLE_X + titleWidth + PAGE_BUTTON_AFTER_TITLE;
  }

  public static int previousPageButtonX(int titleWidth) {
    return nextPageButtonX(titleWidth) - PAGE_BUTTON_WIDTH - 1;
  }
}
