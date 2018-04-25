package ru.nailsoft.files.toolkit.concurrent;


import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ru.nailsoft.files.toolkit.ThreadPool;

import static ru.nailsoft.files.diagnostics.DebugUtils.safeThrow;

public class ExclusiveExecutor {
    private final int minDelay;
    private final Executor executor;
    private final Runnable task;
    private final AtomicInteger sync = new AtomicInteger(0);
    private final Runnable internalTask;
    private final Runnable restartTask;
    private final AtomicBoolean forced = new AtomicBoolean();

    public ExclusiveExecutor(int delay, Executor executor, Runnable task) {
        minDelay = delay;
        this.executor = executor;
        this.task = task;

        if (delay > 0) {
            restartTask = new Runnable() {
                @Override
                public void run() {
                    if (sync.getAndSet(0) > 1)
                        execute(false);
                }
            };

            internalTask = new Runnable() {
                @Override
                public void run() {
                    forced.set(false);
                    try {
                        ExclusiveExecutor.this.task.run();
                    } catch (Exception e) {
                        safeThrow(e);
                    }
                    if (forced.getAndSet(false))
                        restartTask.run();
                    else
                        ThreadPool.SCHEDULER.schedule(restartTask, minDelay, TimeUnit.MILLISECONDS);
                }
            };
        } else {
            restartTask = null;
            internalTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        ExclusiveExecutor.this.task.run();
                    } catch (Exception e) {
                        safeThrow(e);
                    }
                    if (sync.getAndSet(0) > 1)
                        execute(false);
                }
            };
        }
    }

    public void execute(boolean forced) {
        if (sync.getAndIncrement() > 0) {
            if (forced)
                this.forced.set(true);
            return;
        }
        executor.execute(internalTask);
    }
}
