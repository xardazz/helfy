package one.helfy.vmstruct.scope;

import one.helfy.vmstruct.CompressedReadStream;
import one.helfy.vmstruct.NMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author aleksei.gromov
 * @date 25.04.2018
 */
public class ScopeDesc {
    private final long nmethod;
    private final int scopeDesc;
    private final int methodOffset;
    private final int senderOffset;
    private final int localsOffset;
    private final int bci;

    public ScopeDesc(long nmethod, long pcdesc) {
        this(nmethod, pcdesc, true);
    }

    public ScopeDesc(long nmethod, long desc, boolean needsDecode) {
        this.nmethod = nmethod;
        this.scopeDesc = needsDecode ? PCDesc.getScopeDecodeOffset(desc) : (int) desc;
        CompressedReadStream crs = new CompressedReadStream(NMethod.getScopesDataBegin(nmethod), scopeDesc);
        senderOffset = crs.readInt();
        methodOffset = crs.readInt();
        bci = crs.readInt(); // non-corrected bci
        localsOffset = crs.readInt();
        int expressionsDecodeOffset = crs.readInt();
        int monitorsDecodeOffset = crs.readInt();
    }

    public ScopeDesc sender() {
        if (senderOffset == 0) {
            return null;
        }
        return new ScopeDesc(nmethod, senderOffset, false);
    }

    public int bci() {
        return bci;
    }

    public long method() {
        if (methodOffset == 0) {
            return 0;
        }
        return NMethod.getMetadataAt(nmethod, methodOffset);
    }

    public List<Object> locals() {
        return decodeScopeValues(localsOffset);
    }

    private List<Object> decodeScopeValues(int decodeOffset) {
        if (decodeOffset == 0) {
            return Collections.emptyList();
        } else {
            CompressedReadStream crs = new CompressedReadStream(NMethod.getScopesDataBegin(nmethod), decodeOffset);
            int length = crs.readInt();
            List<Object> res = new ArrayList(length);
            for(int i = 0; i < length; ++i) {
                res.add(ScopeValue.readFrom(nmethod, crs));
            }

            return res;
        }
    }

    public long nmethod() {
        return nmethod;
    }

    public int scopeDesc() {
        return scopeDesc;
    }

}
