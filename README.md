# CosmicClientBootstrap

Spigot 1.8.8 plugin by **CrazyLadAlex**.

Main class: `me.alex.cosmicclientbootstrap.CosmicClientBootstrap`

A standalone Cosmic client bootstrap for Spigot 1.8.8. Initializes client features, permissions, player headers, cooldowns, and world names when players join, and provides administrative packet commands and a reusable packet service.

Adapted from the CosmicPrisons/CosmicCore packet module. CosmicCore, CosmicFramework, ACF, and Lombok are not needed by this plugin.

## Build

Use JDK 17 or newer and Maven to build. The plugin is compiled to Java 8 bytecode and runs on Java 8 or newer.

Populate `libs/` with `twspigot.jar`, `CosmicClientAPI.jar`, `DemonicClientComms.jar`, and `ProtocolLib.jar` from your CosmicPrisons setup. These dependencies are not distributed in this repository or bundled into the output JAR.

From the repository root, install them into your local Maven repository once:

```shell
mvn install:install-file -Dfile=libs/twspigot.jar -DgroupId=me.alex.cosmicclientbootstrap.vendor -DartifactId=twspigot -Dversion=local -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=libs/CosmicClientAPI.jar -DgroupId=me.alex.cosmicclientbootstrap.vendor -DartifactId=CosmicClientAPI -Dversion=local -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=libs/DemonicClientComms.jar -DgroupId=me.alex.cosmicclientbootstrap.vendor -DartifactId=DemonicClientComms -Dversion=local -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=libs/ProtocolLib.jar -DgroupId=me.alex.cosmicclientbootstrap.vendor -DartifactId=ProtocolLib -Dversion=local -Dpackaging=jar -DgeneratePom=true
```

Then build with `mvn clean package`. The output is `target/CosmicClientBootstrap-1.0.0.jar`.

## Install

Place the built JAR in the server's `plugins/` directory alongside **CosmicClientAPI**, **DemonicClientComms**, and a **ProtocolLib version compatible with your 1.8.8 server**. Restart the server.

The supplied client dependencies reference `v1_8_R3`, so the concrete target is **1.8.8**. Java 8 is the minimum runtime. Compatible Cosmic client software is needed to display these features. DemonicClientComms only sends its packets after it recognizes the player's client. A command request is not proof that a client displayed the result.

Use this as the replacement for CosmicCore's packet module. Running both bootstraps sends duplicate initialization packets; disable the original module if CosmicCore is still installed.

## Behavior and commands

On join, and for online players when enabled, the plugin follows the original bootstrap: immediate legacy feature set; feature set and headers at 10 ticks; permissions, registration, repeated feature set and empty cooldowns at 20 ticks; feature set and headers at 40 ticks; world name at 100 ticks. Delayed work skips disconnected players and is cancelled when the plugin disables.

Commands require `cosmic.admin` (operators by default). An omitted player targets the sender; console must specify a player.

```text
/packets bootstrap [player]
/packets feature [player]
/packets permissions [player]
/packets worldname <name> [player]
/packets highlight <operation> <min> <max> [player]
/packets mapimage <url> <width> <height> <topLeftX> <topLeftZ> [player]
```

Aliases: `/clientpacket`, `/cpacket`, `/cpclient`. Highlight targets the block the recipient is looking at within ten blocks, using red. Operation integers are passed through as in the original module. Map image URLs must use HTTP or HTTPS, with positive dimensions.

Other plugins can obtain `getPacketService()` from the main plugin instance. The service retains feature, permissions, headers, cooldowns, XP bar, XP text, XP values, titles, map images, highlights, and raw client packet helpers. Call it from the server thread. Reflection is retained for packet fields because the supplied CosmicClientAPI JAR has no setters for them; different API builds may require adaptation.

Packet module provenance: `CosmicCore/src/main/java/dev/wishnote/core/modules/packet` in the supplied CosmicPrisons project. Plugin metadata credits CrazyLadAlex; the packet service and handshake are adapted from that existing module.

## License

Released under the [MIT License](LICENSE). Anyone may use, modify, and redistribute this plugin, including commercially, while retaining the license and copyright notice. Third-party dependencies retain their own licenses.
