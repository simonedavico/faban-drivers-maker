package cloud.benchflow.libraries;

public abstract class Monitor {

    public abstract void monitor();

    private void start() {
        //implement
    }

    private void stop() {
        //implement
    }

    final public void run() {
        start();
        monitor();
        stop();
    };

    final public sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException e) {
            Thread t = Thread.currentThread();
            t.getUncaughtExceptionHandler().uncaughtException(t, e);
        }
    }
}