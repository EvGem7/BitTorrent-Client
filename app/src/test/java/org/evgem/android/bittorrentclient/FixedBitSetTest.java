package org.evgem.android.bittorrentclient;

import org.evgem.android.bittorrentclient.util.FixedBitSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FixedBitSetTest {
    @Test
    public void test() {
        byte[] arr = {(byte) 0xF0, 0x0F, 0x00};
        FixedBitSet bitSet = new FixedBitSet(arr);

        int i = 0;
        assertTrue(bitSet.getBits().get(i++));
        assertTrue(bitSet.getBits().get(i++));
        assertTrue(bitSet.getBits().get(i++));
        assertTrue(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));

        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertTrue(bitSet.getBits().get(i++));
        assertTrue(bitSet.getBits().get(i++));
        assertTrue(bitSet.getBits().get(i++));
        assertTrue(bitSet.getBits().get(i++));

        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i++));
        assertFalse(bitSet.getBits().get(i));

        byte[] restored = bitSet.toByteArray();
        assertEquals(arr.length, restored.length);
        for (i = 0; i < arr.length; ++i) {
            assertEquals(arr[i], restored[i]);
        }
    }
}
