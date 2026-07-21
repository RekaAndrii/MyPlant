package com.my.plant;

import org.junit.Test;

import javax.xml.bind.DatatypeConverter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class JaxbCompatibilityTest {

    @Test
    public void datatypeConverterShouldBeAvailable() {
        byte[] bytes = DatatypeConverter.parseHexBinary("0A0B");

        assertArrayEquals(new byte[] {10, 11}, bytes);
        assertEquals("0A0B", DatatypeConverter.printHexBinary(bytes));
    }
}
