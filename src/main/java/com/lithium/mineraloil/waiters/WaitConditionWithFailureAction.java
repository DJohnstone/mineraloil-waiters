package com.lithium.mineraloil.waiters;

import lombok.Delegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WaitConditionWithFailureAction implements WaiterWithFailureAction {
    private final Logger logger = LoggerFactory.getLogger(WaitConditionWithFailureAction.class);

    private final int waitTimeBeforeExecutingFailureAction = 20;

    @Delegate
    WaiterImpl<WaitConditionWithFailureAction> waiter;

    public WaitConditionWithFailureAction() {
        waiter = new WaiterImpl<>(this);
    }

    public boolean checkWaitCondition(long waitTimeElapsed, int timeout) {
        // try the onFailure if we've been in in this loop more than 20% of the allotted time
        double waiterProgressPercentage = ((double) waitTimeElapsed / (double) timeout) * 100;
        if (waiterProgressPercentage > waitTimeBeforeExecutingFailureAction) {
            logger.info(String.format("...running failure action: %s, timeout: %s, progress: %.2f", waitTimeElapsed, timeout, waiterProgressPercentage));
            onFailureAction();
        }

        boolean result = isSatisfied();
        if (!result) {
            logWaiterState(waitTimeElapsed, timeout, result, waiterProgressPercentage);
        }
        return result;
    }

    private void logWaiterState(long waitTimeElapsed, int timeout, boolean result, double waiterProgressPercentage) {
        logger.info(String.format("...waittimeElapsed: %s, timeout: %s, progress: %.2f", waitTimeElapsed, timeout, waiterProgressPercentage));
    }
}
