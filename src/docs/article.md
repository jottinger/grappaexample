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
 
Now our test performs as we'd expect: it matches the article, and *only* the article - as soon as 
it has a single article, it expects the end of input. If it doesn't find that sequence, exactly, it fails
to match and `isSuccess()` returns `false`.

It's not really very kind for us to not accept whitespace, though: we probably want to parse `" a "` 
as a valid article, but not `" a the"` or anything like that.

It shouldn't be very surprising that we can use `sequence()` for that, too, along with a few new 
rules from Grappa itself. Here's our `Rule` for articles with surrounding whitespace:
  
    public Rule articleWithWhitespace() {
        return sequence(
                zeroOrMore(wsp()),
                article(),
                zeroOrMore(wsp()),
                EOI
        );
    }

What we've done is added two extra parsing rules, around our `article()` rule: `zeroOrMore(wsp())`.
The `wsp()` rule matches whitespace - spaces and tabs, for example. The `zeroOrMore()` rule seems
faintly self-explanatory, but just in case: it says "this rule will match if zero or more of the 
*contained* rules match."
 
 Therefore, our new rule will match however much whitespace we have before an article, then the article, 
 and then any whitespace *after* the article - but nothing else. That's fun to say, I guess, but it's
 a lot more fun to show:
 
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

Believe it or not, we're actually most of the way to being able to build our full drink order parser - 
we need to figure out how to get data from our parser (hint: it's related to that 
<code>&lt;Void&gt;</code> in the parser's declaration), but that's actually the greatest
burden we have remaining.

One other thing that's worth noting as we go: our code so far actually runs twenty-three tests. On my
development platform, it takes 64 milliseconds to run all twenty-three - the first one takes 49, where it's building 
the parser for the first time. The rest take somewhere between 0 and 4 milliseconds - and I'm pretty 
sure that 4ms reading is an outlier. Our grammar isn't complex, and I imagine we could write something 
*without* a grammar that would be faster - maybe <code>HashSet&lt;String&gt;.contains(input.trim())</code> -
but we're about to step into territory that would end up being a lot less maintainable as our grammar grows.

> I ran the tests one hundred times each and the same pattern showed up: 
  every now and then you'd see a test that ran slower. My initial guess is that this is related 
  to garbage collection or some other housekeeping chore on the JVM's part, but I haven't verified it.)

## Getting Data out of our Parser

Grappa uses an internal stack of values to track and expose information. 
We can tell it the type of the stack values - and in fact, we already did so in our `ArticleParser`. It's the
<code>&lt;Void&gt;</code> we used - that says that we have a stack of `Void` values, which is a cute way of
saying "no value at all." (If you remember carefully, we pointed that out when we first started 
describing the `ArticleParser` - this is where that information is useful!)

Therefore, all we need to do is expose a type, and then manipulate that stack of values. We do so with a
special type of `Rule`, a function that returns a boolean that indicates whether the `Rule` was successful.

Our goal with the article is to parse drink orders, of the general form of "a VESSEL of DRINK." We already worked 
on a parser that demonstrates parsing the "a" there - it's time to think about parsing the next term, which
we'll call a "vessel." Or, since we're using Java, a `Vessel` - which we'll encapsulate in an `Enum` so we can
easily add `Vessel`s.

The `Enum` itself is pretty simple:

    package com.autumncode.bartender;

    public enum Vessel {
        PINT,
        BOWL,
        GLASS,
        CUP,
        PITCHER,
        MAGNUM,
        BOTTLE,
        SPOON
    }
    
What we want to do is create a parser such that we can hand it "a glass" and get `Vessel.GLASS` out of it.

Given that we've said that a parser can be constructed with the "return type", that tells us that our
`VesselParser` wants to extend <code>BaseParser&lt;Vessel&gt;</code>, and so it does. In fact, our 
`VesselParser` isn't even very surprising, given what we've learned from our `ArticleParser`:

