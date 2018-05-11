package one.helfy.vmstruct;

import one.helfy.JVM;

/**
 * @author aleksei.gromov
 * @date 26.04.2018
 */
public class NMethod {
    private static final JVM jvm = JVM.getInstance();

    private static final int wordSize = jvm.intConstant("oopSize");
    private static final long _scope_data_begin = jvm.type("nmethod").offset("_scopes_data_offset");
    private static final long _scope_start = jvm.type("nmethod").offset("_scopes_pcs_offset");
    private static final long _scope_end = jvm.type("nmethod").offset("_dependencies_offset");
    private static final long _metadata_begin = jvm.type("nmethod").offset("_metadata_offset");
    private static final long _oops_begin = jvm.type("nmethod").offset("_oops_offset");
    private static final long _compile_level = jvm.type("nmethod").offset("_comp_level");

    public static long getMetadataAt(long cb, int index) {
        if (index == 0) {
            return 0;
        }
        //senderPC = senderSP.getAddressAt(-1L * VM.getVM().getAddressSize());
        //long senderPC = jvm.getAddress(senderSP - slot_return_addr * wordSize);
        return jvm.getAddress(getMetadataBegin(cb) + (index - 1) * wordSize);
    }

    public static long getOopAt(long cb, int index) {
        if (index == 0) {
            return 0;
        }
        return jvm.getAddress(getOopBegin(cb) + (index - 1) * wordSize);
    }

    public static long getScopesDataBegin(long cb) {
        return cb + jvm.getInt(cb + _scope_data_begin);
    }

    private static long getMetadataBegin(long cb) {
        return cb + jvm.getInt(cb + _metadata_begin);
    }

    private static long getOopBegin(long cb) {
        return cb + jvm.getInt(cb + _oops_begin);
    }

    public static long getCompileLevel(long cb) {
        return jvm.getInt(cb + _compile_level);
    }

}
