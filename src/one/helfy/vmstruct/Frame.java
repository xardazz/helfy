package one.helfy.vmstruct;

import one.helfy.JVM;
import one.helfy.Utils;
import one.helfy.vmstruct.scope.PCDesc;
import one.helfy.vmstruct.scope.ScopeDesc;

import java.io.PrintStream;
import java.util.Map;

/**
 * @author aleksei.gromov
 * @date 24.05.2018
 */
public abstract class Frame {
    protected static final JVM jvm = JVM.getInstance();
    protected static final int wordSize = jvm.intConstant("oopSize");
    protected static final String addressFormat = "0000000000000000".substring(0, wordSize * 2);
    protected static final long _name = jvm.type("CodeBlob").offset("_name");
    protected static final long _frame_size = jvm.type("CodeBlob").offset("_frame_size");
    protected static final long _method = jvm.type("nmethod").offset("_method");

    protected final long sp;
    protected final long unextendedSP;
    public final long fp;
    protected final long pc;
    protected final Map<Integer, Long> registers;
    public long cb;
    protected final boolean isCompiled;


    protected Frame(long sp, long unextendedSP, long fp, long pc, Map<Integer, Long> registers) {
        this.sp = sp;
        this.unextendedSP = unextendedSP;
        this.fp = fp;
        this.pc = pc;
        this.registers = registers;
        if (Interpreter.contains(pc)) {
            this.cb = 0;
            this.isCompiled = false;
        } else {
            this.cb = CodeCache.findBlob(pc);
            this.isCompiled = true;
        }
    }

    public static Frame getFrame(long sp, long fp, long pc, Map<Integer, Long> registers) {
        return getFrame(sp, sp, fp, pc, registers);
    }

    public static Frame getFrame(long sp, long unextendedSP, long fp, long pc, Map<Integer, Long> registers) {
        if (!Interpreter.contains(pc)) {
            long cb = CodeCache.findBlob(pc);
            if (cb != 0) {
                String name = jvm.getStringRef(cb + _name);
                if (name.endsWith("nmethod")) {
                    ScopeDesc scopeDesc = PCDesc.getPCDescAt(pc, cb);
                    if (scopeDesc != null) {
                        return new VFrame(sp, unextendedSP, fp, pc, registers, scopeDesc);
                    }
                }
            }
        }
        return new X86Frame(sp, fp, pc, registers);
    }

    public abstract Frame sender();

    protected abstract Frame getNextRealFrame(long cb, Map<Integer, Long> newRegisters);

    public abstract long local(int i);

    public abstract long localAddress(int index);

    public abstract int bci();

    public abstract long method();

    public long at(int slot) {
        return jvm.getAddress(fp + slot * wordSize);
    }

    private String valueAsText(long value) {
        if (value >= ' ' && value <= '~') {
            return value + " '" + (char) value + "'";
        } else {
            return Long.toString(value);
        }
    }

