package com.bliss.mesh.networking.common;

public enum Packets {
    CLAIM(0),    // "U need help generating?"
    CLAIM_INFO(1), //"here is my queue for gening help pls"
    PUSH(2),     // "Here is the NBT data for a chunk"
    REQUEST(3),  // "I need the data for this chunk"
    SEED(4);     // "Here's the world seed"

    public final int id;
    Packets(int id) { this.id = id; }
}
