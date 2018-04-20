package one.helfy.vmstruct;

import one.helfy.JVM;

public class CodeCache {
    private static final JVM jvm = JVM.getInstance();
    private static final long codeHeap = jvm.getAddress(jvm.type("CodeCache").global("_heap"));
    private static final long _memory = codeHeap + jvm.type("CodeHeap").offset("_memory");
    private static final long _segmap = codeHeap + jvm.type("CodeHeap").offset("_segmap");
    private static final long _log2_segment_size = codeHeap + jvm.type("CodeHeap").offset("_log2_segment_size");
    private static final long _low = jvm.type("VirtualSpace").offset("_low");
    private static final long _high = jvm.type("VirtualSpace").offset("_high");
    private static final long _heap_block_size = jvm.type("HeapBlock").size;
    private static final long _name = jvm.type("CodeBlob").offset("_name");

    public static boolean contains(long pc) {
        return jvm.getAddress(_memory + _low) <= pc && pc < jvm.getAddress(_memory + _high);
    }

    public static long findBlob(long pc) {
        if (!contains(pc)) {
            return 0;
        }

        long codeHeapStart = jvm.getAddress(_memory + _low);
        int log2SegmentSize = jvm.getInt(_log2_segment_size);

        long i = (pc - codeHeapStart) >>> log2SegmentSize;
        long b = jvm.getAddress(_segmap + _low);

        int v = jvm.getByte(b + i) & 0xff;
        if (v == 0xff) {
            return 0;
        }

        while (v > 0) {
            i -= v;
            v = jvm.getByte(b + i) & 0xff;
        }

        long heapBlock = codeHeapStart + (i << log2SegmentSize);
        return heapBlock + _heap_block_size;
    }
}