package one.helfy;

public class GetMemoryUsage {
    private static long tlabOffset;
    private static long topOffset;
    private static long startOffset;
    private static long endOffset;
    private static long allocatedBytesOffset;
    public static void main(String... args) throws Exception {
        JVM jvm = new JVM();
        Type jvmThread = jvm.type("Thread");
        allocatedBytesOffset = jvmThread.field("_allocated_bytes").offset;
        Field tlab = jvmThread.field("_tlab");
        tlabOffset = tlab.offset;
        Type tlabType = jvm.type(tlab.typeName);
        // actually both of these point to HeapWord which contains array of chars at offset 0
        topOffset = tlabType.field("_top").offset;
        startOffset = tlabType.field("_start").offset;
        endOffset = tlabType.field("_end").offset;


        java.lang.reflect.Field eetopField = Thread.class.getDeclaredField("eetop");
        eetopField.setAccessible(true);
        long jvmThreadOffset = (long) eetopField.get(Thread.currentThread());
        long threadLocalBufferTop = jvm.getAddress(jvmThreadOffset + tlabOffset + topOffset);
        long threadLocalBufferStart = jvm.getAddress(jvmThreadOffset + tlabOffset + startOffset);
        long threadLocalBufferEnd = jvm.getAddress(jvmThreadOffset + tlabOffset + endOffset);
        long allocatedBytes = jvm.getLong(jvmThreadOffset + allocatedBytesOffset);
        System.out.println(String.format("Thread tlab start: %s, thread tlab top: %s, thread tlab end: %s, diff: %s, Raw Allocated Bytes: %s",
                Long.toHexString(threadLocalBufferStart),
                Long.toHexString(threadLocalBufferTop),
                Long.toHexString(threadLocalBufferEnd),
                threadLocalBufferTop - threadLocalBufferStart + allocatedBytes,
                allocatedBytes
        ));
        int[] test = new int[500_000];
        threadLocalBufferTop = jvm.getAddress(jvmThreadOffset + tlabOffset + topOffset);
        threadLocalBufferStart = jvm.getAddress(jvmThreadOffset + tlabOffset + startOffset);
        threadLocalBufferEnd = jvm.getAddress(jvmThreadOffset + tlabOffset + endOffset);
        allocatedBytes = jvm.getLong(jvmThreadOffset + allocatedBytesOffset);
        System.out.println(String.format("Thread tlab start: %s, thread tlab top: %s, thread tlab end: %s, diff: %s, Raw Allocated Bytes: %s",
                Long.toHexString(threadLocalBufferStart),
                Long.toHexString(threadLocalBufferTop),
                Long.toHexString(threadLocalBufferEnd),
                threadLocalBufferTop - threadLocalBufferStart + allocatedBytes,
                allocatedBytes
        ));
    }

}
