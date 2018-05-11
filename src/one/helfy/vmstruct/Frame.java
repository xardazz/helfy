package one.helfy.vmstruct;

import one.helfy.JVM;
import one.helfy.JVMException;
import one.helfy.Utils;
import one.helfy.vmstruct.scope.PCDesc;
import one.helfy.vmstruct.scope.ScopeDesc;
import sun.jvm.hotspot.code.CodeBlob;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;

public class Frame {
    protected static final JVM jvm = JVM.getInstance();
    protected static final int wordSize = jvm.intConstant("oopSize");
    private static final String addressFormat = "0000000000000000".substring(0, wordSize * 2);
    private static final long _name = jvm.type("CodeBlob").offset("_name");
    private static final long _frame_size = jvm.type("CodeBlob").offset("_frame_size");
    private static final long _method = jvm.type("nmethod").offset("_method");
    private static final long _scope_desc_start = jvm.type("nmethod").offset("_scopes_data_offset");
    private static final long _scopes_pcs_offset = jvm.type("nmethod").offset("_scopes_pcs_offset");
    private static final long _narrow_oop_base = jvm.getAddress(jvm.type("Universe").global("_narrow_oop._base"));
    private static final int _narrow_oop_shift = jvm.getInt(jvm.type("Universe").global("_narrow_oop._shift"));

    private static final int slot_interp_bcp = -7;
    private static final int slot_interp_locals = -6;
    private static final int slot_interp_method = -3;
    private static final int slot_interp_sender_sp = -1;
    private static final int slot_link = 0;
    private static final int slot_return_addr = 1;
    private static final int slot_sender_sp = 2;

    protected final long sp;
    protected final long unextendedSP;
    protected final long fp;
    protected final long pc;
    protected final long cb;
    protected final ScopeDesc scopeDesc;
    protected final boolean isCompiled;

    public Frame(long sp, long fp, long pc) {
        this(sp, sp, fp, pc);
    }

    public Frame(long sp, long unextendedSP, long fp, long pc) {
        this(sp, unextendedSP, fp, pc, null);
    }

    public Frame(long sp, long unextendedSP, long fp, long pc, ScopeDesc scopeDesc) {
        this.sp = sp;
        this.unextendedSP = unextendedSP;
        this.fp = fp;
        this.pc = pc;
        if (scopeDesc != null) {
            this.cb = scopeDesc.nmethod();
            this.scopeDesc = scopeDesc;
            this.isCompiled = true;
        } else if (Interpreter.contains(pc)) {
            this.cb = 0;
            this.scopeDesc = null;
            this.isCompiled = false;
        } else {
            this.cb = CodeCache.findBlob(pc);
            if (cb != 0) {
                String name = jvm.getStringRef(cb + _name);
                if (name.endsWith("nmethod")) {
                    this.scopeDesc = PCDesc.getPCDescAt(pc, cb);
                } else {
                    this.scopeDesc = null;
                }
            } else {
                this.scopeDesc = null;
            }
            this.isCompiled = true;
        }
    }

    public long at(int slot) {
        return jvm.getAddress(fp + slot * wordSize);
    }

    public long local(int index) {
        return jvm.getAddress(at(slot_interp_locals) - index * wordSize);
    }

    public int bci() {
        long bcp = at(slot_interp_bcp);
        if (0 <= bcp && bcp <= 0xffff) {
            return (int) bcp;
        }
        return Method.bcpToBci(at(slot_interp_method), bcp);
    }

    public long method() {
        if (!isCompiled) {
            return at(slot_interp_method);
        }

        if (cb != 0) {
            String name = jvm.getStringRef(cb + _name);
            if (name.endsWith("nmethod")) {
                return jvm.getAddress(cb + _method);
            }
        }

        return 0;
    }

    public Frame sender() {
        if (!isCompiled) {
            return new Frame(fp + slot_sender_sp * wordSize, at(slot_interp_sender_sp), at(slot_link), at(slot_return_addr));
        }

        if (cb != 0) {
            if (this.scopeDesc != null) {
                ScopeDesc sender = this.scopeDesc.sender();
                if (sender != null) {
                    return new VFrame(this, sender);
                }
            }
            return getNextRealFrame(cb);
        }

        return null;
    }

