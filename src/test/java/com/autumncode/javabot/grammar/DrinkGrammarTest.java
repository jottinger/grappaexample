package com.autumncode.javabot.grammar;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class DrinkGrammarTest {
    @DataProvider Object[][] simpleVesselTestData() {
        return new Object[][] {
                {"pint", true, Vessel.PINT},
                {"bowl", true, Vessel.BOWL},
                {"spoon", true, Vessel.SPOON},
                {"glass", true, Vessel.GLASS},
                {"fork", false, null}
        };
    }

    @Test(dataProvider = "simpleVesselTestData")
    public void testParseVessel(String input, boolean valid, Vessel resultValue) {
        VesselParser parser = Grappa.createParser(VesselParser.class);
        ListeningParseRunner<Vessel> runner=new ListeningParseRunner<>(parser.VESSEL());
        ParsingResult<Vessel> result=runner.run(input);
        assertEquals(result.isSuccess(), valid);
        if(result.isSuccess()) {
            assertEquals(result.getTopStackValue(), resultValue);
        }
    }
}
