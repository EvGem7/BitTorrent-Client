package org.evgem.android.bittorrentclient.util;

public class NetworkUtil {
    private static int BYTE_SIZE = 8;

    public static int getPort(byte[] data) {
        if (data.length != 2) {
            return -1;
        }
        int port = data[1] & 0xFF;
        port |= (data[0] << BYTE_SIZE) & 0xFF_00;
        return port;
    }
}
