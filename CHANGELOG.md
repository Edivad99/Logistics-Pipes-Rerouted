# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
follows [Semantic Versioning](https://semver.org/) where practical.

## [Unreleased]

### Fixed
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
