# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
follows [Semantic Versioning](https://semver.org/) where practical.

## [0.0.2] - 2026-06-10

Second public beta. Fixes the 0.0.1 production crash, restores the guide
book and several inert features to working order, and removes ~1,900 lines
of dead 1.12-era compatibility code. Please report issues at the project
issue tracker — this is a testing release.

### Fixed
- **Client crashed rendering pipes when rejoining a world that already
  contained them** (`NullPointerException: Cannot read field "isClientSide"`
  in the block-entity renderer). The client tile entity can run `load()` more
  than once on a cold world load; each run replaced the pipe object without
  rebinding it to its container. `load()` now rebinds, and
  `MainProxy.isClient(Level)` null-guards like its `isServer` twin.
  Latent since the original port — earlier testing only ever placed pipes
  live, which initializes through a different path.
- **Guide Book was non-functional**: page content never scrolled (no `tick()`
  animation), every button click went to an empty handler (home, add/remove
  bookmark, tab switching, tab color cycling all dead), the active tab and
  tab tooltips were drawn under the opaque frame, the bookmark button's
  hitbox drifted from its rendered position, and the slider rail showed a
  tiling artifact. All ported faithfully from LP1's `drawScreen`/
  `updateScreen`/`mousePressed` flow onto the 1.20.1 `GuiGraphics`/widget
  model.
- **"Creative Tab Based Item Sink" module sank nothing on dedicated servers**
  (and showed `null` for survival players): vanilla only builds creative tab
  contents from the client creative-inventory screen. The item→tab mapping is
  now built mod-side from CATEGORY tabs only — previously the SEARCH tab
  (which aggregates every item) swallowed most lookups.
- **Five fluid pipes were uncraftable** (request, provider, satellite,
  insertion, extractor): chipped-crafting recipes and the program-compiler
  FLUID category are reinstated, based off the fluid supplier pipe.
- **Fluid-type picker GUI never opened** from fluid module slots; rewired to
  LP1's `SelectItemOutOfList` popup flow.
- Three packets (`ComponentList`, `MissingItems`,
  `RoutingUpdateAskForTarget`) referenced client classes unguarded on
  dedicated servers; orderer popup toggle (`displayPopup`) now persists; a
  `%d`-on-ResourceLocation crash in `ServerRouter.toString()` debug output.

### Changed
- **Registry access migrated to vanilla** `BuiltInRegistries`/`Registries`
  keys throughout (including `DeferredRegister.create`); zero
  `ForgeRegistries` references remain, shrinking the loader-coupled surface
  for future version ports.
- The inert `@ClientSideOnlyMethodContent` annotation (honored only by the
  deleted 1.12 coremod, i.e. fake protection) is gone; every former use now
  has a real `FMLEnvironment.dist` guard + `@OnlyIn(Dist.CLIENT)` helper.
- Guide book main-menu links point at this repository instead of upstream
  RS485 (bug reports, builds, contribution); original-creator credits remain.
- `MIGRATION.md` gained a measured "Forward-port surface (1.21+)" section
  (namespace rename, capabilities rework, `CustomPacketPayload`, events,
  fluids, toolchain).

### Removed
- **The entire 1.12-era dead-mod compat layer** (−1,866 lines): BuildCraft,
  IC2, ComputerCraft, CoFH/Thermal Expansion, Thermal Dynamics, NEI,
  IronChest, EnderStorage, EnderCore, OpenComputers and MCMultiPart stubs,
  whose dummy behavior is now constant-folded at the former call sites (no
  runtime behavior change; also removed two latent NPE paths). `PowerProxy`
  stays — it is LP's own live Forge Energy implementation.

### Fixed (0.0.1 production)
- **0.0.1 crashed on load on every production NeoForge/Forge 1.20.1 install**
  (`NoSuchFieldError: BLOCK_ENTITY_TYPE` at `LPRegistries.<clinit>`, issue #1).
  NeoGradle 7 does not reobfuscate for 1.20.1, so the published jar shipped
  Mojmap names while the production runtime uses SRG. The build now
  reobfuscates Mojmap → SRG with TinyRemapper; AutoRenamingTool was unusable
  here because it propagated MC method renames (e.g. `Container.isEmpty →
  m_7983_`) into `java.util.Deque`, crashing the mod a second way.
- **Mod would not load on a dedicated server** once past that crash —
  client-only classes leaked onto the common load path. Gated the render
  proxy (`CCLProxy` / `LPRenderStateImpl`), the client tick/chat/login event
  handlers, and render-model preloading behind `Dist.CLIENT`, and moved two
  GUI packets' client bodies into `@OnlyIn(Dist.CLIENT)` helpers. Packet IDs
  are now index-based so a packet skipped on one side can no longer desync the
  protocol. A dedicated server now reaches *Done* with zero LogisticsPipes
  errors.
- **Branch HEAD compiles again.** Commit `7e2591c38` ("gate DEBUG on
  production env") made `LogisticsPipes.DEBUG` `final` + initialiser-
  gated on `!FMLEnvironment.production`, but left the older manifest-
  based `LogisticsPipes.DEBUG = false;` assignment in place — final-
  reassignment compile error. Removed the dead line; the field-
  initialiser already handles the production check. (`ce8371362`)

### Status
- **Feature parity with LP 1.12.2 (upstream `dev`):** essentially
  complete. Only 18 Java files from LP1 `dev` are intentionally not
  ported — all 1.12.2-only legacy: ASM coremod transformers
  (`asm/*` — superseded by Mixins on 1.20.1+), MCMP / Thermal
  Dynamics integration (those mods don't exist on 1.20.1), old
  `IGuiHandler` API (`network/GuiHandler.java`), OreDictionary recipe
  conditions (replaced by Tags), and 1.12.2 API-name utilities
  (`EnumFacingUtil`, `FinalNBTTagCompound`, etc.).
- **Build verified:** `./gradlew runClient` boots cleanly, mod
  identifies as `"Logistic Pipes 2 1.0.0 (logisticspipes)"`, all 121
  pipe textures register, integrated server tick fires.

## [0.0.1] - 2026-04-16

First public beta of Logistic Pipes 2 — a NeoForge 1.20.1 revival of the
classic Logistics Pipes. Distributed under the MMPL-1.0.1.

### Added
- Initial NeoForge 1.20.1 port of the full Forge 1.12.2 codebase
- Core pipe placement, persistence, and rendering
- Routing network (`ServerRouter` / `ClientRouter`), pathfinding, promises
- Chassis pipes (Mk1–Mk5) with hot-swappable modules
- Request pipes, provider pipes, crafter pipes, satellite pipes
- GUI rewrite on the 1.20.1 Menu/Screen API (all LP screens)
- Packet system on `SimpleChannel` + `LPPacketPayload`
- `RegisterCapabilitiesEvent` wiring for the main block entities
- Item models and crafting recipes for modules and upgrades
- Brigadier-based commands; OreDictionary → Tags migration

### Known issues
- **JEI integration** — LP's JEI plugin is ported but JEI itself cannot be
  loaded on NeoForge 1.20.1 due to an SRG-name remap gap; see
  [MIGRATION.md](MIGRATION.md) for details
- **Third-party mod integrations** — BuildCraft, IC2, Thermal Dynamics,
  EnderStorage, IronChest, OpenComputers, The One Probe are stubbed;
  waiting on upstream 1.20.1 ports
- **Rendering polish** — traveling items render but are still being tuned
- **SideConfigDisplay** — not yet ported
- **No world upgrade path from 1.12.2 saves** — start on a fresh 1.20.1 world

### Credits
- **Krapht** — original Logistics Pipes concept
- **RS485 and the LogisticsPipes contributors** — the 1.12.2 codebase this
  port is built on; see
  [upstream contributors](https://github.com/RS485/LogisticsPipes/contributors)
- **NoZeroG** — NeoForge 1.20.1 migration and ongoing maintenance

[0.0.1]: https://github.com/VoiceLessQ/Logistic-Pipes-2/releases/tag/v0.0.1
