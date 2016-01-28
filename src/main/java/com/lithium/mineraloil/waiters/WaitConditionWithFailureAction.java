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
            logger.info(String.format("waiter progress: %.2f%% (running failure action)", waiterProgressPercentage));
            onFailureAction();
        }

        boolean result = checkIsSatisfied();
        if (!result) {
            logger.info(String.format("waiter progress: %.2f%%", waiterProgressPercentage));
        }
        return result;
    }

    private boolean checkIsSatisfied() {
        boolean isIt = false;
        try {
            isIt = isSatisfied();
        } catch (Exception e) {
            logger.info(String.format("Failure action caused by isSatified returning: %s", e.toString() ));
        }
        return isIt;
    }
}
