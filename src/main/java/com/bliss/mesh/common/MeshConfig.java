package com.bliss.mesh.common;

import net.neoforged.neoforge.common.ModConfigSpec;

import static com.bliss.mesh.Mesh.LOGGER;

public class MeshConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<MeshModes> MODE;
    public static final ModConfigSpec.ConfigValue<String> REMOTE_ADDRESS;
    public static final ModConfigSpec.IntValue PORT;

    static {
        BUILDER.push("Mesh Mod Settings");

        MODE = BUILDER.comment("Define the role of this instance.")
                .defineEnum("meshMode", MeshModes.STANDALONE);

        REMOTE_ADDRESS = BUILDER.comment("The IP address of the peer machine.")
                .define("peerAddress", "127.0.0.1");

        PORT = BUILDER.comment("The port used for Mesh-to-Mesh communication.")
                .defineInRange("meshPort", 25560, 1024, 65535);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

