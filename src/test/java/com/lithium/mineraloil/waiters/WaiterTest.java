package com.lithium.mineraloil.waiters;

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

    class TestException extends RuntimeException {
        private static final long serialVersionUID = 11464659670706970L;
    };
    class TestIgnoredException extends RuntimeException {
        private static final long serialVersionUID = -2681061731839891188L;
    };

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
    public void throwExceptionOnFailure() {
        thrown.expect(TestException.class);

        new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1)
         .throwExceptionOnFailure(new TestException())
         .waitUntilSatisfied();
    }

    @Test
    public void trapCustomExceptions() {
        WaiterImpl.addExpectedException(TestIgnoredException.class);

        thrown.expect(WaitExpiredException.class);

        new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                throw new TestIgnoredException();
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
    public void pollInterval() {
        Instant start = Instant.now();
        new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).setPollInterval(TimeUnit.SECONDS, 3).waitAndIgnoreExceptions();
        Instant end = Instant.now();
        int duration = (int) new Duration(start, end).getStandardSeconds();
        Assert.assertTrue(String.format("Took %s seconds", duration), duration > 3 && duration < 7);
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

    @Test
    public void getResult() {
        int result = (int) new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                setResult(1);
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).waitAndIgnoreExceptions().getResult();
        Assert.assertEquals(1, result);
    }

    @Test
    public void getSuccessFalse() {
        boolean result = new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                return false;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).waitAndIgnoreExceptions().isSuccessful();
        Assert.assertFalse(result);
    }

    @Test
    public void getSuccessTrue() {
        boolean result = new WaitCondition() {
            @Override
            public boolean isSatisfied() {
                return true;
            }
        }.setTimeout(TimeUnit.SECONDS, 1).waitAndIgnoreExceptions().isSuccessful();
        Assert.assertTrue(result);
    }

}
