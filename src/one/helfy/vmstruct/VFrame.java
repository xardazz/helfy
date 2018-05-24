package one.helfy.vmstruct;

import one.helfy.JVMException;
import one.helfy.Utils;
import one.helfy.vmstruct.scope.Location;
import one.helfy.vmstruct.scope.ScopeDesc;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author aleksei.gromov
 * @date 26.04.2018
 */
public class VFrame extends X86Frame {
    private static int InvocationEntryBci = jvm.intConstant("InvocationEntryBci");
    private final ScopeDesc scopeDesc;

    public VFrame(long sp, long unextendedSp, long fp, long pc, Map<Integer, Long> registers, ScopeDesc scopeDesc) {
        super(sp, unextendedSp, fp, pc, registers);
        if (scopeDesc == null) {
            throw new JVMException("Scope Desc can't be null for virtual frame!");
        }
        this.cb = scopeDesc.nmethod();
        this.scopeDesc = scopeDesc;
    }

    public VFrame(Frame frame, ScopeDesc scopeDesc) {
        this(frame.sp, frame.unextendedSP, frame.fp, frame.pc, frame.registers, scopeDesc);
    }

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
            return new VFrame(this.sp, this.unextendedSP, this.fp, this.pc, this.registers, sender);
        }
        Map<Integer, Long> newRegisters = new HashMap<>(registers);
        OopMapSet.updateRegisters(scopeDesc.nmethod(), pc, unextendedSP, newRegisters);
        newRegisters.put(RBP, fp);
        return super.getNextRealFrame(scopeDesc.nmethod(), newRegisters);
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
        //out.println("Total values: " + localValues.size());
        for (int i = 0; i < maxLocals; i++) {
            String strVal;
            Object localVal = localValues.get(i);
            Method.LocalVar localVar = vars.length > 0 && i < vars.length ? vars[i] : null;
            boolean longOrDouble = localVar != null && (localVar.type.equals("long") || localVar.type.equals("double")) && (i + 1) < maxLocals;
            boolean skipNext = false;
            if (localVal == null) {
                strVal = "null";
            } else if (localVal instanceof Location) {
                Location location = (Location) localVal;
                Object obj = location.toObject(unextendedSP, registers);
                Object secondPart = null;
                if (longOrDouble) {
                    skipNext = true;
                    // if one part of long or double is location, second part will also be location
                    Location secondPartLocation = (Location) localValues.get(i + 1);
                    secondPart = secondPartLocation.toObject(unextendedSP, registers);
                }
                strVal = compiledVar2String(obj, localVar, secondPart) +
                        (location.isRegister() ? " in " + VMRegImpl.getRegisterName(location.getOffset()) : "");
            } else if (localVal instanceof Integer) {
                Object secondPart = null;
                if (localVar != null && localVal.equals(0) && longOrDouble) {
                    skipNext = true;
                    // if one part of long or double is 0 int, second part will be location or long
                    Object secondPartRaw = localValues.get(i + 1);
                    if (secondPartRaw instanceof Location) {
                        Location secondPartLocation = (Location) secondPartRaw;
                        secondPart = secondPartLocation.toObject(unextendedSP, registers);
                    } else {
                        secondPart = secondPartRaw;
                    }
                }
                strVal = compiledVar2String(localVal, localVar, secondPart);
            } else {
                strVal = String.valueOf(localVal);
            }

            String varName = localVar != null ? vars[i].name + " (" + vars[i].type + ")" : "<na>";

            out.println("\t\t[" + i + "]\t" + varName + "\t" + strVal);
            if (skipNext) i++;
        }
    }

    private String compiledVar2String(Object obj, Method.LocalVar localVar, Object secondPart) {
        if (obj == null) {
            return "(invalid)";
        }
        if (localVar != null) {
            if (localVar.isArray) {
                return Utils.getArrayAsString(obj, localVar.valueType, localVar.type, localVar.dim);
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
            return Utils.unknownObject2String(obj);
        }
    }

    protected Object[] getLocalsWithValues(int maxLocals, Method.LocalVar[] vars) {
        Object[] varValues = new Object[maxLocals];
        List<Object> localValues = scopeDesc.locals();
        for (int i = 0; i < maxLocals; i++) {
            Object localVarVal = localValues.get(i);
            boolean skipNext = false;
            Method.LocalVar localVar = vars.length > 0 && i < vars.length ? vars[i] : null;
            boolean longOrDouble = localVar != null && (localVar.type.equals("long") || localVar.type.equals("double")) && (i + 1) < maxLocals;
            if (localVarVal == null) {
                varValues[i] = null;
            } else if (localVarVal instanceof Location) {
                Location location = (Location) localVarVal;
                Object obj = location.toObject(unextendedSP, registers);
                Object secondPart = null;

                if (longOrDouble) {
                    skipNext = true;
                    // if one part of long or double is location, second part will also be location
                    Location secondPartLocation = (Location) localValues.get(i + 1);
                    secondPart = secondPartLocation.toObject(unextendedSP, registers);
                    varValues[i] = exportLocalVarValue(localVarVal, localVar, secondPart);
                    varValues[i + 1] = varValues[i];
                } else {
                    varValues[i] = exportLocalVarValue(obj, localVar, null);
                }
            } else if (localVarVal instanceof Integer) {
                Object secondPart = null;
                if (localVar != null && localVarVal.equals(0) && longOrDouble) {
                    skipNext = true;
                    // if one part of long or double is 0 int, second part will be location or long
                    Object secondPartRaw = localValues.get(i + 1);
                    if (secondPartRaw instanceof Location) {
                        Location secondPartLocation = (Location) secondPartRaw;
                        secondPart = secondPartLocation.toObject(unextendedSP, registers);
                    } else {
                        secondPart = secondPartRaw;
                    }
                }
                varValues[i] = exportLocalVarValue(localVarVal, localVar, secondPart);
                if (secondPart != null) {
                    varValues[i + 1] = varValues[i];
                }
            } else {
                varValues[i] = localVarVal;
            }

            if (skipNext) i++;
        }
        return varValues;
    }

    protected Object exportLocalVarValue(Object obj, Method.LocalVar localVar, Object secondPart) {
        if (localVar == null) {
            return obj;
        }
        if (localVar.type.equals("float")) {
            return Float.intBitsToFloat((int) obj);
        } else if (localVar.type.equals("boolean")) {
            return (int) obj == 1;
        } else if (localVar.type.equals("long") || localVar.type.equals("double")) {
            if (wordSize == 8) {
                return obj.equals(0) ? String.valueOf(secondPart) : String.valueOf(obj);
            }
            long bits = (((Integer) secondPart).longValue() & 0xffffffffL) + ((((Integer) obj).longValue() << 32L) & 0xffffffff00000000L);
            if (localVar.type.equals("long")) {
                return bits;
            } else {
                return Double.longBitsToDouble(bits);
            }
        } else if (localVar.type.equals("char")) {
            return (char) ((Integer) obj).intValue();
        }
        return obj;
    }

}
