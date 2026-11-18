# Curios Fabric

Curios Fabric is a Fabric implementation of the Curios accessory API for Minecraft 1.21.1. It
provides a data-driven slot system, an inventory API, item lifecycle hooks, attribute modifiers,
events, client rendering integration, and compatibility bridges for NeoForge Curios, Trinkets,
and Accessories.

The public API lives under:

```text
dev.arakiel.curiosfabric.api
```

Compatibility is intentionally not guaranteed with older Curios Fabric builds. The current runtime
identity, command, data namespace, package root, and item tags all use `curiosfabric`.

## Requirements

| Requirement | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 or newer |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | Current 1.21.1 build |
| Gemini | 1.0.5 or newer |

Optional integrations include JEI, Mod Menu, Kilt, NeoForge Curios, Trinkets, and Accessories.

## Quick Start

A normal Curios Fabric mod needs three pieces:

1. Slot definitions.
2. Entity-to-slot assignments.
3. Item tags or custom validators that decide which items fit each slot.

### 1. Define a slot

Create:

```text
data/<namespace>/curiosfabric/slots/<slot_id>.json
```

Example:

```json
{
  "order": 40,
  "size": 2,
  "icon": "examplemod:slot/empty_sigil_slot",
  "validators": [
    "curiosfabric:tag"
  ],
  "add_cosmetic": true,
  "render_toggle": true,
  "drop_rule": "DEFAULT"
}
```

The file path becomes the slot identifier. The example above defines a slot named `sigil`.

### 2. Assign slots to entities

Create:

```text
data/<namespace>/curiosfabric/entities/<assignment_id>.json
```

Example:

```json
{
  "entities": [
    "minecraft:player",
    "#minecraft:undead"
  ],
  "slots": [
    "sigil"
  ]
}
```

### 3. Assign items to the slot

Create:

```text
data/curiosfabric/tags/item/sigil.json
```

Example:

```json
{
  "replace": false,
  "values": [
    "examplemod:silver_sigil"
  ]
}
```

This contributes to the item tag:

```text
#curiosfabric:sigil
```

The default `curiosfabric:tag` validator accepts items in the matching
`#curiosfabric:<slot>` tag. Items in `#curiosfabric:curio` are accepted by all tag-validated slots.

## Data Pack Reference

### Slot JSON

Path:

```text
data/<namespace>/curiosfabric/slots/<slot_id>.json
```

| Field | Type | Default | Description |
| --- | --- | --- | --- |
| `replace` | boolean | `false` | Replaces overlapping properties when another definition uses the same slot identifier. |
| `order` | integer | absent | Optional type-level anchor. Curios Fabric uses the value directly; NeoForge Curios values are normalized separately. Omit it to place the type in the fill queue. |
| `size` | integer | `1` | Number of capacity slots created for this slot type. Must be zero or greater. |
| `operation` | string | `SET` | How a merged `size` is applied: `SET`, `ADD`, or `REMOVE`. |
| `icon` | resource location | empty curio icon | Atlas sprite used as the slot background. |
| `drop_rule` | string | `DEFAULT` | `DEFAULT`, `ALWAYS_DROP`, `ALWAYS_KEEP`, or `DESTROY`. |
| `add_cosmetic` | boolean | `false` | Adds a parallel cosmetic capacity slot for each functional capacity slot. |
| `use_native_gui` | boolean | `true` | Shows the capacity slots in the Curios GUI. |
| `render_toggle` | boolean | `true` | Allows each capacity slot to toggle whether its equipped item renders. |
| `validators` | string array | `["curiosfabric:tag"]` | Predicate IDs that decide whether an item can use the slot. |
| `fabric:load_conditions` | array | absent | Fabric resource conditions. The file is skipped when its conditions do not match. |

#### Important ordering rules

- `order` belongs to the slot type, not to each capacity slot.
- A slot with `size: 3` is still one type in the type-order table.
- If `order` is absent, the type enters the fill queue.
- Explicit Curios Fabric anchors use the declared integer directly.
- Explicit NeoForge Curios anchors use:

