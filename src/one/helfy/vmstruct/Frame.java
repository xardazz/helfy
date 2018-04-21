package one.helfy.vmstruct;

import one.helfy.JVM;
import one.helfy.JVMException;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;

public class Frame {
    private static final JVM jvm = JVM.getInstance();
    private static final int wordSize = jvm.intConstant("oopSize");
    private static final long _name = jvm.type("CodeBlob").offset("_name");
    private static final long _frame_size = jvm.type("CodeBlob").offset("_frame_size");
    private static final long _method = jvm.type("nmethod").offset("_method");
    private static final long _narrow_oop_base = jvm.getAddress(jvm.type("Universe").global("_narrow_oop._base"));
    private static final int _narrow_oop_shift = jvm.getInt(jvm.type("Universe").global("_narrow_oop._shift"));

    private static final int slot_interp_bcp = -7;
    private static final int slot_interp_locals = -6;
    private static final int slot_interp_method = -3;
    private static final int slot_interp_sender_sp = -1;
    private static final int slot_link = 0;
    private static final int slot_return_addr = 1;
    private static final int slot_sender_sp = 2;

    private final long sp;
    private final long unextendedSP;
    private final long fp;
    private final long pc;

    public Frame(long sp, long fp, long pc) {
        this(sp, sp, fp, pc);
    }

    public Frame(long sp, long unextendedSP, long fp, long pc) {
        this.sp = sp;
        this.unextendedSP = unextendedSP;
        this.fp = fp;
        this.pc = pc;
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
        if (Interpreter.contains(pc)) {
            return at(slot_interp_method);
        }

        long cb = CodeCache.findBlob(pc);
        if (cb != 0) {
            String name = jvm.getStringRef(cb + _name);
            if (name.endsWith("nmethod")) {
                return jvm.getAddress(cb + _method);
            }
        }

        return 0;
    }

    public Frame sender() {
        if (Interpreter.contains(pc)) {
            return new Frame(fp + slot_sender_sp * wordSize, at(slot_interp_sender_sp), at(slot_link), at(slot_return_addr));
        }

        long cb = CodeCache.findBlob(pc);
        if (cb != 0) {
            long senderSP = unextendedSP + jvm.getInt(cb + _frame_size);
            if (senderSP != sp) {
                long senderPC = jvm.getAddress(senderSP - slot_return_addr * wordSize);
                long savedFP = jvm.getAddress(senderSP - slot_sender_sp * wordSize);
                return new Frame(senderSP, savedFP, senderPC);
            }
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
        Method.LocalVar[] vars = Method.getLocalVars(method);
        for (int i = 0; i < maxLocals; i++) {
            long localVarVal = local(i);
            boolean skipNext = false;
            String valAsText = null;
            if (vars.length > 0 && vars[i] != null) {
                if (!vars[i].valueType && !vars[i].isArray) {
                    ObjRef strRef = new ObjRef();
                    // if we have -XX:-UseCompressedOops specified base and offset will be 0
                    strRef.ptr = (int) ((localVarVal - _narrow_oop_base) >> _narrow_oop_shift);
                    Object val = jvm.getObject(strRef, jvm.fieldOffset(ObjRef.ptrField));
                    valAsText = val != null ? val.toString() : "null";
                } else if (vars[i].isArray) {
                    ObjRef strRef = new ObjRef();
                    strRef.ptr = (int) ((localVarVal - _narrow_oop_base) >> _narrow_oop_shift);
                    Object val = jvm.getObject(strRef, jvm.fieldOffset(ObjRef.ptrField));
                    valAsText = getArrayAsString(val, vars[i].valueType, vars[i].type);
                } else if (vars[i].type.equals("long")) {
                    // long values take 2 slots
                    long secondPart = local(i + 1);
                    if (wordSize == 8) { // 64 bit
                        valAsText = "slot " + i + " -> " + String.valueOf(localVarVal);
                        valAsText += "; slot " + (i + 1) + " -> " + String.valueOf(secondPart);
                    } else {
                        valAsText = String.valueOf((secondPart & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L));
                    }
                    skipNext = true;
                } else if (vars[i].type.equals("double")) {
                    // double values take 2 slots
                    long secondPart = local(i + 1);
                    if (wordSize == 8) {
                        valAsText = "slot " + i + " -> " + String.valueOf(Double.longBitsToDouble(localVarVal));
                        valAsText += "; slot " + (i + 1) + " -> " + String.valueOf(Double.longBitsToDouble(secondPart));
                    } else {
                        valAsText = String.valueOf(Double.longBitsToDouble((secondPart & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L)));
                    }
                    skipNext = true;
                } else if (vars[i].type.equals("char")) {
                    valAsText = localVarVal + " '" + Character.toString((char) localVarVal) + "'";
                } else if (vars[i].type.equals("float")) {
                    valAsText = String.valueOf(Float.intBitsToFloat((int) localVarVal));
                }
            }
            valAsText = valAsText == null ? valueAsText(localVarVal) : valAsText;
            String varName = vars.length > 0 && vars[i] != null ? vars[i].name + " (" + vars[i].type + ")" : "<na>";
            out.println("\t  [" + i + "] \t" + varName + " \t" + valAsText);
            if (skipNext) i++;
        }
    }

    private String getArrayAsString(Object val, boolean valueType, String type) {
        if (val == null) {
            return "null";
        }
        if (!valueType) {
            Object[] arrVal = (Object[]) val;
            return Arrays.deepToString(arrVal);
        }
        switch (type) {
            case "int[]":
                return Arrays.toString((int[]) val);
            case "long[]":
                return Arrays.toString((long[]) val);
            case "short[]":
                return Arrays.toString((short[]) val);
            case "byte[]":
                return Arrays.toString((byte[]) val);
            case "double[]":
                return Arrays.toString((double[]) val);
            case "float[]":
                return Arrays.toString((float[]) val);
            case "boolean[]":
                return Arrays.toString((boolean[]) val);
            case "char[]":
                return Arrays.toString((char[]) val);
            default:
                return "unknown";
        }
    }

    public void dump(PrintStream out) {
        long method = method();
        if (method == 0) {
            return;
        }

        if (Interpreter.contains(pc)) {
            out.println("\tat " + Method.name(method) + " @ " + bci());
            dumpLocals(out, method);
            return;
        }

        long cb = CodeCache.findBlob(pc);
        if (cb != 0) {
            out.println("[compiled] " + Method.name(method));
        }
    }

    private static class ObjRef {
        private static Field ptrField;
        static {
            try {
                ptrField = ObjRef.class.getDeclaredField("ptr");
            } catch (NoSuchFieldException e) {
                throw new JVMException("Couldn't obtain ptr field of own class");
            }
        }
        int ptr;
    }
}
