package one.helfy;

import java.lang.management.GarbageCollectorMXBean;
import java.util.Collection;
import java.util.Collections;

/**
 * @author aleksei.gromov
 * @date 14.05.2018
 */
public class ThreadList {
    private static final JVM jvm = JVM.getInstance();
    private static final long _thread_list = jvm.type("Threads").global("_thread_list");
    private static final long _number_of_threads = jvm.type("Threads").global("_number_of_threads");
    private static final long _next = jvm.type("JavaThread").field("_next").offset;
    private static final long _threadObj = jvm.type("JavaThread").field("_threadObj").offset;
    private static final long _osthread = jvm.type("JavaThread").field("_osthread").offset;
    private static final long _thread_id = jvm.type("OSThread").field("_thread_id").offset;

    public static void main(String... args) {
        System.out.println("VMStructs Found threads: " + jvm.getInt(_number_of_threads));
        long curThread = jvm.getAddress(_thread_list);
        int i = 1;
        do {
            Object threadObj = JVM.Ptr2Obj.getFromPtr2Ptr(curThread + _threadObj);
            int systemThreadId = jvm.getInt(jvm.getAddress(curThread + _osthread) + _thread_id);
            System.out.println("Thread #" + i + ": " + threadObj.toString() + " [System thread id: " + systemThreadId + "]");
            curThread = jvm.getAddress(curThread + _next);
            i++;
        } while (curThread != 0);

        Collection<Thread> jThreads = Thread.getAllStackTraces().keySet();
        System.out.println("Thread.enumerate Found threads: " + jThreads.size());
        i = 1;
        for (Thread jThread : jThreads) {
            System.out.println("Thread #" + i + ": " + jThread.toString());
            i++;
        }

    }
}
