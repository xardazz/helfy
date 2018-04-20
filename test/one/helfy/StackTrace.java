package one.helfy;

import one.helfy.vmstruct.Frame;
import one.helfy.vmstruct.JavaThread;


public class StackTrace {

    private static void dumpThread(Thread thread) {
        long vmThread = VMThread.of(thread);
        for (Frame f = JavaThread.topFrame(vmThread); f != null; f = f.sender()) {
            f.dump(System.err);
        }
    }

    public static void testDifferentParams() {
        testDifferentParams(1, new Thread[] {new Thread("unusedthread") }, 56L, new long[] {3L}, (short) 7, (byte) 4, 3.4, 5.6f, true, 'o', "test");
    }

    public static void testDifferentParams(int a, Thread[] arr, long zzz, long[] b, short c, byte d, double e, float f, boolean g, char h, String i) {
        dumpCurrentThread();
    }

    public static void dumpCurrentThread() {
        new Thread("StackTrace getter") {
            final Thread caller = Thread.currentThread();

            @Override
            public synchronized void run() {
                dumpThread(caller);
            }

            synchronized void startAndWait() {
                try {
                    start();
                    join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.startAndWait();
    }

    public static void main(String[] args) {
        testDifferentParams();
    }
}
