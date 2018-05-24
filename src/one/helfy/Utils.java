package one.helfy;

import java.util.Arrays;

/**
 * @author aleksei.gromov
 * @date 10.05.2018
 */
public class Utils {
    public static String getArrayAsString(Object val, boolean valueType, String type, int dim) {
        if (val == null) {
            return "null";
        }
        if (dim < 1) {
            return "<not an array>";
        }
        if (!valueType || dim > 1) {
            Object[] arrVal = (Object[]) val;
            return Arrays.deepToString(arrVal);
        }
        type = type.substring(0, type.indexOf("["));
        return valueTypeArray2String(val, type);
    }

    private static String valueTypeArray2String(Object val, String type) {
        switch (type) {
            case "int":
                return Arrays.toString((int[]) val);
            case "long":
                return Arrays.toString((long[]) val);
            case "short":
                return Arrays.toString((short[]) val);
            case "byte":
                return Arrays.toString((byte[]) val);
            case "double":
                return Arrays.toString((double[]) val);
            case "float":
                return Arrays.toString((float[]) val);
            case "boolean":
                return Arrays.toString((boolean[]) val);
            case "char":
                return Arrays.toString((char[]) val);
            default:
                return "unknown";
        }
    }

    public static String unknownObject2String(Object obj) {
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

    public static String strRepeat(String str, int num) {
        if (num == 1) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() * num);
        for (int i = 0; i < num; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
