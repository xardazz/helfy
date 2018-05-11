package one.helfy.vmstruct;

import one.helfy.Utils;
import one.helfy.vmstruct.scope.Location;
import one.helfy.vmstruct.scope.ScopeDesc;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/**
 * @author aleksei.gromov
 * @date 26.04.2018
 */
public class VFrame extends Frame {
    private static int InvocationEntryBci = jvm.intConstant("InvocationEntryBci");

    public VFrame(long sp, long unextendedSp, long fp, long pc, ScopeDesc scopeDesc) {
        super(sp, unextendedSp, fp, pc, scopeDesc);
    }

    public VFrame(Frame frame, ScopeDesc scopeDesc) {
        super(frame.sp, frame.unextendedSP, frame.fp, frame.pc, scopeDesc);
    }
//
//    @Override
//    public long local(int index) {
//
//    }
    @Override
    public int bci() {
        return scopeDesc.bci() + InvocationEntryBci;
    }

    @Override
    public long method() {
        long method = scopeDesc.method();
        return method != 0 ? method : super.method();
    }

    @Override
    public Frame sender() {
        ScopeDesc sender = scopeDesc.sender();
        if (sender != null) {
            return new VFrame(this.sp, this.unextendedSP, this.fp, this.pc, sender);
        }
        return super.getNextRealFrame(scopeDesc.nmethod());
    }

    @Override
    public void dump(PrintStream out) {
        long method = method();
        out.println("\tat " + Method.name(method) + " [compiled] [pc=" + hexString(pc) + "] @ " + scopeDesc.scopeDesc());
        dumpLocals(out, method);
    }

    @Override
    public void dumpLocals(PrintStream out, long method) {
        int maxLocals = Method.maxLocals(method);
        Method.LocalVar[] vars = Method.getLocalVars(method, bci());
        List<Object> localValues = scopeDesc.locals();
        out.println("Total values: " + localValues.size());
        for (int i = 0; i < maxLocals; i++) {
            String strVal;
            Object localVal = localValues.get(i);
            Method.LocalVar localVar = vars.length > 0 && i < vars.length ? vars[i] : null;
            boolean longOrDouble = localVar != null && (localVar.type.equals("long") || localVar.type.equals("double")) && (i + 1) < maxLocals;
            boolean skipNext = false;
            if (localVal == null) {
                strVal = "null";
            } else if (localVal instanceof Location) {
                Object obj = ((Location) localVal).toObject(unextendedSP, fp);
                Object secondPart = null;
                if (longOrDouble) {
                    skipNext = true;
                    // if one part of long or double is location, second part will also be location
                    Location secondPartLocation = (Location) localValues.get(i + 1);
                    secondPart = secondPartLocation.toObject(unextendedSP, fp);
                }
                strVal = compiledVar2String(obj, localVar, secondPart);
            } else if (localVal instanceof Integer) {
                Object secondPart = null;
                if (localVar != null && localVal.equals(0) && longOrDouble) {
                    skipNext = true;
                    // if one part of long or double is 0 int, second part will be location or long
                    Object secondPartRaw = localValues.get(i + 1);
                    if (secondPartRaw instanceof Location) {
                        Location secondPartLocation = (Location) secondPartRaw;
                        secondPart = secondPartLocation.toObject(unextendedSP, fp);
                    } else {
                        secondPart = secondPartRaw;
                    }
                }
                strVal = compiledVar2String(0, localVar, secondPart);
            } else {
                strVal = String.valueOf(localVal);
            }

            String varName = localVar != null ? vars[i].name + " (" + vars[i].type + ")" : "<na>";

            out.println("\t\t[" + i + "]\t" + varName + "\t" + strVal);
            if (skipNext) i++;
        }
    }

    private String compiledVar2String(Object obj, Method.LocalVar localVar, Object secondPart) {
        if (localVar != null) {
            if (localVar.isArray) {
                return Utils.getArrayAsString(obj, localVar.valueType, localVar.type);
            } else if (localVar.type.equals("float")) {
                return String.valueOf(Float.intBitsToFloat((int) obj));
            } else if (localVar.type.equals("boolean")) {
                return (int) obj == 1 ? "true" : "false";
            } else if (localVar.type.equals("long") || localVar.type.equals("double")) {
                if (wordSize == 8) {
                    return obj.equals(0) ? String.valueOf(secondPart) : String.valueOf(obj);
                }
                long bits =  (((Integer) secondPart).longValue() & 0xffffffffL) + ((((Integer) obj).longValue() << 32L) & 0xffffffff00000000L);
                if (localVar.type.equals("long")) {
                    return String.valueOf(bits);
                } else {
                    return String.valueOf(Double.longBitsToDouble(bits));
                }
            } else if (localVar.type.equals("char")) {
                return obj + " '" + Character.toString((char) ((Integer) obj).intValue()) + "'";
            } else {
                return String.valueOf(obj);
            }
        } else {
            Class<?> objClass = obj.getClass();
            if (objClass == byte[].class) {
                return Arrays.toString((byte[]) obj);
            } else if (objClass == short[].class) {
                return Arrays.toString((short[]) obj);
            } else if (objClass == int[].class) {
                return Arrays.toString((int[]) obj);
            } else if (objClass == long[].class) {
                return Arrays.toString((long[]) obj);
            } else if (objClass == char[].class) {
                return Arrays.toString((char[]) obj);
            } else if (objClass == float[].class) {
                return Arrays.toString((float[]) obj);
            } else if (objClass == double[].class) {
                return Arrays.toString((double[]) obj);
            } else if (objClass == boolean[].class) {
                return Arrays.toString((boolean[]) obj);
            } else if (objClass.isArray()) {
                return Arrays.deepToString((Object[]) obj);
            } else {
                return String.valueOf(obj);
            }
        }
    }

    private Object compiledVar2Object(Object obj, Method.LocalVar localVar) {
        if (localVar.type.equals("float")) {
            return Float.intBitsToFloat((int) obj);
        } else if (localVar.type.equals("boolean")) {
            return (int) obj == 1;
        }
        return obj;
    }

}
