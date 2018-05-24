package one.helfy;

import one.helfy.vmstruct.Frame;
import one.helfy.vmstruct.Method;
import one.helfy.vmstruct.X86Frame;
import one.helfy.vmstruct.JavaThread;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class StackTrace {
    public static final int DEBUG_THREADS = 5;
    private static final ExecutorService debuggingExecutor = Executors.newFixedThreadPool(DEBUG_THREADS, (Runnable r) -> {
        Thread th = new Thread(r);
        th.setName("Debugging thread-" + th.getId());
        th.setDaemon(true);
        return th;
    });

    private static void dumpThread(Thread thread) {
        long vmThread = VMThread.of(thread);
        for (Frame f = JavaThread.topFrame(vmThread); f != null; f = f.sender()) {
            f.dump(System.err);
        }
    }

    public static void testDifferentParams(int a) {
        testDifferentParams(a, new Thread[] {new Thread("unusedthread") }, 56L, new long[] {3L}, (short) 7,
                (byte) 4, 3.4, 5.6f, true, 'o', "test", new long[][] {{70L}}, new String[][][] {{{"test 2"}}});
    }

    public static void testDifferentParams(int a, Thread[] arr, long zzz, long[] b, short c, byte d, double e, float f,
                                           boolean g, char h, String i, long[][] multiDim2, String[][][] multiDim3) {
        for (int ppp = 0; ppp <= a; ppp++) {
            i.substring(0, ppp % 3);
        }
        try {
            currentFrame().dump(System.out);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
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

    public static void main(String[] args) throws ExecutionException, InterruptedException {

//        new Thread(() -> {
//            while (true) {
//                System.gc();
//            }
//        }).start();
        for (int i = 0; i < 1000; i++) {
            testDifferentParams(i);
        }
    }

    public static X86Frame.ExportedFrame currentFrame() throws ExecutionException, InterruptedException {
        long curThread = VMThread.current();
        Thread cur = Thread.currentThread();
        Future<Frame.ExportedFrame> future = debuggingExecutor.submit(() -> {
            while (cur.getState() != Thread.State.WAITING) {
                //Thread.sleep(1);
            }
            boolean stop = false;
            for (Frame f = JavaThread.topFrame(curThread); f != null; f = f.sender()) {
                if (stop) {
                    return f.export();
                }
                if (Method.name(f.method()).equals(StackTrace.class.getName().replace('.', '/') + ".currentFrame")) {
                    stop = true;
                }
            }
            return null;
        });
        return future.get();
    }
}
