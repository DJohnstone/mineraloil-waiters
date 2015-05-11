import mineraloil.waiters.WaitCondition;
import mineraloil.waiters.WaitConditionWithFailureAction;
import mineraloil.waiters.WaitExpiredException;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.verification.AtLeast;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class WaiterTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void waiterTimeout() {
        thrown.expect(WaitExpiredException.class);

        new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).waitUntilSatisfied();
    }

    @Test
    public void waiterTimeoutIgnored() {
        Instant start = Instant.now();
        new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).waitAndIgnoreExceptions();
        Instant end = Instant.now();
        Assert.assertTrue((int) new Duration(start, end).getStandardSeconds() < 2);
    }

    @Test
    public void nestedWaiters() {
        Instant start = Instant.now();
        new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                new WaitCondition() {
                    @Override
                    public boolean isSatisfied() {
                        return false;
                    }
                }.setTimeout(TimeUnit.SECONDS, 1).waitUntilSatisfied();
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).waitAndIgnoreExceptions();
        Instant end = Instant.now();
        Assert.assertTrue((int) new Duration(start, end).getStandardSeconds() < 4);
    }


    @Test
    public void waiterTimeoutWithFailureAction() {
        class FailureAction {
          public void doSomething() {
          }
        }

        FailureAction failureAction = mock(FailureAction.class);

        new WaitConditionWithFailureAction() {
            @Override
            public void onFailureAction() {
                failureAction.doSomething();
            }

            @Override
            public boolean isSatisfied() {
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).waitAndIgnoreExceptions();
        Mockito.verify(failureAction, new AtLeast(1)).doSomething();
    }
}
