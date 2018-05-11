package one.helfy.vmstruct.scope;

import one.helfy.JVM;

import static one.helfy.vmstruct.scope.Location.Type.OOP;
import static one.helfy.vmstruct.scope.Location.Where.IN_REGISTER;
import static one.helfy.vmstruct.scope.Location.Where.ON_STACK;

/**
 * @author aleksei.gromov
 * @date 26.04.2018
 */
public class Location {
    private static JVM jvm = JVM.getInstance();
    private static int OFFSET_MASK = jvm.intConstant("Location::OFFSET_MASK");
    private static int OFFSET_SHIFT = jvm.intConstant("Location::OFFSET_SHIFT");
    private static int TYPE_MASK = jvm.intConstant("Location::TYPE_MASK");
    private static int TYPE_SHIFT = jvm.intConstant("Location::TYPE_SHIFT");
    private static int WHERE_MASK = jvm.intConstant("Location::WHERE_MASK");
    private static int WHERE_SHIFT = jvm.intConstant("Location::WHERE_SHIFT");
    private static int TYPE_NORMAL = jvm.intConstant("Location::normal");
    private static int TYPE_OOP = jvm.intConstant("Location::oop");
    private static int TYPE_NARROWOOP = jvm.intConstant("Location::narrowoop");
    private static int TYPE_INT_IN_LONG = jvm.intConstant("Location::int_in_long");
    private static int TYPE_LNG = jvm.intConstant("Location::lng");
    private static int TYPE_FLOAT_IN_DBL = jvm.intConstant("Location::float_in_dbl");
    private static int TYPE_DBL = jvm.intConstant("Location::dbl");
    private static int TYPE_ADDR = jvm.intConstant("Location::addr");
    private static int TYPE_INVALID = jvm.intConstant("Location::invalid");
    private static int WHERE_ON_STACK = jvm.intConstant("Location::on_stack");
    private static int WHERE_IN_REGISTER = jvm.intConstant("Location::in_register");
    private static final long _narrow_oop_base = jvm.getAddress(jvm.type("Universe").global("_narrow_oop._base"));
    private static final int _narrow_oop_shift = jvm.getInt(jvm.type("Universe").global("_narrow_oop._shift"));

    private final int value;

    Location(int value) {
        this.value = value;
    }

    public Location.Where getWhere() {
        int where = (this.value & WHERE_MASK) >> WHERE_SHIFT;
        if (where == WHERE_ON_STACK) {
            return ON_STACK;
        } else if (where == WHERE_IN_REGISTER) {
            return Location.Where.IN_REGISTER;
        } else {
            throw new RuntimeException("should not reach here");
        }
    }

    public Location.Type getType() {
        int type = (this.value & TYPE_MASK) >> TYPE_SHIFT;
        if (type == TYPE_NORMAL) {
            return Location.Type.NORMAL;
        } else if (type == TYPE_OOP) {
            return OOP;
        } else if (type == TYPE_NARROWOOP) {
            return Location.Type.NARROWOOP;
        } else if (type == TYPE_INT_IN_LONG) {
            return Location.Type.INT_IN_LONG;
        } else if (type == TYPE_LNG) {
            return Location.Type.LNG;
        } else if (type == TYPE_FLOAT_IN_DBL) {
            return Location.Type.FLOAT_IN_DBL;
        } else if (type == TYPE_DBL) {
            return Location.Type.DBL;
        } else if (type == TYPE_ADDR) {
            return Location.Type.ADDR;
        } else if (type == TYPE_INVALID) {
            return Location.Type.INVALID;
        } else {
            throw new RuntimeException("should not reach here");
        }
    }

    public short getOffset() {
        return (short)((this.value & OFFSET_MASK) >> OFFSET_SHIFT);
    }

    public Object toObject(long unextendedSP) {
        Where where = getWhere();
        int type = (this.value & TYPE_MASK) >> TYPE_SHIFT;
        if (where == Where.ON_STACK) {
            if (type == TYPE_OOP) {
                long objAddr = jvm.getAddress(unextendedSP + 4 * getOffset());
                JVM.ObjRef objRef = new JVM.ObjRef();
                objRef.ptr = (int) ((objAddr - _narrow_oop_base) >> _narrow_oop_shift);
                return jvm.getObject(objRef, jvm.fieldOffset(JVM.ObjRef.ptrField));
            } else if (type == TYPE_NARROWOOP) {
                long objAddr = jvm.getAddress(unextendedSP + 4 * getOffset());
                JVM.ObjRef objRef = new JVM.ObjRef();
                objRef.ptr = (int) (objAddr);
                return jvm.getObject(objRef, jvm.fieldOffset(JVM.ObjRef.ptrField));
            } else if (type == TYPE_NORMAL) {
                // in 32-bit JVM normal also means half of double or half of long
                int normalVal = jvm.getInt(unextendedSP + 4 * getOffset());
                return normalVal;
            } else if (type == TYPE_DBL || type == TYPE_FLOAT_IN_DBL) {
                long dblBits = jvm.getLong(unextendedSP + 4 * getOffset());
                return Double.longBitsToDouble(dblBits);
            } else if (type == TYPE_LNG || type == TYPE_INT_IN_LONG) {
                return jvm.getLong(unextendedSP + 4 * getOffset());
            }
        }
        return toString();
    }

    @Override
    public String toString() {
        return this.value + " (" + getType().toString() + ")"
                + (getWhere() == IN_REGISTER ? " in register " + getOffset() : "");
    }

    public static class Type {
        public static final Type NORMAL = new Type("normal");
        public static final Type OOP = new Type("oop");
        public static final Type NARROWOOP = new Type("narrowoop");
        public static final Type INT_IN_LONG = new Type("int_in_long");
        public static final Type LNG = new Type("lng");
        public static final Type FLOAT_IN_DBL = new Type("float_in_dbl");
        public static final Type DBL = new Type("dbl");
        public static final Type ADDR = new Type("addr");
        public static final Type INVALID = new Type("invalid");
        private String value;

        private Type(String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }

        public int getValue() {
            if (this == NORMAL) {
                return TYPE_NORMAL;
            } else if (this == OOP) {
                return TYPE_OOP;
            } else if (this == NARROWOOP) {
                return TYPE_NARROWOOP;
            } else if (this == INT_IN_LONG) {
                return TYPE_INT_IN_LONG;
            } else if (this == LNG) {
                return TYPE_LNG;
            } else if (this == FLOAT_IN_DBL) {
                return Location.TYPE_FLOAT_IN_DBL;
            } else if (this == DBL) {
                return Location.TYPE_DBL;
            } else if (this == ADDR) {
                return Location.TYPE_ADDR;
            } else if (this == INVALID) {
                return Location.TYPE_INVALID;
            } else {
                throw new RuntimeException("should not reach here");
            }
        }
    }

    public static class Where {
        public static final Where ON_STACK = new Where("on_stack");
        public static final Where IN_REGISTER = new Where("in_register");
        private String value;

        private Where(String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }

        public int getValue() {
            if (this == ON_STACK) {
                return WHERE_ON_STACK;
            } else if (this == IN_REGISTER) {
                return Location.WHERE_IN_REGISTER;
            } else {
                throw new RuntimeException("should not reach here");
            }
        }
    }
}