```text
max(0, round((neoForgeOrder - 20) / 20))
```

- Equal anchor positions are sorted by identifier, then later anchors move forward.
- The fill queue starts with Curios Fabric defaults, followed by other Curios Fabric definitions,
  NeoForge Curios definitions, Trinkets definitions, and finally Accessories definitions.
- NeoForge Curios, Trinkets, or Accessories definitions already mapped to a Curios Fabric default
  are ignored.
- After filling anchors and gaps, slot types are deduplicated by `mappingIdentifier`.
- `size` is expanded only after the final type-order table is known.

For example, a type-order table of `hand, ring, head` with `ring.size = 3` produces:

```text
hand, ring, ring, ring, head
```

### Entity JSON

Path:

```text
data/<namespace>/curiosfabric/entities/<assignment_id>.json
```

| Field | Type | Default | Description |
| --- | --- | --- | --- |
| `replace` | boolean | `false` | Replaces previously loaded slot assignments for the affected entities. |
| `entities` | string array | empty | Entity IDs or entity-type tags such as `#minecraft:undead`. |
| `slots` | string array | empty | Curios slot identifiers assigned to the listed entities. |
| `fabric:load_conditions` | array | absent | Fabric resource conditions controlling whether the file is loaded. |

### Validators and predicates

Built-in validators:

| Predicate | Behavior |
| --- | --- |
| `curiosfabric:tag` | Accepts the matching `#curiosfabric:<slot>` tag and the universal `#curiosfabric:curio` tag. |
| `curiosfabric:all` | Accepts every item. |
| `curiosfabric:none` | Rejects every item. |

NeoForge-compatible aliases under `curios:tag`, `curios:all`, and `curios:none` are also accepted.
Custom predicates can be registered with:

```java
CuriosApi.registerCurioPredicate(
    ResourceLocation.fromNamespaceAndPath("examplemod", "magic_only"),
    slotResult -> slotResult.stack().is(ExampleItems.MAGIC_ITEM)
);
```

### Data generation

`CuriosDataProvider` can generate both slot and entity files:

```java
public final class ExampleCuriosData extends CuriosDataProvider {
  public ExampleCuriosData(PackOutput output,
                           CompletableFuture<HolderLookup.Provider> registries) {
    super("examplemod", output, registries);
  }

  @Override
  public void generate(HolderLookup.Provider registries) {
    createSlot("sigil")
        .size(2)
        .icon(ResourceLocation.fromNamespaceAndPath("examplemod", "slot/empty_sigil_slot"))
        .addValidator(ResourceLocation.fromNamespaceAndPath("curiosfabric", "tag"));

    createEntities("player")
        .addPlayer()
        .addSlots("sigil");
  }
}
```

## Java API Guide

### Public package map

| Package | Purpose |
| --- | --- |
| `dev.arakiel.curiosfabric.api` | Main entry point, slot context/results, tags, attributes, data generation, and triggers. |
| `api.type` | Slot metadata and source information. |
| `api.type.capability` | Item behavior and entity inventory contracts. |
| `api.type.inventory` | Equipped/cosmetic handlers and dynamic slot storage. |
| `api.type.data` | Slot and entity data-generation interfaces. |
| `api.event` | Curios gameplay events and event bus. |
| `api.client` | Rendering interfaces and renderer registry. |
| `api.extensions` | Per-slot extensions registered through the entrypoint. |

### `CuriosApi`

`dev.arakiel.curiosfabric.api.CuriosApi` is the preferred static entry point.

Common queries:

```java
Optional<ISlotType> slot = CuriosApi.getSlot("ring", level);
Map<String, ISlotType> slots = CuriosApi.getEntitySlots(entity);
Map<String, ISlotType> validSlots = CuriosApi.getItemStackSlots(stack, entity);
Optional<ICurio> behavior = CuriosApi.getCurio(stack);
Optional<ICuriosItemHandler> inventory = CuriosApi.getCuriosInventory(entity);
```

