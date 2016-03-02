package com.autumncode.javabot.grammar;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class DrinkGrammarTest {
    @DataProvider
    Object[][] simpleVesselTestData() {
        return new Object[][]{
                {"pint", true, Vessel.PINT},
                {"bowl", true, Vessel.BOWL},
                {"spoon", true, Vessel.SPOON},
                {"glass", true, Vessel.GLASS},
                {"cup", true, Vessel.CUP},
                {"pitcher", true, Vessel.PITCHER},
                {"magnum", true, Vessel.MAGNUM},
                {"bottle", true, Vessel.BOTTLE},
                {"fork", false, null},
        };
    }

    @Test(dataProvider = "simpleVesselTestData")
    public void testVesselParse(String input, boolean valid, Vessel resultValue) {
        VesselParser parser = Grappa.createParser(VesselParser.class);
        ListeningParseRunner<Vessel> runner = new ListeningParseRunner<>(parser.VESSEL());
        ParsingResult<Vessel> result = runner.run(input);
        assertEquals(result.isSuccess(), valid);
        if (result.isSuccess()) {
            assertEquals(result.getTopStackValue(), resultValue);
        }
    }

    @Test(dataProvider = "simpleVesselTestData")
    public void testArticleVesselParse(String input, boolean valid, Vessel resultValue) {
        String[] articles = {"a", "an", "the"};
        ArticleVesselParser parser = Grappa.createParser(ArticleVesselParser.class);
        ListeningParseRunner<Vessel> runner = new ListeningParseRunner<>(parser.VESSEL());
        for (String article : articles) {
            String newInput=article+" "+input;
            ParsingResult<Vessel> result = runner.run(newInput);
            assertEquals(result.isSuccess(), valid);
            if (result.isSuccess()) {
                assertEquals(result.getTopStackValue(), resultValue);
            }
        }
    }

    @Test(dataProvider = "simpleVesselTestData")
    public void testArticleVesselOfTest(String input, boolean valid, Vessel resultValue) {
        String[] articles = {"a", "an", "the"};
        ArticleVesselOfParser parser = Grappa.createParser(ArticleVesselOfParser.class);
        ListeningParseRunner<Vessel> runner = new ListeningParseRunner<>(parser.VESSEL());
        for (String article : articles) {
            String newInput=article+" "+input;
            ParsingResult<Vessel> result = runner.run(newInput);
            assertFalse(result.isSuccess()); // no of!
            result=runner.run(newInput+" of");
            assertEquals(result.isSuccess(), valid);
            if (result.isSuccess()) {
                assertEquals(result.getTopStackValue(), resultValue);
            }
        }
    }
}
