package one.helfy;

import one.helfy.vmstruct.Frame;
import one.helfy.vmstruct.JavaThread;

import java.util.concurrent.ExecutionException;


public class StackTrace {

    private static void dumpThread(Thread thread) {
        long vmThread = VMThread.of(thread);
        for (Frame f = JavaThread.topFrame(vmThread); f != null; f = f.sender()) {
            f.dump(System.err);
        }
    }

    public static void testDifferentParams(int a) {
        testDifferentParams(a, new Thread[] {new Thread("unusedthread") }, 56L, new long[] {3L}, (short) 7, (byte) 4, 3.4, 5.6f, true, 'o', "test");
    }

    public static void testDifferentParams(int a, Thread[] arr, long zzz, long[] b, short c, byte d, double e, float f, boolean g, char h, String i) {
        for (int ppp = 0; ppp <= a; ppp++) {
            System.out.print(i.substring(0, ppp % 3) + "-");
        }
        dumpCurrentThread();
    }

    public static void dumpCurrentThread() {
        long abc = 909L;
        new Thread("StackTrace getter") {
            final Thread caller = Thread.currentThread();
            final long abc1 = abc;

            @Override
            public synchronized void run() {
                System.out.println(String.valueOf(abc1));
                dumpThread(caller);
            }

            synchronized void startAndWait() {
                int cde1 = (int) abc;
                cde1++;
                try {
                    start();
                    int cde = (int) abc;
                    cde++;
                    join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.startAndWait();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        for (int i = 0; i < 1000; i++) {
            testDifferentParams(i);
        }
        int b = 9;
        VMThreadCache.currentFrame().dump(System.out);
    }
}
