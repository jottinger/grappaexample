package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VesselTest {
    VesselParser parser = Grappa.createParser(VesselParser.class);

    private void testGrammar(String corpus, boolean status, Rule rule) {
        ListeningParseRunner<Vessel> runner
                = new ListeningParseRunner<>(rule);
        ParsingResult<Vessel> result = runner.run(corpus);
        assertEquals(result.isSuccess(), status,
                "failed check on " + corpus + ", parse result was "
                        + result + " and expected " + status);
    }

    private void testGrammarResult(String corpus, boolean status, Vessel value, Rule rule) {
        ListeningParseRunner<Vessel> runner
                = new ListeningParseRunner<>(rule);
        ParsingResult<Vessel> result = runner.run(corpus);
        assertEquals(result.isSuccess(), status,
                "failed check on " + corpus + ", parse result was "
                        + result + " and expected " + status);
        if (result.isSuccess()) {
            assertEquals(result.getTopStackValue(), value);
        }
    }

    @DataProvider
    Object[][] simpleVesselParseData() {
        return new Object[][]{
                {Vessel.PINT.name(), true,},
                {Vessel.BOWL.name(), true,},
                {Vessel.GLASS.name(), true,},
                {Vessel.CUP.name(), true,},
                {Vessel.PITCHER.name(), true,},
                {Vessel.MAGNUM.name(), true,},
                {Vessel.BOTTLE.name(), true,},
                {Vessel.SPOON.name(), true,},
                {"hatful", false,},
        };
    }

    @Test(dataProvider = "simpleVesselParseData")
    public void testSimpleVesselParse(String corpus, boolean valid) {
        testGrammar(corpus, valid, parser.vessel());
    }

    @DataProvider
    Object[][] simpleVesselReturnData() {
        return new Object[][]{
                {Vessel.PINT.name(), true, Vessel.PINT},
                {Vessel.BOWL.name(), true, Vessel.BOWL},
                {Vessel.GLASS.name(), true, Vessel.GLASS},
                {Vessel.CUP.name(), true, Vessel.CUP},
                {Vessel.PITCHER.name(), true, Vessel.PITCHER},
                {Vessel.MAGNUM.name(), true, Vessel.MAGNUM},
                {Vessel.BOTTLE.name(), true, Vessel.BOTTLE},
                {Vessel.SPOON.name(), true, Vessel.SPOON},
                {"hatful", false, null},
        };
    }

    @Test(dataProvider = "simpleVesselReturnData")
    public void testSimpleVesselResult(String corpus, boolean valid, Vessel value) {
        testGrammarResult(corpus, valid, value, parser.VESSEL());
    }
}