    protected Frame getNextRealFrame(long cb) {
        /*
                Address senderSP = this.getUnextendedSP().addOffsetTo(cb.getFrameSize());
        Address senderPC = senderSP.getAddressAt(-1L * VM.getVM().getAddressSize());
        Address savedFPAddr = senderSP.addOffsetTo(-2L * VM.getVM().getAddressSize());
        if (map.getUpdateMap()) {
            map.setIncludeArgumentOops(cb.callerMustGCArguments());
            if (cb.getOopMaps() != null) {
                OopMapSet.updateRegisterMap(this, cb, map, true);
            }

            this.updateMapWithSavedLink(map, savedFPAddr);
        }
         */
        long senderSP = unextendedSP + jvm.getInt(cb + _frame_size) * wordSize;
        if (senderSP != sp) {
            long senderPC = jvm.getAddress(senderSP - slot_return_addr * wordSize);
            long savedFP = jvm.getAddress(senderSP - slot_sender_sp * wordSize);
            return new Frame(senderSP, savedFP, senderPC);
        }
        return null;
    }

    private String valueAsText(long value) {
        if (value >= ' ' && value <= '~') {
            return value + " '" + (char) value + "'";
        } else {
            return Long.toString(value);
        }
    }

    public void dumpLocals(PrintStream out, long method) {
        int maxLocals = Method.maxLocals(method);
        Method.LocalVar[] vars = Method.getLocalVars(method, bci());
        for (int i = 0; i < maxLocals; i++) {
            long localVarVal = local(i);
            boolean skipNext = false;
            String valAsText = null;
            Method.LocalVar localVar = vars.length > 0 && i < vars.length ? vars[i] : null;
            if (localVar != null) {
                if (!localVar.valueType && !localVar.isArray) {
                    JVM.ObjRef strRef = new JVM.ObjRef();
                    // if we have -XX:-UseCompressedOops specified base and offset will be 0
                    strRef.ptr = (int) ((localVarVal - _narrow_oop_base) >> _narrow_oop_shift);
                    Object val = jvm.getObject(strRef, jvm.fieldOffset(JVM.ObjRef.ptrField));
                    valAsText = val != null ? val.toString() : "null";
                } else if (localVar.isArray) {
                    JVM.ObjRef strRef = new JVM.ObjRef();
                    strRef.ptr = (int) ((localVarVal - _narrow_oop_base) >> _narrow_oop_shift);
                    Object val = jvm.getObject(strRef, jvm.fieldOffset(JVM.ObjRef.ptrField));
                    valAsText = Utils.getArrayAsString(val, vars[i].valueType, vars[i].type);
                } else if (localVar.type.equals("long")) {
                    // long values take 2 slots
                    long secondPart = local(i + 1);
                    if (wordSize == 8) { // 64 bit
                        valAsText = String.valueOf(secondPart);
                    } else {
                        valAsText = String.valueOf((secondPart & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L));
                    }
                    skipNext = true;
                } else if (localVar.type.equals("double")) {
                    // double values take 2 slots
                    long secondPart = local(i + 1);
                    if (wordSize == 8) {
                        valAsText = String.valueOf(Double.longBitsToDouble(secondPart));
                    } else {
                        valAsText = String.valueOf(Double.longBitsToDouble((secondPart & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L)));
                    }
                    skipNext = true;
                } else if (localVar.type.equals("char")) {
                    valAsText = localVarVal + " '" + Character.toString((char) localVarVal) + "'";
                } else if (localVar.type.equals("float")) {
                    valAsText = String.valueOf(Float.intBitsToFloat((int) localVarVal));
                }
            }
            valAsText = valAsText == null ? valueAsText(localVarVal) : valAsText;
            String varName = localVar != null ? localVar.name + " (" + localVar.type + ")" : "<na>";
            out.println("\t  [" + i + "] \t" + varName + " \t" + valAsText);
            if (skipNext) i++;
        }
    }


    public ExportedFrame export() {
        long method = method();
        int maxLocals = Method.maxLocals(method);
        Method.LocalVar[] vars = Method.getLocalVars(method, bci());
        return new ExportedFrame(method, vars, getLocalsWithValues(maxLocals, vars));
    }

