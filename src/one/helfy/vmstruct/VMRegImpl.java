package one.helfy.vmstruct;

import one.helfy.JVM;

/**
 * @author aleksei.gromov
 * @date 17.05.2018
 */
public class VMRegImpl {
    private static final JVM jvm = JVM.getInstance();
    private static final int wordSize = jvm.intConstant("oopSize");
    private static long stack0 = jvm.type("VMRegImpl").global("stack0");
    private static long regName = jvm.type("VMRegImpl").global("regName[0]");


    public static int getStack0() {
        return (int) jvm.getAddress(stack0);
    }

    public static String getRegisterName(int index) {
        return jvm.getStringRef(regName + index * wordSize);
    }

}
