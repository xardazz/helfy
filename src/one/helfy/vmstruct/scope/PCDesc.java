package one.helfy.vmstruct.scope;

import one.helfy.JVM;

/**
 * @author aleksei.gromov
 * @date 26.04.2018
 */
public class PCDesc {
    private static final JVM jvm = JVM.getInstance();
    private static long _pc_offset = jvm.type("PcDesc").offset("_pc_offset");
    private static long _content_offset = jvm.type("CodeBlob").offset("_content_offset");
    private static long _size = jvm.type("PcDesc").size;
    private static long _scope_decode_offset = jvm.type("PcDesc").offset("_scope_decode_offset");
    private static long _obj_decode_offset = jvm.type("PcDesc").offset("_obj_decode_offset");
    private static long _flags = jvm.type("PcDesc").offset("_flags");
    private static int reexecuteMask = jvm.intConstant("PcDesc::PCDESC_reexecute");
    private static int isMethodHandleInvokeMask = jvm.intConstant("PcDesc::PCDESC_is_method_handle_invoke");
    private static int returnOopMask = jvm.intConstant("PcDesc::PCDESC_return_oop");
    private static final long _scope_data_start = jvm.type("nmethod").offset("_scopes_data_offset");
    private static final long _scope_pcs_start = jvm.type("nmethod").offset("_scopes_pcs_offset");
    private static final long _scope_end = jvm.type("nmethod").offset("_dependencies_offset");



    public static ScopeDesc getPCDescAt(long pc, long cb) {
        long scopesPcBegin = cb + jvm.getInt(cb + _scope_pcs_start);
        long scopesPcEnd = cb + jvm.getInt(cb + _scope_end);
        for (long pcDescSearch = scopesPcBegin; pcDescSearch < scopesPcEnd; pcDescSearch += _size) {
            if (getRealPc(cb, pcDescSearch) == pc) {
                return new ScopeDesc(cb, pcDescSearch);
            }
        }
        return null;
    }

    public static long getRealPc(long cb, long pcdesc) {
        long codeBegin = cb + jvm.getInt(cb + _content_offset);
        return codeBegin + jvm.getInt(pcdesc + _pc_offset);
    }

    public static int getScopeDecodeOffset(long pcdesc) {
        return jvm.getInt(pcdesc + _scope_decode_offset);
    }

    public static int getObjectDecodeOffset(long pcdesc) {
        return jvm.getInt(pcdesc + _obj_decode_offset);
    }
}
