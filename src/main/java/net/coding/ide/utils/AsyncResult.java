/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.utils;

/**
 * Created by mingshun on 3/30/15.
 */
public class AsyncResult<T> {
    private final Object lock = new Object();
    private T result;
    private boolean done;

    public T get() {
        synchronized (this.lock) {
            try {
                this.lock.wait(30000);
            } catch (InterruptedException e) {
                // Ignore
            }

            return result;
        }
    }

    public void set(T result) {
        synchronized (this.lock) {
            this.result = result;
            this.done = true;
            this.lock.notify();
        }
    }

    public boolean isDone() {
        synchronized (this.lock) {
            return this.done;
        }
    }
}