<pre>public class VesselParser extends BaseParser&lt;Vessel&gt; {
    final static Collection<String> vessels = Stream
            .of(Vessel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    public Rule vessel() {
        return trieIgnoreCase(vessels);
    }
}</pre>

What does this do? Well, most of it is building a `List` of the `Vessel` values, by extracting the values
from `Vessel`. It's marked `final static` so it will only initialize that `List` once; the 
`Rule` (`vessel()`) simply uses the exact same technique we used in parsing articles. It doesn't actually
 do anything with the match, though. It would simply fail if it was handed text that did not match
 a `Vessel` type.
 
 Let's try it out, using the same sort of generalized pattern we saw in our `ArticleParser` tests. (We're going to
 add a new generalized test method, when we add in the type that should be returned, but this will do
 for now.)
 
<pre>public class VesselTest {
    VesselParser parser = Grappa.createParser(VesselParser.class);

    private void testGrammar(String corpus, boolean status, Rule rule) {
        ListeningParseRunner&lt;Vessel&gt; runner
                = new ListeningParseRunner&lt;&gt;(rule);
        ParsingResult&lt;Vessel&gt; result = runner.run(corpus);
        assertEquals(result.isSuccess(), status,
                "failed check on " + corpus + ", parse result was "
                        + result + " and expected " + status);
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
}</pre>

 The idiom that Grappa uses - and that I will use, in any event - involves the use of the 
 `push()` and `match()` methods.
 
 Basically, when we match a `Vessel` - using that handy `vessel()` rule - what we will do is
 `push()` a value corresponding the the `Vessel` whose name corresponds to the `Rule` we just wrote. 
 We can get the text of the `Rule` we just matched, with the rather-handily-named `match()` method.
 
 It's actually simpler to program than it is to describe:
 
     // in VesselParser.java
     public Rule VESSEL() {
         return sequence(
                 vessel(), 
                 push(Vessel.valueOf(match().toUpperCase()))
         );
     }

This is a rule that encapsulates the matching of the vessel name - thus, `vessel()` - and then, assuming the
match is found, calls `push()` with the `Vessel` whose text is held in `match()`.
 
That's fine to say, but much better to show. Here's a test of our `VESSEL()` rule, following the same sort of
generalized pattern we saw for parsing articles, along with a new generalized test runner that examines the
returned value if the input data is valid according to the grammar:

    private void testGrammarResult(String corpus, boolean status, Vessel value, Rule rule) {
        ListeningParseRunner<Vessel> runner
                = new ListeningParseRunner<>(rule);
        ParsingResult<Vessel> result = runner.run(corpus);
        assertEquals(result.isSuccess(), status,
                "failed check on " + corpus + ", parse result was "
                        + result + " and expected " + status);
        if(result.isSuccess()) {
            assertEquals(result.getTopStackValue(), value);
        }
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

Note that we're testing with a `Rule` of `parser.VESSEL()` - the one that simply matches a vessel name is
named `parser.vessel()`, and the one that updates the parser's value stack is `parser.VESSEL()`. 

> This is a personal idiom. I reserve the right to change my mind if sanity demands it.

So what this does is very similar to our prior test - except it also tests the value on the parser's stack 
(accessed via `result.getTopStackValue()`) against the value that our `DataProvider` says should be returned,
as long as the parse was expected to be valid.

All this is well and good - we can hand it `"glass"` and get `Vessel.GLASS` -- but we haven't fulfilled 
everything we want out of a `VesselParser`. We want to be able to ask for `"   a pint "` -- 
note the whitespace! -- and get `Vessel.PINT`. We need to add in our article parsing.

First, let's write our tests, so we know when we're done:

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
                {"the stein", false, null},
        };
    }

    @Test(dataProvider = "articleVesselReturnData")
    public void testArticleVesselResult(String corpus, boolean valid, Vessel value) {
        testGrammarResult(corpus, valid, value, parser.ARTICLEVESSEL());
    }

Our tests should be able to ignore the leading article and any whitespace. Any wrongful formation 
(as you see in `" a an magnum"`) should fail, and any vessel type that isn't valid (`"hatful"` and
`"the stein"`) should fail.

