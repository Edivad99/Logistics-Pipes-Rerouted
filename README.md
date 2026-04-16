# Logistic Pipes 2

A revival of [Logistics Pipes](https://github.com/RS485/LogisticsPipes), ported from Forge 1.12.2 to **NeoForge 1.20.1** — bringing the classic request-based item routing, automated crafting, and modular chassis pipes back to modern Minecraft.

See the original [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/logistics-pipes) for background on what the mod has been since its 1.4.x days.

## What is Logistic Pipes

A logistics mod that lets you build pipe networks capable of requesting items on demand, automatically sorting inventory, and triggering crafting chains. Pipes connect to any inventory and route items intelligently based on your configured rules — no constant item flow, no lost items, just point-to-point delivery driven by what you ask for.

<!-- TODO: add a screenshot or short GIF of a network in action -->

## Status

Work in progress. The migration is an ongoing port of the original 1.12.2 codebase to the modern toolchain. Core gameplay (placing pipes, routing, requesting, chassis modules, crafting) is functional. Polish and third-party integrations are the main outstanding work. See [MIGRATION.md](MIGRATION.md) for a detailed checkpoint.

## Known Limitations

- **Mod integrations disabled** — BuildCraft, IndustrialCraft 2, Thermal Dynamics, NEI, and similar integrations are stubbed out; most are waiting on upstream ports to 1.20.1.
- **Items-in-transit rendering** — visible but still being polished.
- **SideConfigDisplay** — not yet functional.
- **No world upgrade path from 1.12.2 saves** — legacy data fixers are not ported; start on a 1.20.1 world.

## Planned

- Fabric support via [Architectury](https://docs.architectury.dev/) (after the NeoForge 1.20.1 port reaches feature parity)
- Published API jar so addons can be built against a stable surface
- Performance improvements (render caching, dirty-flag system)

## Versions

- Minecraft **1.20.1**
- NeoForge **47.1.88**
- Java **17** (provisioned automatically via Gradle toolchains)
- Kotlin **1.7.10**

## Building

```bash
./gradlew build         # full build; output in build/libs/
./gradlew runClient     # launch a dev client
./gradlew runServer     # launch a dev server
./gradlew check         # unit tests
```

Requires `git-lfs` for a small number of LFS-tracked JARs. If you cloned without it:

```bash
git lfs install && git lfs fetch && git lfs checkout
```

## Contributing

Issues and PRs welcome. Skim [MIGRATION.md](MIGRATION.md) and the open issues for current focus areas before starting work.

## Credits

- **Krapht** — original concept and early codebase
- **[RS485 and LogisticsPipes contributors](https://github.com/RS485/LogisticsPipes/contributors)** — the 1.12.2 codebase this project is built on
- This fork focuses exclusively on the NeoForge 1.20.1 port

## License

See [LICENSE.md](LICENSE.md).
