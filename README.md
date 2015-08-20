# mineraloil-waiters
Easy way to add polling waits


### Maven 

```
<dependency>
    <groupId>com.lithium.mineraloil</groupId>
    <artifactId>waiters</artifactId>
    <version>0.1.14</version>
</dependency>
```

### WaitCondition

WaitCondition is an abstract class and is intended to be used inline with your code
to perform a polling wait. If the wait condition is met, the code exits the waiter. If
the condition is not met, the waiter will sleep a few hundred ms and then try again. By 
default the waiter will continue to check for a true result for 20s but this is of course 
configurable. 

This approach is FAR SUPERIOR to a sleep(). The problem with sleeps is that you have to 
guess how long to wait and if you're wrong, you still have a problem. Further, a sleep() 
guarantees the program is idle, even if the thing you're waiting for happens immediately. 
With the WaitCondition, the wait time is entirely dependent on how long it takes your 
condition to be met. 

For example:

```
new WaitCondition() {
    @Override
    public boolean isSatisfied() {
        return [some code that returns a boolean when the condition is met];
    }
}.waitUntilSatisfied();
```

You'll notice that we're calling the waitUntilSatisfied() method. That will continue 
the polling wait. It will queitly continue if isSatisfied() returns true and throw a
WaitExpiredException if it times out. 

There are additional methods you can call and these can be chained in a fluent manner. For
example, you could set a longer timeout using:

```
new WaitCondition() {
    @Override
    public boolean isSatisfied() {
        return [some code that returns a boolean when the condition is met];
    }
}.setTimeout(TimeUnit.SECONDS, 60).waitUntilSatisfied();
```

#### waitUntilSatisfied

This is the most commonly used waiter and throws a WaitExpiredException on failure

#### setTimeout(TimeUnit timeUnit, int duration)

Set how long we should wait. The default is 20s

#### setPollInterval(TimeUnit timeUnit, int duration)

Set how long we should wait in-between checking the wait condition. The default is 100ms

#### waitAndIgnoreExceptions

Sometimes it's a best effort and you want to continue along. The other use case is
if all you're interested in is the final state - was it satisfied or not? in that case
you could call .waitAndIgnoreExceptions.isSatisfied()

#### isSatisfied

This should come after a waitAndIgnoreExceptions and returns the last value we got from isSatisfied 
in the waiter

#### throwExceptionOnFailure(RuntimeException exception)

Allows you to throw a different exception instead of WaitExpiredException

### WaitConditionWithFailureAction

This class is a permutation of the wait condition that allows you to perform an action 
if you have not seen a success after 20% of the wait time has elapsed. This is most 
useful when you need to work around an existing problem that is out of your control.
 
For example, there's a UI issue that will never get fixed that requires the user to (sometimes)
refresh the page to get the expected result. 

```
new WaitConditionWithFailureAction() {
    @Override
    public void onFailureAction() {
        [refresh the page]
    }

    @Override
    public boolean isSatisfied() {
        return [your condition here];
    }
}.waitUntilSatisfied
```

### Ignoring Exceptions 

WaiterImpl.addExpectedException(RuntimeException.class)

You can call the static method addExpectedException as many times as needed to tell the waiters to
always ignore exceptions of these types. This is useful in Selenium testing, for example to 
make sure you're always waiting and retrying in the event of StaleElement. Currently we're using 

```
WaiterImpl.addExpectedException(StaleElementReferenceException.class);
WaiterImpl.addExpectedException(NoSuchElementException.class);
WaiterImpl.addExpectedException(ElementNotVisibleException.class);
WaiterImpl.addExpectedException(WebDriverException.class);
WaiterImpl.addExpectedException(MoveTargetOutOfBoundsException.class);
```





