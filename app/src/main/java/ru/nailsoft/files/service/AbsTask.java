package ru.nailsoft.files.service;

import java.io.File;

public abstract class AbsTask implements Runnable {
    private AbsTask.State state = AbsTask.State.NEW;
    private int progress = 0;
    private int count;
    private File currentFile;

    public State getState() {
        return state;
    }

    protected void setState(State state) {
        this.state = state;
    }

    public int getCount() {
        return count;
    }

    protected void setCount(int count) {
        this.count = count;
    }

    public int getProgress() {
        return progress;
    }

    protected void setProgress(int progress) {
        this.progress = progress;
    }

    protected void incrementProgress() {
        progress++;
    }

    public String getCurrentFile() {
        File file = this.currentFile;
        if (file == null)
            return "";
        return file.getName();
    }

    protected void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    public enum State {
        NEW, ANALIZE, PROGRESS, FINALIZE, COMPLETE, FAIL;
    }
}
