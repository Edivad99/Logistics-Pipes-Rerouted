# Migration Status

A high-level checkpoint of where the Forge 1.12.2 → NeoForge 1.20.1 port stands. Updated periodically; if you're picking up work, check the open issues for finer-grained scope.

## Working

- **Compile & tests** — full project compiles on NeoForge 1.20.1; unit tests pass.
- **Runtime** — pipes place, persist across save/load, and render; GUIs open; dev client and dev server both launch and are playable.
- **Registration** — blocks, items, block entities, menus, recipe types, entities all on `DeferredRegister`.
- **Networking** — packets wired via `SimpleChannel` and a unified `LPPacketPayload`; packet auto-discovery uses `ModFileScanData`.
- **Capabilities** — `RegisterCapabilitiesEvent` wired for the main block entities.
- **Routing & logistics** — `ServerRouter` / `ClientRouter`, pathfinding, promises, crafting orderer, chassis modules, request pipes.
- **Ore dictionary → Tags** — all tag lookups ported.
- **Commands** — Brigadier-based.
- **GUIs (non-rendering)** — every screen and container migrated to the 1.20.1 menu/screen API.
- **Item models & recipes** — modules, upgrades, tool items, most pipe items.

## In progress

- **Rendering polish** — traveling items render but visuals are still being tuned; some block-entity renderers need cleanup.
- **Item / fluid model coverage** — the majority are done; a handful of edge-case items still on placeholder models.

## Deferred

- **Third-party mod integrations** — BuildCraft, IndustrialCraft 2, Thermal Dynamics, NEI/JEI (partial), EnderStorage, IronChest, OpenComputers, The One Probe. Most are blocked on upstream 1.20.1 ports; stubs exist so the rest of the mod loads without them.
- **Legacy data fixers** — no upgrade path from 1.12.2 worlds; start a fresh 1.20.1 save.
- **SideConfigDisplay** — not yet ported.
- **HUD power level readout** — UI polish pending.
- **Fabric loader support** — planned after the NeoForge port reaches feature parity (via Architectury).

## Contributing

- Check open issues before starting — some areas are actively being worked on.
- Gradle toolchains provision JDK 17 automatically, so you don't need to set `JAVA_HOME`.
- `./gradlew build` should succeed out of the box after `git lfs fetch`.
- For rendering-related work, compare against the original 1.12.2 `dev` branch of upstream [RS485/LogisticsPipes](https://github.com/RS485/LogisticsPipes) as the visual reference.
