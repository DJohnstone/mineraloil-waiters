package mineraloil.waiters;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WaiterImpl<T extends Waiter> {
    private final Logger logger = LoggerFactory.getLogger(WaiterImpl.class);
    private final T waiter;
    private final String caller;
    private int timeout = (int) TimeUnit.SECONDS.toMillis(20);
    private int pollInterval = (int) TimeUnit.MILLISECONDS.toMillis(100);
    private Object result;
    private RuntimeException exception;
    private int expectedTimeElapsed;
    private long waitTimeElapsed;
    private static int activeWaiterCount = 0;

    private static List<Class> expectedExceptions = new ArrayList<>();

    @Getter
    public boolean successful;

    @Getter
    private Map<String, Exception> exceptions = new HashMap<>();

    public WaiterImpl(T waiter) {
        caller = Thread.currentThread().getStackTrace()[3].toString().replaceAll("^[^\\(]+", "");
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

    public WaiterImpl<T> expectedTimeElapsed(int expectedTimeElapsed) {
        this.expectedTimeElapsed = expectedTimeElapsed;
        return this;
    }

    public WaiterImpl<T> throwExceptionOnFailure(RuntimeException exception) {
        this.exception = exception;
        return this;
    }

    public WaiterImpl<T> waitAndIgnoreExceptions() {
        activeWaiterCount ++;
        logger.info(String.format("%sWaiter started from %s at %s", getIndentationMarker(), caller, System.currentTimeMillis()));
        try {
            waitUntilSatisfied(false);
        } catch (WaitExpiredException e) {
            // ignore
        }
        activeWaiterCount--;
        return this;
    }

    public WaiterImpl<T> waitUntilSatisfied() {
        activeWaiterCount ++;
        logger.info(String.format("%sWaiter started from %s at %s", getIndentationMarker(), caller, System.currentTimeMillis()));
        WaiterImpl<T> waiter = waitUntilSatisfied(true);
        activeWaiterCount--;
        return waiter;

    }

    private String getIndentationMarker() {
        if (activeWaiterCount == 1) {
            return "";
        } else {
            return new String(new char[activeWaiterCount-1]).replace("\0", "\t");
        }
    }

    private WaiterImpl<T> waitUntilSatisfied(boolean displayNestedExceptions) {
        successful = false;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + timeout) {
            successful = performWait(startTime);
            if (successful){
                break;
            }
        }

        if (!successful) {
            // try one last time on failure to make sure we've at least tried a minimum of two times
            successful = performWait(startTime);

            if (!successful) {
                logger.info(String.format("%sWaiter ended from %s at %s", getIndentationMarker(), caller, System.currentTimeMillis()));
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
        }
        return this;
    }

    private boolean performWait(long startTime) {
        try {
            waitTimeElapsed = System.currentTimeMillis() - startTime;
            boolean success = waiter.checkWaitCondition(waitTimeElapsed, timeout);
            if (success) {
                if (expectedTimeElapsed != 0 && waitTimeElapsed > expectedTimeElapsed) {
                    logger.warn(String.format("Wait time exceeded threshold. Expected %s, but took %s", expectedTimeElapsed, waitTimeElapsed));
                }
                return true;
            }
            Thread.sleep(pollInterval);
        } catch (InterruptedException e) {
            activeWaiterCount--;
            e.printStackTrace();
            return true;
        } catch (WaitExpiredException e) { // catch case where an inner waiter fails
            activeWaiterCount--;
            trackException(e);
        } catch (Exception e) {
            if (expectedExceptions.size() == 0 || expectedExceptions.stream().noneMatch(exception -> e.getClass().isAssignableFrom(exception))) {
                activeWaiterCount--;
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