Find equipped items:

```java
CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
  handler.findFirstCurio(ExampleItems.MAGIC_RING).ifPresent(result -> {
    SlotContext context = result.slotContext();
    ItemStack stack = result.stack();
    // Use the equipped ring.
  });
});
```

Check whether a stack fits a slot:

```java
boolean valid = CuriosApi.isStackValid(slotContext, stack);
```

### Item behavior with `ICurioItem`

`ICurioItem` is intended to be implemented directly by an `Item`.

```java
public final class MagicRingItem extends Item implements ICurioItem {
  public MagicRingItem(Properties properties) {
    super(properties);
  }

  @Override
  public void curioTick(SlotContext slotContext, ItemStack stack) {
    // Called on both sides while equipped.
  }

  @Override
  public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
    return true;
  }
}
```

Important lifecycle methods:

| Method | Purpose |
| --- | --- |
| `curioTick` | Called every tick while equipped. |
| `onEquip` | Called after the stack enters a slot or its data changes. |
| `onUnequip` | Called after the stack leaves a slot or its data changes. |
| `canEquip` / `canUnequip` | Final item-specific equip restrictions. |
| `onEquipFromUse` | Called when equipped directly by using the item. |
| `getEquipSound` | Controls equip feedback. |
| `canSync`, `writeSyncData`, `readSyncData` | Optional server-to-client state synchronization. |
| `getDropRule` | Per-stack death behavior override. |
| `getFortuneLevel` / `getLootingLevel` | Fortune and Looting integration. |

`ICurio` is the stack-bound capability form of the same behavior. Most mods should prefer
`ICurioItem` unless they need to register behavior separately with
`CuriosApi.registerCurio(item, curio)`.

### Inventory API

`ICuriosItemHandler` represents an entity's Curios inventory:

```java
handler.getCurios();                         // Slot identifier -> stacks handler
handler.getStacksHandler("ring");            // One slot type
handler.getEquippedCurios();                 // Flattened equipped item view
handler.findFirstCurio(stack -> stack.is(...));
handler.findCurios("ring", "necklace");
handler.setEquippedCurio("ring", 0, stack);
```

Each `ICurioStacksHandler` contains:

- `getStacks()`: functional capacity slots.
- `getCosmeticStacks()`: cosmetic capacity slots.
- `getRenders()`: per-capacity render toggles.
- `getActiveStates()`: per-capacity active states.
- Permanent and transient slot modifiers.

Capacity slots are addressed with an integer index. The index is local to the slot type.

### Attributes and slot modifiers

Item behavior can expose equipped attribute modifiers:

```java
@Override
public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
    SlotContext slotContext, ResourceLocation id, ItemStack stack) {
  Multimap<Holder<Attribute>, AttributeModifier> modifiers = LinkedHashMultimap.create();
  modifiers.put(Attributes.MAX_HEALTH,
      new AttributeModifier(id, 4.0, AttributeModifier.Operation.ADD_VALUE));
  return modifiers;
}
```

Use stable modifier IDs so equip and unequip calculations remain symmetric.

Slot count modifiers can be added transiently or permanently through `ICuriosItemHandler`, or
attached to item data with the helpers in `CurioAttributeModifiers` and `CuriosApi`.

### Events

Register listeners through `CuriosEventBus`:

```java
CuriosEventBus.register(CurioChangeEvent.class, event -> {
  LivingEntity wearer = event.getEntity();
  String slot = event.getIdentifier();
  ItemStack previous = event.getFrom();
  ItemStack current = event.getTo();
});
```

Available events:

