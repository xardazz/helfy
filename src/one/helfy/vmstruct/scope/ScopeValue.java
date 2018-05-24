package one.helfy.vmstruct.scope;

import one.helfy.JVMException;
import one.helfy.vmstruct.CompressedReadStream;
import one.helfy.vmstruct.NMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aleksei.gromov
 * @date 26.04.2018
 */
public class ScopeValue {
    static final int LOCATION_CODE = 0;
    static final int CONSTANT_INT_CODE = 1;
    static final int CONSTANT_OOP_CODE = 2;
    static final int CONSTANT_LONG_CODE = 3;
    static final int CONSTANT_DOUBLE_CODE = 4;
    static final int CONSTANT_OBJECT_CODE = 5;
    static final int CONSTANT_OBJECT_ID_CODE = 6;

    public static Object readFrom(long nmethod, CompressedReadStream crs) {
        int varType = crs.readInt();
        switch (varType) {
            case LOCATION_CODE:
                return new Location(crs.readInt());
            case CONSTANT_INT_CODE:
                return crs.readSignedInt();
            case CONSTANT_OOP_CODE:
                return NMethod.getOopAt(nmethod, crs.readInt());
            case CONSTANT_LONG_CODE:
                return crs.readLong();
            case CONSTANT_DOUBLE_CODE:
                return crs.readDouble();
            case CONSTANT_OBJECT_CODE:
                return readObject(nmethod, crs);
            case CONSTANT_OBJECT_ID_CODE:
                return null; // todo support
            default:
                throw new JVMException("Unknown scope variable type.");
        }

    }

    private static ObjectValue readObject(long nmethod, CompressedReadStream crs) {
        int id = crs.readInt();
        Object klass = readFrom(nmethod, crs);
        if (!(klass instanceof Long)) {
            throw new JVMException("Invalid Klass object.");
        }
        int length = crs.readInt();
        ObjectValue result = new ObjectValue(id, (Long) klass);
        for (int i = 0; i < length; ++i) {
            result.addFieldValue(readFrom(nmethod, crs));
        }
        return result;
    }

    public static class ObjectValue {
        private final int id;
        private final long klass;
        private final List<Object> fieldValues = new ArrayList<>();

        public ObjectValue(int id, long klass) {
            this.id = id;
            this.klass = klass;
        }

        private void addFieldValue(Object fieldValue) {
            fieldValues.add(fieldValue);
        }
    }
}
