package ru.nailsoft.files.service;

import ru.nailsoft.files.toolkit.ThreadPool;

public class CopyService {

    private AbsTask currentTask;

    public void enqueue(AbsTask task) {
        if(currentTask == null)
            currentTask = task;
        ThreadPool.COPY.execute(task);
    }

    public AbsTask getCurrentTask() {
        return currentTask;
    }

    public void onStart(AbsTask task) {
        currentTask = task;
    }
}
