package one.helfy.vmstruct;

import one.helfy.JVM;

import java.util.HashMap;

public class JavaThread {
    private static final JVM jvm = JVM.getInstance();
    private static final long _anchor = jvm.type("JavaThread").offset("_anchor");
    private static final long _last_Java_sp = _anchor + jvm.type("JavaFrameAnchor").offset("_last_Java_sp");
    private static final long _last_Java_pc = _anchor + jvm.type("JavaFrameAnchor").offset("_last_Java_pc");
    private static final long _last_Java_fp = _anchor + jvm.type("JavaFrameAnchor").offset("_last_Java_fp");

    public static X86Frame topFrame(long thread) {
        long lastJavaSP = jvm.getAddress(thread + _last_Java_sp);
        long lastJavaFP = jvm.getAddress(thread + _last_Java_fp);
        long lastJavaPC = jvm.getAddress(thread + _last_Java_pc);
        return X86Frame.getFrame(lastJavaSP, lastJavaFP, lastJavaPC, new HashMap<>());
    }
}