package org.openhab.binding.russound.rnet.internal;

import org.apache.commons.lang.ArrayUtils;

public class StringHexUtils {

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b & 0xff));
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String byteArrayToHex(Byte[] a) {
        return byteArrayToHex(ArrayUtils.toPrimitive(a));
    }
}