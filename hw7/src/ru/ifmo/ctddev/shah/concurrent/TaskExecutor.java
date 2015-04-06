package ru.ifmo.ctddev.shah.concurrent;

/**
 * TaskExecutor takes tasks from given blocking queue
 * and run it.
 *
 * @author Egor Shah
 */
public class TaskExecutor implements Runnable {

    private final TaskQueue queue;
    private final ThreadLocal<Task> task = new ThreadLocal<>();

    public TaskExecutor(TaskQueue queue) {
        this.queue = queue;
    }

    @Override
    /**
     * Gets Tasks from <code>queue</code> and run it.
     */
    public void run() {
        try {
            while (!Thread.interrupted()) {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        queue.wait();
                    }
                    task.set(queue.poll());
                }
                task.get().calculateResult();
                task.remove();
            }
        } catch (InterruptedException ignored) {
        }
        Thread.currentThread().interrupt();
    }
}
