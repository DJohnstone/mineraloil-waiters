package com.lithium.mineraloil.waiters;

public interface WaiterWithFailureAction extends Waiter{
    void onFailureAction();
}