| Event | Purpose |
| --- | --- |
| `CurioChangeEvent` | An equipped stack changed on the server. |
| `CurioCanEquipEvent` | Allows or denies an equip attempt. |
| `CurioCanUnequipEvent` | Allows or denies an unequip attempt. |
| `CurioAttributeModifierEvent` | Modifies calculated attribute modifiers. |
| `DropRulesEvent` | Adds stack-specific death drop rule overrides. |
| `CurioDropsEvent` | Observes or cancels dropped curio entities. |
| `SlotModifiersUpdatedEvent` | A slot type's capacity changed through modifiers. |

### Client rendering

Register renderers from the client entry point:

```java
CuriosRendererRegistry.register(ExampleItems.MAGIC_RING, MagicRingRenderer::new);
```

Implement `ICurioRenderer#render`. `SlotContext` supplies the wearer, type, index, cosmetic state,
and visibility. `ICurioRenderer` also provides helpers for sneaking and head/body rotation.

### Entry points and extensions

Declare a Curios entry point in `fabric.mod.json`:

```json
"entrypoints": {
  "curiosfabric": [
    "com.example.example.ExampleCuriosEntrypoint"
  ]
}
```

Implement `CuriosEntrypoint`:

```java
public final class ExampleCuriosEntrypoint implements CuriosEntrypoint {
  @Override
  public void createSlots(Consumer<SlotTypeMessage> registrar) {
    registrar.accept(new SlotTypeMessage.Builder("pocket").size(2).build());
  }

  @Override
  public void registerExtensions(RegisterCuriosExtensionsEvent event) {
    // Register ICurioSlotExtension instances.
  }
}
```

Data packs are the preferred slot-definition mechanism. `SlotTypeMessage` remains available for
source compatibility and code-driven integrations.

## Compatibility

### NeoForge Curios

Curios Fabric reads:

```text
data/<namespace>/curios/slots
data/<namespace>/curios/entities
#curios:<slot>
```

Third-party definitions remain distinct identifiers while sharing semantic mapping IDs. Upstream
definitions mapped to Curios Fabric defaults are ignored so they cannot create duplicate default
slots.

### Trinkets

Trinkets native paths are mapped to Curios Fabric semantics, for example:

```text
hand/ring    -> ring
offhand/ring -> ring
head/hat     -> head
```

Third-party Trinkets paths are exposed as Curios slot types. Trinkets group order and slot order are
currently not inherited; this is a low-priority compatibility TODO.

### Accessories

Curios Fabric reads Accessories' final Fabric loader state rather than registering its JSON files
as a second data source:

```text
data/<namespace>/accessories/slot
data/<namespace>/accessories/group
data/<namespace>/accessories/entity
```

Accessories body-part groups and `order` values are intentionally ignored. Like Trinkets, every
unmapped slot is appended to the normal fill queue, after Trinkets. Built-in slots are mapped to
Curios Fabric defaults:

```text
hat/face -> head
cape/back -> back
hand -> hands
wrist -> bracelet
```

Third-party Accessories slots are discovered dynamically and retain their own source, icon,
language key, and entity bindings. Accessories' inventory is exposed as a live view over the
Curios Fabric inventory, while its own GUI, entity tick, drop, equip and render layers are
disabled to keep Curios Fabric as the single accessory runtime.

## Commands

Commands require permission level 2.

```mcfunction
/curiosfabric add ring @s 1
/curiosfabric set necklace @s 3
/curiosfabric remove ring @s 1
/curiosfabric list
```

`add`, `set`, and `remove` change the player's capacity for an already registered type. They do not
create new slot types.

## Building

```text
gradlew.bat clean build
```

The primary output is:

```text
build/libs/curiosfabric-fabric-1.21.1-<version>.jar
```

## Credits

Curios and its original API design were created by **C4 (TheIllusiveC4)**. Curios Fabric builds on
that public API and gameplay model, and also acknowledges the Curios contributors, Trinkets
maintainers, and the Fabric ecosystem projects that make compatibility work possible.

This project is maintained by **FromtheArakiel**.

## License

Curios Fabric is distributed under the **GNU Lesser General Public License 3.0 or later**.
Portions derived from upstream Curios retain the upstream copyright and license notices.
