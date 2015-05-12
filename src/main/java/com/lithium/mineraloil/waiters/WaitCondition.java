package com.lithium.mineraloil.waiters;

import lombok.Delegate;

public abstract class WaitCondition implements Waiter {

    @Delegate
    WaiterImpl<Waiter> waiter;

    public  WaitCondition() {
        waiter = new WaiterImpl<>(this);
    }

    public boolean checkWaitCondition(long waitTimeElapsed, int timeout) {
        return isSatisfied();
    }
}
