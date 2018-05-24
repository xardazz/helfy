package one.helfy.vmstruct;

import one.helfy.JVM;
import one.helfy.Utils;
import one.helfy.vmstruct.scope.PCDesc;
import one.helfy.vmstruct.scope.ScopeDesc;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class X86Frame extends Frame {
    public static final int RBP = wordSize == 4 ? 5 : 10;

    private static final int slot_interp_bcp = -7;
    private static final int slot_interp_locals = -6;
    private static final int slot_interp_method = -3;
    private static final int slot_interp_sender_sp = -1;
    private static final int slot_link = 0;
    private static final int slot_return_addr = 1;
    private static final int slot_sender_sp = 2;


    public X86Frame(long sp, long fp, long pc, Map<Integer, Long> registers) {
        this(sp, sp, fp, pc, registers);
    }

    public X86Frame(long sp, long unextendedSP, long fp, long pc, Map<Integer, Long> registers) {
        super(sp, unextendedSP, fp, pc, registers);
    }

    public long local(int index) {
        return jvm.getAddress(at(slot_interp_locals) - index * wordSize);
    }

    public long localAddress(int index) {
        return at(slot_interp_locals) - index * wordSize;
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
            Map<Integer, Long> newRegisters = new HashMap<>(registers);
            newRegisters.put(RBP, fp);
            return Frame.getFrame(fp + slot_sender_sp * wordSize, at(slot_interp_sender_sp), at(slot_link), at(slot_return_addr), newRegisters);
        }

        if (cb != 0) {
            Map<Integer, Long> newRegisters = new HashMap<>(registers);
            OopMapSet.updateRegisters(cb, pc, unextendedSP, newRegisters);
            return getNextRealFrame(cb, newRegisters);
        }

        return null;
    }

    protected Frame getNextRealFrame(long cb, Map<Integer, Long> newRegisters) {
        long senderSP = unextendedSP + jvm.getInt(cb + _frame_size) * wordSize;
        if (senderSP != sp) {
            long senderPC = jvm.getAddress(senderSP - slot_return_addr * wordSize);
            long savedFP = jvm.getAddress(senderSP - slot_sender_sp * wordSize);
            newRegisters.put(RBP, senderSP - slot_sender_sp * wordSize);
            return Frame.getFrame(senderSP, savedFP, senderPC, newRegisters);
        }
        return null;
    }
}
