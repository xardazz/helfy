
package one.helfy.vmstruct;

public class CompressedReadStream extends CompressedStream {
    public CompressedReadStream(long buffer) {
        this(buffer, 0);
    }

    public CompressedReadStream(long buffer, int position) {
        super(buffer, position);
    }

    public boolean readBoolean() {
        return this.read() != 0;
    }

    public byte readByte() {
        return (byte)this.read();
    }

    public char readChar() {
        return (char)this.readInt();
    }

    public short readShort() {
        return (short)this.readSignedInt();
    }

    public int readSignedInt() {
        return this.decodeSign(this.readInt());
    }

    public int readInt() {
        int b0 = this.read();
        return b0 < 192 ? b0 : this.readIntMb(b0);
    }

    public float readFloat() {
        return Float.intBitsToFloat(this.reverseInt(this.readInt()));
    }

    public double readDouble() {
        int rh = this.readInt();
        int rl = this.readInt();
        int h = this.reverseInt(rh);
        int l = this.reverseInt(rl);
        return Double.longBitsToDouble((long)(h << 32) | (long)l & 4294967295L);
    }

    public long readLong() {
        long low = (long)this.readSignedInt() & 4294967295L;
        long high = (long)this.readSignedInt();
        return high << 32 | low;
    }

    private int readIntMb(int b0) {
        int pos = this.position - 1;
        int sum = b0;
        int lg_H_i = 6;
        int i = 0;

        while(true) {
            ++i;
            int b_i = this.read(pos + i);
            sum += b_i << lg_H_i;
            if (b_i < 192 || i == 4) {
                this.setPosition(pos + i + 1);
                return sum;
            }

            lg_H_i += 6;
        }
    }

    private short read(int index) {
        return (short) (jvm.getByte(buffer + index) & 0xff);
    }

    private short read() {
        short retval = (short)(jvm.getByte(buffer + position) & 0xff);
        ++this.position;
        return retval;
    }
}