    private Object[] getLocalsWithValues(int maxLocals, Method.LocalVar[] vars) {
        Object[] varValues = new Object[maxLocals];
        for (int i = 0; i < maxLocals; i++) {
            long localVarVal = local(i);
            boolean skipNext = false;
            if (vars.length > 0 && vars[i] != null) {
                if (vars[i].type.equals("long") || vars[i].type.equals("double")) {
                    long secondPart = local(i + 1);
                    varValues[i] = exportLocalVarValue(vars[i], localVarVal, secondPart);
                    varValues[i + 1] = varValues[i];
                    skipNext = true;
                } else {
                    varValues[i] = exportLocalVarValue(vars[i], localVarVal, 0);
                }
            } else {
                varValues[i] = localVarVal;
            }
            if (skipNext) i++;
        }
        return varValues;
    }

    private Object exportLocalVarValue(Method.LocalVar localVar, long localVarVal, long slot2) {
        Object result;
        if (!localVar.valueType) {
            JVM.ObjRef strRef = new JVM.ObjRef();
            strRef.ptr = (int) ((localVarVal - _narrow_oop_base) >> _narrow_oop_shift);
            result = jvm.getObject(strRef, jvm.fieldOffset(JVM.ObjRef.ptrField));
        } else if (localVar.type.equals("long")) {
            // long values take 2 slots
            if (wordSize == 8) { // 64 bit
                result = localVarVal != 0 ? localVarVal : slot2;
            } else {
                result = (slot2 & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L);
            }
        } else if (localVar.type.equals("double")) {
            // double values take 2 slots
            if (wordSize == 8) {
                result = localVarVal != 0 ? Double.longBitsToDouble(localVarVal) : Double.longBitsToDouble(slot2);
            } else {
                result = Double.longBitsToDouble((slot2 & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L));
            }
        } else if (localVar.type.equals("char")) {
            result = (char) localVarVal;
        } else if (localVar.type.equals("float")) {
            result = Float.intBitsToFloat((int) localVarVal);
        } else {
            result = localVarVal;
        }
        return result;
    }

    public void dump(PrintStream out) {
        long method = method();

        if (method == 0) {
            if (StubRoutines.returnsToCallStub(pc)) {
                String hexString = hexString(pc);
                out.println("\tat StubRoutines [" + hexString + "]");
            }
            return;
        }

        if (!isCompiled) {
            out.println("\tat " + Method.name(method) + " @ " + bci());
            dumpLocals(out, method);
            return;
        }

        if (cb != 0) {
            if (scopeDesc != null) {
                new VFrame(this, scopeDesc).dump(out);
            } else {
                out.println("\tat " + Method.name(method) + " [compiled]");
            }
        }
    }

    protected String hexString(long address) {
        String hexString = Long.toHexString(address);
        hexString = "0x" + addressFormat.substring(hexString.length()) + hexString;
        return hexString;
    }

    public static class ExportedFrame {
        private final long method;
        private final Method.LocalVar[] localVars;
        private final Object[] localVarValues;

        public ExportedFrame(long method, Method.LocalVar[] localVars, Object[] localVarValues) {
            this.method = method;
            this.localVars = localVars;
            this.localVarValues = localVarValues;
        }

        public long getMethod() {
            return method;
        }

        public Method.LocalVar[] getLocalVars() {
            return localVars;
        }

        public Object[] getLocalVarValues() {
            return localVarValues;
        }

        public void dump(PrintStream ps) {
            if (localVarValues == null) {
                return;
            }
            for (int i = 0; i < localVarValues.length; i++) {
                Object localVarValue = localVarValues[i];
                String valAsText = "\t[" + i + "]\t";
                if (localVars.length > 0) {
                    valAsText += localVars[i].name + " (" + localVars[i].type + ")\t";
                    if (localVars[i].isArray) {
                        valAsText += Utils.getArrayAsString(localVarValue, localVars[i].valueType, localVars[i].type);
                    } else if (localVars[i].type.equals("long") || localVars[i].type.equals("double")) {
                        valAsText += String.valueOf(localVarValue);
                        i++;
                    } else {
                        valAsText += String.valueOf(localVarValue);
                    }
                } else {
                    valAsText += "<na>\t" + String.valueOf(localVarValue);
                }
                ps.println(valAsText);
            }
        }
    }
}
