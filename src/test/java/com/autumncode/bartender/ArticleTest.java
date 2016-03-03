package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ArticleTest {
    ArticleParser parser = Grappa.createParser(ArticleParser.class);

    @DataProvider
    Object[][] articleData() {
        return new Object[][]{
                {"a", true},
                {"an", true},
                {"the", true},
                {"me", false},
                {"THE", true},
                {" a", false},
                {"a ", true},
                {"afoo", true},
        };
    }

    @DataProvider
    Object[][] fullArticleData() {
        return new Object[][]{
                {"a", true},
                {" a", true},
                {"a ", true},
                {" a ", true},
                {" a a", false},
                {"an", true},
                {"the", true},
                {"ar", false},
                {" ar", false},
        };
    }

    @Test(dataProvider = "articleData")
    public void testOnlyArticle(String article, boolean status) {
        testArticleGrammar(article, status, parser.article());
    }

    private void testArticleGrammar(String article, boolean status, Rule rule) {
        ListeningParseRunner<Void> runner
                = new ListeningParseRunner<>(rule);
        ParsingResult<Void> articleResult = runner.run(article);
        assertEquals(articleResult.isSuccess(), status,
                "failed check on " + article + ", parse result was "
                        + articleResult + " and expected " + status);
    }

    @Test(dataProvider = "fullArticleData")
    public void testARTICLE(String article, boolean status) {
        testArticleGrammar(article, status, parser.articleWithWhitespace());
    }

    @DataProvider
    Object[][] articleTerminalData() {
        return new Object[][]{
                {"a", true},
                {"an", true},
                {"the", true},
                {"me", false},
                {"THE", true},
                {" a", false},
                {"a ", false},
                {"afoo", false},
        };
    }

    @Test(dataProvider = "articleTerminalData")
    public void testArticleTerminal(String article, boolean status) {
        testArticleGrammar(article, status, parser.articleTerminal());
    }

    @DataProvider
    Object[][] articleWithWhitespaceData() {
        return new Object[][]{
                {"a", true},
                {"a      ", true},
                {"     the", true},
                {"me", false},
                {" THE ", true},
                {" a an the ", false},
                {"afoo", false},
        };
    }

    @Test(dataProvider = "articleWithWhitespaceData")
    public void testArticleWithWhitespace(String article, boolean status) {
        testArticleGrammar(article, status, parser.articleWithWhitespace());
    }

}
