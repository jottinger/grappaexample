package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PoliteDrinkOrderParserTest {
    PoliteDrinkOrderParser parser = Grappa.createParser(PoliteDrinkOrderParser.class);

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
                {"a glass of water please", true, new DrinkOrder(Vessel.GLASS, "water", false)},
                {"a pitcher of old 66, please", true, new DrinkOrder(Vessel.PITCHER, "old 66", false)},
                {"a pitcher of old 66", true, new DrinkOrder(Vessel.PITCHER, "old 66", false)},
                {"a glass of pinot noir, 1986", true, new DrinkOrder(Vessel.GLASS, "pinot noir, 1986", false)},
                {"a glass of pinot noir, 1986, ok?", true, new DrinkOrder(Vessel.GLASS, "pinot noir, 1986", false)},
                {"a    pint  of duck   vomit   ", true, new DrinkOrder(Vessel.PINT, "duck vomit", false)},
                {"a    pint  of duck   vomit  , please ", true, new DrinkOrder(Vessel.PINT, "duck vomit", false)},
                {" pint , duck   vomit please  ", true, new DrinkOrder(Vessel.PINT, "duck vomit", false)},
                {"a shoeful of motor oil", false, null},
                {"nothing", true, new DrinkOrder(null, null, true)},
        };
    }

    @Test(dataProvider = "drinkOrderProvider")
    public void testDrinkOrderParser(String corpus, boolean valid, DrinkOrder result) {
        testGrammarResult(corpus, valid, result, parser.ORDER());
    }
}
