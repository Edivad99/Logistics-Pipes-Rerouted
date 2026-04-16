# Logistic Pipes 2

A revival of [Logistics Pipes](https://github.com/RS485/LogisticsPipes), ported from Forge 1.12.2 to **NeoForge 1.20.1** — bringing the classic request-based item routing, automated crafting, and modular chassis pipes back to modern Minecraft.

## Status

Work in progress. The migration is an ongoing port of the original 1.12.2 codebase to the modern toolchain. Gameplay is functional; some subsystems (rendering polish, third-party mod integrations, legacy data fixers) are still being finished.

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

## Credits

- **Krapht** — original concept and early codebase
- **[RS485 and LogisticsPipes contributors](https://github.com/RS485/LogisticsPipes/contributors)** — the 1.12.2 codebase this project is built on
- This fork focuses exclusively on the NeoForge 1.20.1 port

## License

See [LICENSE.md](LICENSE.md).
