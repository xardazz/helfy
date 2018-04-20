package one.helfy.vmstruct;

import one.helfy.JVM;

public class Klass {
    private static final JVM jvm = JVM.getInstance();
    private static final long _name = jvm.type("Klass").offset("_name");

    static String name(long klass) {
        long symbol = jvm.getAddress(klass + _name);
        return Symbol.asString(symbol);
    }
}