Our `Rule` is going to *look like* a monster, because it has to handle a set of possibilities, but
 it's actually pretty simple. Let's take a look, then walk through the grammar:
 
    public Rule article() {
        return trieIgnoreCase("a", "an", "the");
    }

    public Rule ARTICLEVESSEL() {
        return sequence(
                zeroOrMore(wsp()),
                optional(
                        sequence(
                                article(),
                                oneOrMore(wsp())
                        )),
                VESSEL(),
                zeroOrMore(wsp()),
                EOI);
    }

First, we added our `article()` `Rule`, from our `ArticleParser`. It might be tempting to copy all the whitespace
 handling from that parser as well, but we shouldn't - all we care about is the articles themselves 
 ("lexemes," if we're trying to look all nerdy.) 
 
 It's the `ARTICLEVESSEL()` `Rule` that's fascinating. What that is describing is a sequence, consisting of:
 
 * Perhaps some whitespace, expressed as `zeroOrMore(wsp())`.
 * An optional sequence, consisting of:
     * An article.
     * At least one whitespace character.
 * A vessel (which, since we're using `VESSEL()`, means the parser's stack is updated.)
 * Perhaps some whitespace.
 * The end of input.
 
Any input that doesn't follow that exact sequence (`"spoon bottle"`, for example) fails.
 
 Believe it or not, we're now very much on the downhill slide for our bar-tending AI.
 
 We need to add a preposition ("of") and then generalized text handling for the type of drink, and we need to
 add the container type - but of this, only the type of drink will add any actual complexity to our parser.
 
## Rounding out the Bartender
 
 Our `VesselParser` is actually a pretty good model for the `DrinkOrderParser` that our `Bartender`
 will use. What we need to add is matching for two extra tokens: "of," as mentioned, and then a 
 generalized description of a drink.
 
 We're not going to be picky about the description; we could validate it (just like we've done for 
 `Vessel`) but there are actual better lessons to be gained by leaving it free-form.
 
 Let's take a look at the operative part of `Bartender` again, which will set the stage for the full parser.
 
<pre>DrinkOrderParser parser
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
return done;</pre>

 The very first thing we're going to do is create a `DrinkOrder` class, that contains the information about
 our drink order. 
 
 <pre>public class DrinkOrder {
    Vessel vessel;
    String description;
    boolean terminal;
}</pre>
      
I'm actually using Lombok in the project (and the `@Data` annotation) but for the sake of example, 
imagine that we have the standard boilerplate accesors and mutators for each of those attributes.
Thus, we can call `setDescription()`, et al, even though we're not showing that code. We're also going to have
`equals()` and `hashCode()` created (via Lombok), as well as a no-argument constructor and another constructor
for all properties. 

In other words, it's a fairly standard Javabean, but we're not showing all the the boilerplate code - and thanks
to Lombok, we don't even *need* the boilerplate code. Lombok makes it for us.
 
> If you do need the code for `equals()`, `hashCode()`, `toString()`, or the mutators, accessors, and 
 constructors shown, you may be reading the wrong tutorial. How did you make it this far?
 
Before we dig into the parser - which has only one really interesting addition to the things we've seen so far - 
let's take a look at our test. This is the *full* test, so it's longer than some of our code has been. 
The `DrinkOrderParser` will be much longer.

<pre>public class DrinkOrderParserTest {
    DrinkOrderParser parser = Grappa.createParser(DrinkOrderParser.class);
    
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
                {"a pitcher of old 66", true, new DrinkOrder(Vessel.PITCHER, "old 66", false)},
                {"a    pint  of duck   vomit   ", true, new DrinkOrder(Vessel.PINT, "duck vomit", false)},
                {"a shoeful of motor oil", false, null},
                {"nothing", true, new DrinkOrder(null, null, true)},
        };
    }
    
    @Test(dataProvider = "drinkOrderProvider")
    public void testDrinkOrderParser(String corpus, boolean valid, DrinkOrder result) {
        testGrammarResult(corpus, valid, result, parser.DRINKORDER());
    }
}</pre>

