package one.helfy;

import one.helfy.vmstruct.Frame;
import one.helfy.vmstruct.JavaThread;

import java.util.HashMap;
import java.util.Map;

public class VMThreadCache {
    private static final Map<Thread, Long> threadMap = new HashMap<>(100, 1.0f);

    public static long current() {
        return of(Thread.currentThread());
    }

    public static long of(Thread th) {
        return threadMap.computeIfAbsent(th, VMThread::of);
    }

    public static Frame currentFrame() {
        return JavaThread.topFrame(current());
    }
}
