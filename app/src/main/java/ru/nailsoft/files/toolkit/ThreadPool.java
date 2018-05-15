package ru.nailsoft.files.toolkit;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Comparator;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    public static final Handler UI = new Handler(Looper.getMainLooper());
    public static final PriorityExecutors QUICK_EXECUTORS = new PriorityExecutors(Runtime.getRuntime().availableProcessors());
    public static final ThreadPoolExecutor COPY = new ThreadPoolExecutor(1, 1, 3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    public static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledThreadPoolExecutor(1);

    public static boolean isUiThread() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return UI.getLooper().isCurrentThread();
        }
        return UI.getLooper().getThread() == Thread.currentThread();
    }

    public static class PriorityExecutors implements Comparator<Runnable> {
        private final Executor mExecutor;
        private final PriorityExecutor[] mExecutors;
        private final WeakHashMap<Runnable, Priority> mRunnablePriorityMap;

        private PriorityExecutors(int poolSize) {
            mExecutor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, new PriorityBlockingQueue<>(10, this), new MyThreadFactory());
            mExecutors = new PriorityExecutor[Priority.VALUES.length];
            for (int i = 0; i < mExecutors.length; i++)
                mExecutors[i] = new PriorityExecutor(Priority.VALUES[i]);
            mRunnablePriorityMap = new WeakHashMap<>();
        }

        @Override
        public int compare(Runnable lhs, Runnable rhs) {
            final int lhsPriorityOrdinal;
            final int rhsPriorityOrdinal;
            synchronized (mRunnablePriorityMap) {
                lhsPriorityOrdinal = mRunnablePriorityMap.get(lhs).ordinal();
                rhsPriorityOrdinal = mRunnablePriorityMap.get(rhs).ordinal();
            }

            return lhsPriorityOrdinal - rhsPriorityOrdinal;
        }

        public Executor getExecutor(Priority priority) {
            return mExecutors[priority.ordinal()];
        }

        private class PriorityExecutor implements Executor {
            private final Priority mPriority;

            public PriorityExecutor(Priority priority) {
                mPriority = priority;
            }

            @Override
            public void execute(@NonNull Runnable command) {
                synchronized (mRunnablePriorityMap) {
                    mRunnablePriorityMap.put(command, mPriority);
                }
                mExecutor.execute(command);
            }
        }
    }

    public static class MyThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        MyThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);

            t.setPriority(Thread.NORM_PRIORITY - 2);
            return t;
        }
    }

    public enum Priority {
        HIGHEST, HIGH, MEDIUM, LOW, LOWEST;

        public static final Priority[] VALUES = values();
    }
}
