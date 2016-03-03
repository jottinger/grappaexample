Grappa is a parser library for Java. It's a fork of Parboiled, which focuses more on Scala as a runtime environment; Grappa seems to use more Java idioms than Parboiled does.

Grappa's similar in focus to other libraries like ANTLR and JavaCC; the main advantage to using something like Grappa instead of ANTLR is in the lack of a processing phase. With ANTLR and JavaCC, you have a grammar file, which then generates a lexer and a parser in Java source code. Then you compile that generated source to get your parser.

Grappa (and Parboiled) represent the grammar in actual source code, so there is no external phase; this makes programming with them *feel* faster. It's certainly easier to integrate with tooling, since there is no separate tool to invoke apart from the compiler itself.

I'd like to walk through a simple experience of *using* Grappa, to perhaps help expose how Grappa works.

## The Goal

What I want to do is mirror a tutorial I found for ANTLR, "[ANTLR 4: using the lexer, parser and listener with example grammar](http://www.theendian.com/blog/antlr-4-lexer-parser-and-listener-with-example-grammar/)." It's an okay tutorial, but the main thing *I* thought after reading was: "Hmm, ANTLR's great, everyone uses it, but let's see if there are alternatives."

> That led me to Parboiled, but some Parboiled users recommended Grappa for Java, so here we are.

That tutorial basically writes a parser for drink orders. 

Imagine an automated bartender: "What're ya havin?"

Well... let's automate that bartender, such that he can parse responses like "`A pint of beer`." We can imagine more variations on this, but we're going to center on one: we'd also like to allow our bartender to parse orders from people who're a bit too inebriated to use the introductory article: "`glass of wine`" (no `a`) should also be acceptable.

Let's take a look at our Bartender's source code, just to set the stage for our grammar. (Actually, we'll be writing multiple grammars, because we want to take it in small pieces.)

<pre>package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;

import java.util.Scanner;

public class Bartender {
    public static void main(String[] args) {
        new Bartender().run();
    }

    public void run() {
        final Scanner scanner = new Scanner(System.in);
        boolean done = false;
        do {
            writePrompt();
            String order = scanner.nextLine();
            done = order == null || handleOrder(order);
        } while (!done);
    }

    private void writePrompt() {
        System.out.print("Your order? ");
        System.out.flush();
    }

    private boolean handleOrder(String order) {
        DrinkOrderParser parser
                = Grappa.createParser(DrinkOrderParser.class);
        ListeningParseRunner&lt;DrinkOrder&gt; runner
                = new ListeningParseRunner&lt;&gt;(parser.DRINKORDER());
        ParsingResult&lt;DrinkOrder&gt; result = runner.run(order);
        DrinkOrder drinkOrder;
        boolean done = false;
        if (result.isSuccess()) {
            drinkOrder = result.getTopStackValue();
            done = drinkOrder.isTerminal();
            if (!done) {
                System.out.printf("Here's your %s of %s. Please drink responsibly!%n",
                        drinkOrder.getVessel().toString().toLowerCase(),
                        drinkOrder.getDescription());
            }
        } else {
            System.out.println("I'm sorry, I don't understand. Try again?");
        }
        return done;
    }
}</pre>
This isn't the world's greatest command line application, but it serves to get the job done. We don't have to worry about `handleOrder` yet - we'll explain it as we go through generating a grammar.

## What it does

Grappa describes a grammar as a set of `Rule`s. A rule can describe a match or an action; both matches and actions return boolean values to indicate success. A rule has failed when processing sees `false` in its stream.

Let's generate a very small parser for the sake of example. Our first parser (`ArticleParser`) is going to do nothing other than detect an [article](https://en.wikipedia.org/wiki/English_articles) - a word like "a", "an", or "the."

> Actually, those are all of the articles in English - there are other forms of articles, but English has those three and no others as examples of articles.

The way you interact with a parser is pretty simple. The grammar itself can extend <code>BaseParser&lt;T&gt;</code>, where `T` represents the output from the parser; you can use `Void` to indicate that the parser doesn't have any output internally.

Therefore, our `ArticleParser`'s declaration will be:

<pre>public class ArticleParser extends BaseParser&lt;Void&gt; {</pre>

We need to add a `Rule` to our parser, so that we can define an entry point from which the parser should begin. As a first stab, we'll create a `Rule` called `article()`, that tries to match one of our words:

<pre>public class ArticleParser extends BaseParser&lt;Void&gt; {
    public Rule article() {
        return trieIgnoreCase("a", "an", "the");
    }
}</pre>
This rule should match any variant of "a", "A", "an", "tHe", or anything like that - while not matching any text that doesn't somehow fit in as an article. Let's write a test that demonstrates this, using TestNG so we can use data providers:

<pre>package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
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
}</pre>

So what is happening here?

First, we create a global (for the test) `ArticleParser` instance through Grappa. Then, for every test,
 we create a `ListeningParseRunner`, with the entry point to the grammar as a parameter; this builds 
the internal model for the parser (stuff we don't really care about, but it *is* memoized, 
so we can use that code over and over again without incurring the time it takes for 
processing the grammar at runtime.)

> I used a utility method because the form of the tests themselves doesn't change - only the inputs and 
the rules being applied. As we add more to our grammar, this will allow us to run *similar* tests
with different inputs, results, and rules.

After we've constructed our Parser's `Runner`, we do something completely surprising: we run it, 
with <code>ParsingResult&lt;Void&gt; articleResult = runner.run(article);</code>. Adding 
in the TestNG data provider, this means we're calling our parser with every one of those articles 
as a test, and checking to see if the parser's validity - 
shown by `articleResult.isSuccess()` - matches what we expect. 

In most cases, it's pretty straightforward, since we are indeed passing in valid articles. Where we're not, 
the parser says that it's not a successful parse, as when we pass it `me`. 

There are three cases where the result might be surprising: `" a"`, `"a "`, and `"afoo"`. 
The whitespace is significant, for the parser; for our test, the article with a *trailing* space 
*passes validation*, as does "`afoo`", while the article with the *leading* space does not.

The leading space is easy: our parser doesn't consume any whitespace, and Grappa assumes whitespace 
is significant unless told otherwise. So that space doesn't match our article; 
it fails to parse, because of that.

However, the *trailing* space (and `"afoo"`) is a little more odd. What's happening there 
is that Grappa is parsing as much of the input as is necessary to fulfill the grammar; 
once it's done doing that, it doesn't care about anything that *follows* the grammar. So 
once it matches the initial text - the "`a`" - it doesn't care what the rest of the 
content is. It's not significant that `"foo"` follows the "`a`"; 
it matches the "`a`" and it's done.

We can fix that, of course, by specifying a better rule, one that includes 
a *terminal condition*. That introduces a core concept for Grappa, the "sequence."

Let's expand our `ArticleParser` a little more. Now it looks like:

<pre>package com.autumncode.bartender;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

public class ArticleParser extends BaseParser&lt;Void&gt; {
    public Rule article() {
        return trieIgnoreCase("a", "an", "the");
    }

    public Rule articleTerminal() {
        return sequence(
                article(),
                EOI
        );
    }
}</pre>

What we've done is added a new Rule - `articleTerminal()` - which contains a sequence. 
That sequence is "an article" -- which consumes "a," "an", or "the" - and then the special `EOI` rule, 
which stands for "end of input." That means that our simple article grammar won't consume leading or 
trailing spaces - the grammar will fail if any content exists besides our article. 
We can show that with a new test:

<pre>@DataProvider
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
 }</pre>

