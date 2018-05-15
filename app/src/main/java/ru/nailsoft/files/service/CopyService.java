package ru.nailsoft.files.service;

import ru.nailsoft.files.toolkit.ThreadPool;

public class CopyService {

    private CopyTask currentTask;

    public void enqueue(CopyTask task) {
        if(currentTask == null)
            currentTask = task;
        ThreadPool.COPY.execute(task);
    }

    public CopyTask getCurrentTask() {
        return currentTask;
    }

    public void onStart(CopyTask task) {
        currentTask = task;
    }
}
