package one.helfy;

import one.helfy.vmstruct.X86Frame;
import one.helfy.vmstruct.JavaThread;
import one.helfy.vmstruct.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class VMThreadCache {
    private static final Map<Thread, Long> threadMap = new HashMap<>(100, 1.0f);
    public static final int DEBUG_THREADS = 5;
    private static final ExecutorService debuggingExecutor = Executors.newFixedThreadPool(DEBUG_THREADS, (Runnable r) -> {
            Thread th = new Thread(r);
            th.setName("Debugging thread-" + th.getId());
            th.setDaemon(true);
            return th;
        });

    static {
        debuggingExecutor.submit(() -> {});
    }


    public static long current() {
        return of(Thread.currentThread());
    }

    public static long of(Thread th) {
        return threadMap.computeIfAbsent(th, VMThread::of);
    }

    public static X86Frame.ExportedFrame currentFrame() throws ExecutionException, InterruptedException {
        long curThread = current();
        Thread cur = Thread.currentThread();
        Future<X86Frame.ExportedFrame> future = debuggingExecutor.submit(() -> {
            while (cur.getState() != Thread.State.WAITING) {
                //Thread.sleep(1);
            }
            boolean stop = false;
            for (X86Frame f = JavaThread.topFrame(curThread); f != null; f = f.sender()) {
                if (stop) {
                    return f.export();
                }
                if (Method.name(f.method()).equals(VMThreadCache.class.getName().replace('.', '/') + ".currentFrame")) {
                    stop = true;
                }
            }
            return null;
        });
        return future.get();
    }
}
