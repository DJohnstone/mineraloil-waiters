package mineraloil.waiters;

public interface Waiter {
    boolean isSatisfied();

    // this is a callback that the impl will call to determine what to do
    // when checking the wait condition
    boolean checkWaitCondition(long waitTimeElapsed, int timeout);
}
