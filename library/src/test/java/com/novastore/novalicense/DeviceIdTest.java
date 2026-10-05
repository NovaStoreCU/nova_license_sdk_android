package com.novastore.novalicense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeviceIdTest {

    @Test
    public void randomDeviceIdIs32HexChars() {
        String id = LicenseChecker.randomDeviceId();
        assertEquals(32, id.length());
        assertTrue(id.matches("[0-9a-f]{32}"));
    }

    @Test
    public void normalizeDeviceCodeRemovesSpacesAndDashes() {
        assertEquals("abcd1234", LicenseChecker.normalizeDeviceCode("ab-cd 1234"));
        assertEquals("abcd1234", LicenseChecker.normalizeDeviceCode("abcd1234"));
    }
}
