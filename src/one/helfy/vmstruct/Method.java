package one.helfy.vmstruct;

import one.helfy.JVM;
import one.helfy.Utils;

public class Method {
    private static final JVM jvm = JVM.getInstance();
    private static final int wordSize = jvm.intConstant("oopSize");
    private static final long _constMethod = jvm.type("Method").offset("_constMethod");
    private static final long _constMethod_size = jvm.type("ConstMethod").size;
    private static final long _constants = jvm.type("ConstMethod").offset("_constants");
    private static final long _size = jvm.type("ConstMethod").offset("_constMethod_size");
    private static final long _name_index = jvm.type("ConstMethod").offset("_name_index");
    private static final long _max_locals = jvm.type("ConstMethod").offset("_max_locals");
    private static final long _flags = jvm.type("ConstMethod").offset("_flags");
    private static final int HAS_LOCALVARIABLE_TABLE = jvm.intConstant("ConstMethod::_has_localvariable_table");
    private static final int HAS_CHECKED_EXCEPTIONS = jvm.intConstant("ConstMethod::_has_checked_exceptions");
    private static final int HAS_EXCEPTION_TABLE = jvm.intConstant("ConstMethod::_has_exception_table");
    private static final int HAS_GENERIC_SIGNATURE = jvm.intConstant("ConstMethod::_has_generic_signature");
    private static final int HAS_METHOD_ANNOTATIONS = jvm.intConstant("ConstMethod::_has_method_annotations");
    private static final int HAS_PARAMETER_ANNOTATIONS = jvm.intConstant("ConstMethod::_has_parameter_annotations");
    private static final int HAS_METHOD_PARAMETERS = jvm.intConstant("ConstMethod::_has_method_parameters");
    private static final int HAS_DEFAULT_ANNOTATIONS = jvm.intConstant("ConstMethod::_has_default_annotations");
    private static final int HAS_TYPE_ANNOTATIONS = jvm.intConstant("ConstMethod::_has_type_annotations");
    private static final int exceptionTableElementSize = jvm.type("ExceptionTableElement").size;
    private static final int methodParametersElementSize = jvm.type("MethodParametersElement").size;
    private static final int checkedExceptionElementSize = jvm.type("CheckedExceptionElement").size;
    private static final int localVariableTableElementSize = jvm.type("LocalVariableTableElement").size;
    private static final long localVarNameOffset = jvm.type("LocalVariableTableElement").field("name_cp_index").offset;
    private static final long localVarBCIOffset = jvm.type("LocalVariableTableElement").field("start_bci").offset;
    private static final long localVarLength = jvm.type("LocalVariableTableElement").field("length").offset;
    private static final long localVarDescriptorOffset = jvm.type("LocalVariableTableElement").field("descriptor_cp_index").offset;
    private static final long localVarSlotOffset = jvm.type("LocalVariableTableElement").field("slot").offset;

    public static String name(long method) {
        if (method == 0) {
            return "unknown";
        }
        long constMethod = jvm.getAddress(method + _constMethod);
        long cpool = jvm.getAddress(constMethod + _constants);
        int index = jvm.getShort(constMethod + _name_index) & 0xffff;

        String klassName = Klass.name(ConstantPool.holder(cpool));
        String methodName = Symbol.asString(ConstantPool.at(cpool, index));
        return klassName + '.' + methodName;
    }

    public static int maxLocals(long method) {
        long constMethod = jvm.getAddress(method + _constMethod);
        return jvm.getShort(constMethod + _max_locals) & 0xffff;
    }

    private static boolean hasMethodAnnotations(long flags) {
        return (flags & (long) HAS_METHOD_ANNOTATIONS) != 0L;
    }

    private static boolean hasParameterAnnotations(long flags) {
        return (flags & (long) HAS_PARAMETER_ANNOTATIONS) != 0L;
    }

    private static boolean hasTypeAnnotations(long flags) {
        return (flags & (long) HAS_TYPE_ANNOTATIONS) != 0L;
    }

