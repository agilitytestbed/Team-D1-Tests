package nl.utwente.ing.testing;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        InitialSystemTest.class,
        CategoryRulesTest.class,
        BalanceHistoryTest.class,
        SavingGoalsTest.class,
        PaymentRequestsTest.class,
        UserMessagesTest.class,
        MessageRulesTest.class
})
public class TestSuite {

}