Most of this should be fairly simple; it's the same pattern we've seen used in our other tests.

> I don't actually drink, myself, so... I keep imagining some biker bar in the American southwest 
selling a beer called "Old 66," and
in my imagination "duck vomit" is the kind of wine that comes in a resealable plastic bag.

A lot of the `DrinkOrderParser` will be very familiar. Let's dive in and take a look at *all* 
of it and then break it down:

<pre>public class DrinkOrderParser extends BaseParser&lt;DrinkOrder&gt; {
    Collection&lt;String&gt; vessels = Stream
            .of(Vessel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    public boolean assignDrink() {
        peek().setDescription(match().toLowerCase().replaceAll("\\s+", " "));
        return true;
    }

    public boolean assignVessel() {
        peek().setVessel(Vessel.valueOf(match().toUpperCase()));
        return true;
    }

    public boolean setTerminal() {
        peek().setTerminal(true);
        return true;
    }

    public Rule ARTICLE() {
        return trieIgnoreCase("a", "an", "the");
    }

    public Rule OF() {
        return
                ignoreCase("of");
    }

    public Rule NOTHING() {
        return sequence(
                trieIgnoreCase("nothing", "nada", "zilch", "done"),
                setTerminal(),
                EOI
        );
    }

    public Rule VESSEL() {
        return sequence(
                trieIgnoreCase(vessels),
                assignVessel()
        );
    }

    public Rule DRINK() {
        return sequence(
                join(oneOrMore(firstOf(alpha(), digit())))
                        .using(oneOrMore(wsp()))
                        .min(1),
                assignDrink()
        );
    }

    public Rule DRINKORDER() {
        return sequence(
                push(new DrinkOrder()),
                zeroOrMore(wsp()),
                firstOf(
                        NOTHING(),
                        sequence(
                                optional(sequence(
                                        ARTICLE(),
                                        oneOrMore(wsp())
                                )),
                                VESSEL(),
                                oneOrMore(wsp()),
                                OF(),
                                oneOrMore(wsp()),
                                DRINK()
                        )
                ),
                zeroOrMore(wsp()),
                EOI
        );
    }
}</pre>

We're reusing the mechanism for creating a collection of `Vessel` references. We're also repeating the
`Rule` used to detect an article.

We're adding a `Rule` for the detection of the preposition "of", which is a mandatory element in our grammar:

    public Rule OF() {
        return
                ignoreCase("of");
    }

> Note how I'm skirting my own rule about naming. I said I was reserving the right to change my mind, 
and apparently I've done so even while writing this article. According to the naming convention I
described earlier, it should be `of()` and not `OF()` because it doesn't alter the parser's stack. The same
rule applies to `ARTICLE()`. It's my content, I'll write it how I want to unless I decide to fix it later.

I'm also creating methods to mutate the parser state:

    protected boolean assignDrink() {
        peek().setDescription(match().toLowerCase().replaceAll("\\s+", " "));
        return true;
    }

    protected boolean assignVessel() {
        peek().setVessel(Vessel.valueOf(match().toUpperCase()));
        return true;
    }

    protected boolean setTerminal() {
        peek().setTerminal(true);
        return true;
    }

These are a little interesting, in that they use `peek()`. The actual base rule in our grammar 
is `DRINKORDER()`, which *immediately* pushes a `DrinkOrder` reference onto the parser stack. That means
that there is a `DrinkOrder` that other rules can modify at will; `peek()` gives us that reference. Since it's
typed, through Java's generics, we can call any method that `DrinkOrder` exposes.

These utilities all return `true`. None of them can fail, because they won't be called unless a prior rule 
has matched; these methods are for convenience only.  Actually, let's show the `NOTHING()` and 
`VESSEL()` rules, so we can see how these methods are invoked:

    public Rule NOTHING() {
        return sequence(
                trieIgnoreCase("nothing", "nada", "zilch", "done"),
                setTerminal(),
                EOI
        );
    }

    public Rule VESSEL() {
        return sequence(
                trieIgnoreCase(vessels),
                assignVessel()
        );
    }