    private static boolean hasDefaultAnnotations(long flags) {
        return (flags & (long) HAS_DEFAULT_ANNOTATIONS) != 0L;
    }

    private static boolean hasExceptionTable(long flags) {
        return (flags & (long) HAS_EXCEPTION_TABLE) != 0L;
    }

    private static boolean hasCheckedExceptions(long flags) {
        return (flags & (long) HAS_CHECKED_EXCEPTIONS) != 0L;
    }

    private static boolean hasMethodParameters(long flags) {
        return (flags & (long) HAS_METHOD_PARAMETERS) != 0L;
    }

    private static boolean hasGenericSignature(long flags) {
        return (flags & (long) HAS_GENERIC_SIGNATURE) != 0L;
    }

    private static boolean hasLocalVariableTable(long flags) {
        return (flags & (long) HAS_LOCALVARIABLE_TABLE) != 0L;
    }

    private static long offsetOfLastU2Element(long constMethod, long flags) {
        int offset = 0;
        if (hasMethodAnnotations(flags)) {
            ++offset;
        }

        if (hasParameterAnnotations(flags)) {
            ++offset;
        }

        if (hasTypeAnnotations(flags)) {
            ++offset;
        }

        if (hasDefaultAnnotations(flags)) {
            ++offset;
        }
        int constMethodSize = jvm.getInt(constMethod + _size);
        return constMethodSize * wordSize - (long) offset * wordSize - 2L;
    }

    private static long offsetOfMethodParameters(long constMethod, long flags) {
        if (!hasMethodParameters(flags)) {
            return 0L;
        }
        long offset = hasGenericSignature(flags) ? offsetOfLastU2Element(constMethod, flags) - 2L : offsetOfLastU2Element(constMethod, flags);
        int length = jvm.getShort(constMethod + offset) & 0xffff;
        offset -= length * methodParametersElementSize;
        return offset;
    }

    private static long offsetOfCheckedExceptions(long constMethod, long flags) {
        long offset;
        if (hasMethodParameters(flags)) {
            offset = offsetOfMethodParameters(constMethod, flags) - 2L;
        } else {
            offset = hasGenericSignature(flags) ? offsetOfLastU2Element(constMethod, flags) - 2L : offsetOfLastU2Element(constMethod, flags);
        }
        int length = jvm.getShort(constMethod + offset) & 0xffff;
        offset -= length * checkedExceptionElementSize;
        return offset;
    }

    private static long offsetOfExceptionTable(long constMethod, long flags) {
        long offset;
        if (hasCheckedExceptions(flags)) {
            offset = offsetOfCheckedExceptions(constMethod, flags) - 2L;
        } else if (hasMethodParameters(flags)) {
            offset = offsetOfMethodParameters(constMethod, flags) - 2L;
        } else {
            offset = hasGenericSignature(flags) ? offsetOfLastU2Element(constMethod, flags) - 2L : offsetOfLastU2Element(constMethod, flags);
        }
        int length = jvm.getShort(constMethod + offset) & 0xffff;
        offset -= length * exceptionTableElementSize;
        return offset;
    }

