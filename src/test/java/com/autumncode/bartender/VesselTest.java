package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VesselTest {
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
        VesselParser parser = Grappa.createParser(VesselParser.class);
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
        VesselParser parser = Grappa.createParser(VesselParser.class);
        testGrammarResult(corpus, valid, value, parser.VESSEL());
    }

    @DataProvider
    Object[][] articleVesselReturnData() {
        return new Object[][]{
                {"a pint", true, Vessel.PINT},
                {"the bowl", true, Vessel.BOWL},
                {"  an GLASS", true, Vessel.GLASS},
                {"a     cup", true, Vessel.CUP},
                {"the pitcher    ", true, Vessel.PITCHER},
                {" a an magnum", false, null},
                {"bottle", true, Vessel.BOTTLE},
                {"spoon   ", true, Vessel.SPOON},
                {"spoon  bottle ", false, null},
                {"hatful", false, null},
        };
    }

    @Test(dataProvider = "articleVesselReturnData")
    public void testArticleVesselResult(String corpus, boolean valid, Vessel value) {
        VesselParser parser = Grappa.createParser(VesselParser.class);
        testGrammarResult(corpus, valid, value, parser.ARTICLEVESSEL());
    }
}
