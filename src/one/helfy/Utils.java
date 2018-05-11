package one.helfy;

import java.util.Arrays;

/**
 * @author aleksei.gromov
 * @date 10.05.2018
 */
public class Utils {
    public static String getArrayAsString(Object val, boolean valueType, String type) {
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
}