    public static LocalVar[] getLocalVars(long method, int frameBCI) {
        long constMethod = jvm.getAddress(method + _constMethod);
        long flags = jvm.getShort(constMethod + _flags) & 0xffff;
        if (!hasLocalVariableTable(flags)) {
            return new LocalVar[0];
        }

        long offset;
        if (hasExceptionTable(flags)) {
            offset = offsetOfExceptionTable(constMethod, flags) - 2L;
        } else if (hasCheckedExceptions(flags)) {
            offset = offsetOfCheckedExceptions(constMethod, flags) - 2L;
        } else if (hasMethodParameters(flags)) {
            offset = offsetOfMethodParameters(constMethod, flags) - 2L;
        } else {
            offset = hasGenericSignature(flags) ? offsetOfLastU2Element(constMethod, flags) - 2L : offsetOfLastU2Element(constMethod, flags);
        }
        int length = jvm.getShort(constMethod + offset) & 0xffff;
        if (length == 0) {
            return new LocalVar[0];
        }
        offset -= length * localVariableTableElementSize;
        LocalVar[] result = new LocalVar[length];

        long cpool = jvm.getAddress(constMethod + _constants);
        for (int j = 0; j < length; j++) {
            int typeOffset = jvm.getShort(constMethod + offset + j * localVariableTableElementSize + localVarDescriptorOffset) & 0xffff;
            int nameOffset = jvm.getShort(constMethod + offset + j * localVariableTableElementSize + localVarNameOffset) & 0xffff;
            int bciStart = jvm.getShort(constMethod + offset + j * localVariableTableElementSize + localVarBCIOffset) & 0xffff;
            int bciEnd = bciStart + jvm.getShort(constMethod + offset + j * localVariableTableElementSize + localVarLength) & 0xffff;
            int slot = jvm.getShort(constMethod + offset + j * localVariableTableElementSize + localVarSlotOffset) & 0xffff;
            if (frameBCI < bciStart || frameBCI >= bciEnd) {
                // though it's possible to get value of variable that has BCI not within the frame BCI, but in some cases
                // it can cause the crash due to var value handling based on var type
                // imagine we have 2 vars at slot 15 and both of them aren't in frame BCI. We will return the first found
                // if first found was an object, but later the slot is reused by int var it will lead to crash
                // as we'll try to obtain an object from an int considering it a pointer
                continue;
            }
            if (slot >= result.length) {
                LocalVar[] tmp = new LocalVar[slot + 1];
                System.arraycopy(result, 0, tmp, 0, result.length);
                result = tmp;
            }
            String fieldName = Symbol.asString(ConstantPool.at(cpool, nameOffset));
            result[slot] = vmLocalVar2Readable(fieldName, Symbol.asString(ConstantPool.at(cpool, typeOffset)));
        }

        return result;
    }

    public static LocalVar vmLocalVar2Readable(String fieldName, String vmType) {
        boolean isArray = vmType.startsWith("[");
        int dim = -1;
        final String dimPart;
        if (isArray) {
            while (vmType.charAt(++dim) == '[') {
            }
            vmType = vmType.substring(dim);
            dimPart = Utils.strRepeat("[]", dim);
        } else {
            dimPart = "";
        }

        if (vmType.startsWith("L")) {
            return new LocalVar(fieldName,
                    vmType.replace('/', '.').substring(1, vmType.length() - 1) + dimPart,
                    isArray,
                    dim, false
            );
        }
        String readableType;
        switch (vmType) {
            case "I":
                readableType = "int" + dimPart;
                break;
            case "J":
                readableType = "long" + dimPart;
                break;
            case "S":
                readableType = "short" + dimPart;
                break;
            case "B":
                readableType = "byte" + dimPart;
                break;
            case "D":
                readableType = "double" + dimPart;
                break;
            case "F":
                readableType = "float" + dimPart;
                break;
            case "Z":
                readableType = "boolean" + dimPart;
                break;
            case "C":
                readableType = "char" + dimPart;
                break;
            default:
                readableType = "unknown";
        }
        return new LocalVar(fieldName, readableType, isArray, dim, true);
    }

    public static int bcpToBci(long method, long bcp) {
        long constMethod = jvm.getAddress(method + _constMethod);
        return (int) (bcp - constMethod - _constMethod_size);
    }

    public static class LocalVar {
        final String name;
        final String type;
        final boolean isArray;
        final int dim;
        final boolean valueType;

        private LocalVar(String name, String type, boolean isArray, int dim, boolean valueType) {
            this.name = name;
            this.type = type;
            this.isArray = isArray;
            this.dim = dim;
            this.valueType = valueType;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public boolean isArray() {
            return isArray;
        }

        public boolean isValueType() {
            return valueType;
        }

        public int getDim() {
            return dim;
        }
    }
}