/**
 * 
 */
package lac.inf.puc.rio.br.scep.utils;

import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author vitor
 *
 */
public abstract class ShutdownableThread extends Thread {
    public String name;
    public boolean isInterruptible;

    protected ShutdownableThread(String name) {
        this(name, true);
    }

    protected ShutdownableThread(String name, boolean isInterruptible) {
        super(name);
        this.name = name;
        this.isInterruptible = isInterruptible;

        this.setDaemon(false);
        logger = LoggerFactory.getLogger(ShutdownableThread.class + "[" + name + "], ");
    }

    Logger logger;
    public AtomicBoolean isRunning = new AtomicBoolean(true);
    private CountDownLatch shutdownLatch = new CountDownLatch(1);


    public void shutdown() {
        logger.debug("Shutting down");
        isRunning.set(false);
        if (isInterruptible)
            interrupt();
        onShutDown();
        awaitShutdown();
        logger.debug("Shutdown completed");
    }

    /**
     * After calling shutdown(), use this API to wait until the shutdown is complete
     */
    public void awaitShutdown() {
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            throw new KafkaException(e);
        }
    }

    public abstract void doWork();
    
    public abstract void onShutDown();

    @Override
    public void run() {
        logger.debug("Starting ");
        try {
            while (isRunning.get()) {
                doWork();
            }
        } catch (Throwable e) {
            if (isRunning.get())
                logger.error("Error due to ", e);
        }
        shutdownLatch.countDown();
        logger.debug("Stopped ");
    }
}
