package com.github.piotrrzysko.simdjson;

record JsonCharacterBlock(long whitespace, long op) {

    long scalar() {
        return ~(op | whitespace);
    }
}
