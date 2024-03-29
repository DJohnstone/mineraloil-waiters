package com.lithium.mineraloil.waiters;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WaiterImpl<T extends Waiter> {
    private final Logger logger = LoggerFactory.getLogger(WaiterImpl.class);
    private final T waiter;
    private final String callerClass;
    private final String callerFileName;
    private int timeout = (int) TimeUnit.SECONDS.toMillis(20);
    private int pollInterval = (int) TimeUnit.MILLISECONDS.toMillis(100);
    private Object result;
    private RuntimeException exception;
    private long waitTimeElapsed;
    private static int activeWaiterCount = 0;

    private static Set<Class> expectedExceptions = new HashSet<>();

    @Getter
    public boolean successful;

    @Getter
    private Map<String, Exception> exceptions = new HashMap<>();

    public WaiterImpl(T waiter) {
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        callerClass = callStack[3].getClassName();
        callerFileName = callStack[3].getFileName();
        this.waiter = waiter;
    }

    public static void addExpectedException(Class exceptionClass) {
        expectedExceptions.add(exceptionClass);
    }

    public void setResult(Object obj) {
        result = obj;
    }

    public Object getResult() {
        return result;
    }

    public WaiterImpl<T> setTimeout(TimeUnit timeunit, int duration) {
        this.timeout = (int) timeunit.toMillis(duration);
        return this;
    }

    public WaiterImpl<T> setPollInterval(TimeUnit timeunit, int duration) {
        this.pollInterval = (int) timeunit.toMillis(duration);
        return this;
    }

    public WaiterImpl<T> throwExceptionOnFailure(RuntimeException exception) {
        this.exception = exception;
        return this;
    }

    public WaiterImpl<T> waitAndIgnoreExceptions() {
        try {
            activeWaiterCount++;
            logWaitEvent();
            try {
                waitUntilSatisfied(false);
            } catch (WaitExpiredException e) {
                // ignore
            }
        } finally {
            activeWaiterCount--;
        }
        return this;
    }

    private void logWaitEvent() {
        if (activeWaiterCount <= 1 && !callerClass.contains("com.lithium.mineraloil.selenium")) {
            logger.debug(String.format("Waiter called from %s, %s ms", callerFileName, timeout));
        } else {
            String indentation = new String(new char[activeWaiterCount - 1]).replace("\0", "\t");
            logger.debug(String.format("%sWaiter called from %s, %s ms", indentation, callerFileName, timeout));
        }
    }

    public WaiterImpl<T> waitUntilSatisfied() {
        WaiterImpl<T> waiter = null;
        try {
            activeWaiterCount++;
            logWaitEvent();
            waiter = waitUntilSatisfied(true);
        } finally {
            activeWaiterCount--;
        }
        return waiter;

    }

    private WaiterImpl<T> waitUntilSatisfied(boolean displayNestedExceptions) {
        successful = false;
        long startTime = System.currentTimeMillis();
        int loopCount = 0;
        while (System.currentTimeMillis() < startTime + timeout) {
            loopCount++;
            successful = performWait(startTime);
            if (successful) {
                break;
            }
        }

        if (!successful) {
            if (loopCount < 2) successful = performWait(startTime); // try at least twice

            if (!successful) {
                if (displayNestedExceptions && waiter instanceof WaitCondition && exceptions.size() > 0) {
                    int counter = 0;
                    for (Exception exception : exceptions.values()) {
                        counter++;
                        logger.warn(String.format("Nested wait exception: (%s of %s): ", counter, exceptions.size()), exception);
                    }
                }
                if (exception == null) {
                    throw new WaitExpiredException(String.format("Timed out in waiter in %s milliseconds", timeout));
                } else {
                    throw exception;
                }
            }
        } else {
        }

        return this;
    }

    private boolean performWait(long startTime) {
        try {
            waitTimeElapsed = System.currentTimeMillis() - startTime;
            boolean success = waiter.checkWaitCondition(waitTimeElapsed, timeout);
            if (success) {
                return true;
            }
            Thread.sleep(pollInterval);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        } catch (WaitExpiredException e) { // catch case where an inner waiter fails
            trackException(e);
        } catch (Exception e) {
            if (expectedExceptions.size() == 0 || expectedExceptions.stream().noneMatch(exception -> e.getClass().isAssignableFrom(exception))) {
                throw e;
            }
        }
        return false;
    }

    // only gather one trace for each type of exception so we don't show dups in the log
    private void trackException(Exception e) {
        String exceptionName = e.getClass().toString();
        if (!exceptions.containsKey(exceptionName)) exceptions.put(exceptionName, e);
    }
}
