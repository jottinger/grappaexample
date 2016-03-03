package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DrinkOrderParserTest {
    DrinkOrderParser parser=Grappa.createParser(DrinkOrderParser.class);

    private void testGrammarResult(String corpus, boolean status, DrinkOrder value, Rule rule) {
        ListeningParseRunner<DrinkOrder> runner
                = new ListeningParseRunner<>(rule);
        ParsingResult<DrinkOrder> result = runner.run(corpus);
        assertEquals(result.isSuccess(), status,
                "failed check on " + corpus + ", parse result was "
                        + result + " and expected " + status);
        if (result.isSuccess()) {
            assertEquals(result.getTopStackValue(), value);
        }
    }

    @DataProvider
    public Object[][] drinkOrderProvider() {
        return new Object[][]{
                {"a glass of water", true, new DrinkOrder(Vessel.GLASS, "water", false)},
                {"a shoeful of motor oil", false, null},
                {"nothing", true, new DrinkOrder(null, null, true)},
        };
    }

    @Test(dataProvider = "drinkOrderProvider")
    public void testDrinkOrderParser(String corpus, boolean valid, DrinkOrder result) {
        testGrammarResult(corpus, valid, result, parser.DRINKORDER());
    }
}