    protected String hexString(long address) {
        String hexString = Long.toHexString(address);
        hexString = "0x" + addressFormat.substring(hexString.length()) + hexString;
        return hexString;
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
                    long localAddr = localAddress(i);
                    Object val = JVM.Ptr2Obj.getFromPtr2Ptr(localAddr);
                    valAsText = val != null ? val.toString() : "null";
                } else if (localVar.isArray) {
                    long localAddr = localAddress(i);
                    Object val = JVM.Ptr2Obj.getFromPtr2Ptr(localAddr);
                    valAsText = Utils.getArrayAsString(val, vars[i].valueType, vars[i].type, vars[i].dim);
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
                    valAsText = String.valueOf((int) localVarVal) + " '" + Character.toString((char) localVarVal) + "'";
                } else if (localVar.type.equals("float")) {
                    valAsText = String.valueOf(Float.intBitsToFloat((int) localVarVal));
                } else {
                    valAsText = String.valueOf((int) localVarVal);
                }
            }
            valAsText = valAsText == null ? valueAsText(localVarVal) : valAsText;
            String varName = localVar != null ? localVar.name + " (" + localVar.type + ")" : "<na>";
            out.println("\t  [" + i + "] \t" + varName + " \t" + valAsText);
            if (skipNext) i++;
        }
    }

    public void dump(PrintStream out) {
        long method = method();

        if (method == 0) {
//            if (StubRoutines.returnsToCallStub(pc)) {
//                String hexString = hexString(pc);
//                out.println("\tat StubRoutines [" + hexString + "]");
//            }
            return;
        }

        if (!isCompiled) {
            out.println("\tat " + Method.name(method) + " @ " + bci());
            dumpLocals(out, method);
        } else if (cb != 0) {
            out.println("\tat " + Method.name(method) + " [compiled]");
        }
    }

    public ExportedFrame export() {
        long method = method();
        int maxLocals = Method.maxLocals(method);
        Method.LocalVar[] vars = Method.getLocalVars(method, bci());
        return new ExportedFrame(method, vars, getLocalsWithValues(maxLocals, vars));
    }

    protected Object[] getLocalsWithValues(int maxLocals, Method.LocalVar[] vars) {
        Object[] varValues = new Object[maxLocals];
        for (int i = 0; i < maxLocals; i++) {
            long localVarVal = localAddress(i);
            boolean skipNext = false;
            Method.LocalVar localVar = vars.length > 0 && i < vars.length ? vars[i] : null;
            if (localVar != null) {
                if (localVar.type.equals("long") || localVar.type.equals("double")) {
                    long secondPart = local(i + 1);
                    varValues[i] = exportLocalVarValue(localVar, localVarVal, secondPart);
                    varValues[i + 1] = varValues[i];
                    skipNext = true;
                } else {
                    varValues[i] = exportLocalVarValue(localVar, localVarVal, 0);
                }
            } else {
                varValues[i] = localVarVal;
            }
            if (skipNext) i++;
        }
        return varValues;
    }

    private Object exportLocalVarValue(Method.LocalVar localVar, long localVarValPtr, long slot2) {
        Object result;
        if (!localVar.valueType || localVar.isArray) {
            return JVM.Ptr2Obj.getFromPtr2Ptr(localVarValPtr);
        }
        long localVarVal = jvm.getAddress(localVarValPtr);
        if (localVar.type.equals("long")) {
            // long values take 2 slots
            if (wordSize == 8) { // 64 bit
                result = slot2;
            } else {
                result = (slot2 & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L);
            }
        } else if (localVar.type.equals("double")) {
            // double values take 2 slots
            if (wordSize == 8) {
                result = Double.longBitsToDouble(slot2);
            } else {
                result = Double.longBitsToDouble((slot2 & 0xffffffffL) + ((localVarVal << 32L) & 0xffffffff00000000L));
            }
        } else if (localVar.type.equals("char")) {
            result = (char) localVarVal;
        } else if (localVar.type.equals("float")) {
            result = Float.intBitsToFloat((int) localVarVal);
        } else {
            result = (int) localVarVal;
        }
        return result;
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
                Method.LocalVar localVar = localVars.length > 0 && i < localVars.length ? localVars[i] : null;
                if (localVar != null) {
                    valAsText += localVar.name + " (" + localVar.type + ")\t";
                    if (localVar.isArray) {
                        valAsText += Utils.getArrayAsString(localVarValue, localVar.valueType, localVar.type, localVar.dim);
                    } else if (localVar.type.equals("long") || localVar.type.equals("double")) {
                        valAsText += String.valueOf(localVarValue);
                        i++;
                    } else {
                        valAsText += String.valueOf(localVarValue);
                    }
                } else {
                    valAsText += "<na>\t" + Utils.unknownObject2String(localVarValue);
                }
                ps.println(valAsText);
            }
        }
    }
}
