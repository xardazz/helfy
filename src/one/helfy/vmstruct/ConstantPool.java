package one.helfy.vmstruct;

import one.helfy.JVM;

public class ConstantPool {
    private static final JVM jvm = JVM.getInstance();
    private static final int wordSize = jvm.intConstant("oopSize");
    private static final long _header_size = jvm.type("ConstantPool").size;
    private static final long _pool_holder = jvm.type("ConstantPool").offset("_pool_holder");

    public static long holder(long cpool) {
        return jvm.getAddress(cpool + _pool_holder);
    }

    public static long at(long cpool, int index) {
        return jvm.getAddress(cpool + _header_size + index * wordSize);
    }
}

