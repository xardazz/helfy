package one.helfy.vmstruct;

import one.helfy.JVM;

import java.nio.charset.StandardCharsets;

public class Symbol {
    private static final JVM jvm = JVM.getInstance();
    private static final long _length = jvm.type("Symbol").offset("_length");
    private static final long _body = jvm.type("Symbol").offset("_body");

    public static String asString(long symbol) {
        int length = jvm.getShort(symbol + _length) & 0xffff;

        byte[] data = new byte[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = jvm.getByte(symbol + _body + i);
        }

        return new String(data, StandardCharsets.UTF_8);
    }
}
