package one.helfy.vmstruct;

import one.helfy.JVM;

public class CompressedStream {
    protected static final JVM jvm = JVM.getInstance();
    protected long buffer;
    protected int position;
    public static final int LogBitsPerByte = 3;
    public static final int BitsPerByte = 8;
    public static final int lg_H = 6;
    public static final int H = 64;
    public static final int L = 192;
    public static final int MAX_i = 4;

    public CompressedStream(long buffer) {
        this(buffer, 0);
    }

    public CompressedStream(long buffer, int position) {
        this.buffer = buffer;
        this.position = position;
    }

    public long getBuffer() {
        return this.buffer;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int encodeSign(int value) {
        return value << 1 ^ value >> 31;
    }

    public int decodeSign(int value) {
        return value >>> 1 ^ -(value & 1);
    }

    public int reverseInt(int i) {
        i = (i & 1431655765) << 1 | i >>> 1 & 1431655765;
        i = (i & 858993459) << 3 | i >>> 2 & 858993459;
        i = (i & 252645135) << 4 | i >>> 4 & 252645135;
        i = i << 24 | (i & '\uff00') << 8 | i >>> 8 & '\uff00' | i >>> 24;
        return i;
    }
}
