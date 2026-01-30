package com.bliss.mesh.server.networking;

public enum MeshInternalPackets {
    CLAIM(0),    // "U need help generating?"
    CLAIM_INFO(1), //"here is my queue for gening help pls"
    PUSH(2),     // "Here is the NBT data for a chunk"
    REQUEST(3); // "I need the data for this chunk"

    public final int id;
    MeshInternalPackets(int id) { this.id = id; }
}
