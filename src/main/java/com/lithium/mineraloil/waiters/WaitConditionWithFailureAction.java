package com.lithium.mineraloil.waiters;

import lombok.Delegate;

public abstract class WaitConditionWithFailureAction implements WaiterWithFailureAction {
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
            onFailureAction();
        }

        boolean result = isSatisfied();
        return result;
    }
}
