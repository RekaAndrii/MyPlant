package com.my.plant;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HexEncodingTest {

    @Test
    public void hexEncodingRoundTripShouldWork() {
        HexFormat hex = HexFormat.of().withUpperCase();
        byte[] bytes = HexFormat.of().parseHex("0A0B");

        assertArrayEquals(new byte[]{10, 11}, bytes);
        assertEquals("0A0B", hex.formatHex(bytes));
    }
}
