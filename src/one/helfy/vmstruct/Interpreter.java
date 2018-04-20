package one.helfy.vmstruct;

import one.helfy.JVM;

public class Interpreter {
    private static final JVM jvm = JVM.getInstance();
    private static final long code = jvm.getAddress(jvm.type("AbstractInterpreter").global("_code"));
    private static final long _stub_buffer = code + jvm.type("StubQueue").offset("_stub_buffer");
    private static final long _buffer_limit = code + jvm.type("StubQueue").offset("_buffer_limit");

    public static boolean contains(long pc) {
        long offset = pc - jvm.getAddress(_stub_buffer);
        return 0 <= offset && offset < jvm.getInt(_buffer_limit);
    }
}