[Grappa](https://github.com/fge/grappa) is a parser library for Java. It's a fork of [Parboiled](https://github.com/sirthias/parboiled/wiki), which focuses more on [Scala](http://scala-lang.org) as a development environment; Grappa tries to feel more Java-like than Parboiled does.

Grappa's similar in focus to other libraries like [ANTLR](http://www.antlr.org/) and [JavaCC](https://javacc.java.net/); the main advantage to using something like Grappa instead of ANTLR is in the lack of a processing phase. With ANTLR and JavaCC, you have a grammar file, which then generates a lexer and a parser in Java source code. Then you compile that generated source to get your parser.

Grappa (and Parboiled) represent the grammar in actual source code, so there is no external phase; this makes programming with them *feel* faster. It's certainly easier to integrate with tooling, since there is no separate tool to invoke apart from the compiler itself.

I'd like to walk through a simple experience of *using* Grappa, to perhaps help expose how Grappa works.

## The Goal

What I want to do is mirror a tutorial I found for ANTLR, "[ANTLR 4: using the lexer, parser and listener with example grammar](http://www.theendian.com/blog/antlr-4-lexer-parser-and-listener-with-example-grammar/)." It's an okay tutorial, but the main thing *I* thought after reading was: "Hmm, ANTLR's great, everyone uses it, but let's see if there are alternatives."

> That led me to Parboiled, but some Parboiled users recommended Grappa for Java, so here we are.

That tutorial basically writes a parser for drink orders. We'll do more.

## Our Bartender
 
Imagine an automated bartender: "What're ya havin?"

Well... let's automate that bartender, such that he can parse responses like "`A pint of beer`." We can imagine more variations on this, but we're going to center on one, until we get near the end of the tutorial: we'd also like to allow our bartender to parse orders from people who're a bit too inebriated to use the introductory article: "`glass of wine`" (no `a`) should also be acceptable.

> If you're interested, the code is on [GitHub](https://github.com), in my [grappaexample](https://github.com/jottinger/grappaexample) repository.

Let's take a look at our [bartender](https://github.com/jottinger/grappaexample/blob/master/src/main/java/com/autumncode/bartender/Bartender.java)'s source code, just to set the stage for our grammar. (Actually, we'll be writing multiple grammars, because we want to take it in small pieces.)

<pre lang="java">package com.autumncode.bartender;

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
        System.out.print("What're ya havin'? ");
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

Grappa describes a grammar as a set of [`Rule`](http://fge.github.io/com/github/fge/grappa/rules/Rule.html)s. A rule can describe a match or an action; both matches and actions return boolean values to indicate success. A rule has failed when processing sees `false` in its stream.

Let's generate a very small parser for the sake of example. Our first parser (`ArticleParser`) is going to do nothing other than detect an [article](https://en.wikipedia.org/wiki/English_articles) - a word like "a", "an", or "the."

> Actually, those are all of the articles in English - there are other forms of articles, but English has those three and no others as examples of articles.

The way you interact with a parser is pretty simple. The grammar itself can extend <code><a href="http://fge.github.io/com/github/fge/grappa/parsers/BaseParser.html">BaseParser</a>&lt;T&gt;</code>, where `T` represents the output from the parser; you can use [`Void`](https://docs.oracle.com/javase/8/docs/api/java/lang/Void.html) to indicate that the parser doesn't have any output internally.

Therefore, our `ArticleParser`'s declaration will be:

<pre>public class ArticleParser extends BaseParser&lt;Void&gt; {</pre>

We need to add a `Rule` to our parser, so that we can define an entry point from which the parser should begin. As a first stab, we'll create a `Rule` called `article()`, that tries to match one of our words. With a trie. It's cute that way. 

> A [trie](https://en.wikipedia.org/wiki/Trie) is a type of radix tree. They tend to be super-fast at certain kinds of classifications. Note that this method name may change in later versions of Grappa, because honestly, the actual search mechanism - the trie - isn't important for the purpose of invoking the method.

<pre>public class ArticleParser extends BaseParser&lt;Void&gt; {
    public Rule article() {
        return trieIgnoreCase("a", "an", "the");
    }
}</pre>

This rule should match any variant of "a", "A", "an", "tHe", or anything like that - while not matching any text that doesn't somehow fit in as an article. Let's write a test that demonstrates this, using [TestNG](http://testng.org) so we can use [data providers](http://testng.org/doc/documentation-main.html#parameters-dataproviders):

<pre>package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ArticleTest {
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
        ArticleParser parser = Grappa.createParser(ArticleParser.class);
        testArticleGrammar(article, status, parser.article());
    }
    
    private void testArticleGrammar(String article, boolean status, Rule rule) {
        ListeningParseRunner&lt;Void&gt; runner
                = new ListeningParseRunner&lt;&gt;(rule);
        ParsingResult&lt;Void&gt; articleResult = runner.run(article);
        assertEquals(articleResult.isSuccess(), status,
                "failed check on " + article + ", parse result was "
                        + articleResult + " and expected " + status);
    }
}</pre>

So what is happening here?

First, we create a global (for the test) `ArticleParser` instance through [`Grappa`](http://fge.github.io/com/github/fge/grappa/Grappa.html). Then we create a [`ListeningParseRunner`](http://fge.github.io/com/github/fge/grappa/run/ListeningParseRunner.html),  with the entry point to the grammar as a parameter; this builds the internal model for the parser (stuff we don't really care about, but it *is* [memoized](https://en.wikipedia.org/wiki/Memoization), so we can use that code over and over again without incurring the time it takes for processing the grammar at runtime.)

> I used a utility method because the form of the tests themselves doesn't change - only the inputs and the rules being applied. As we add more to our grammar, this will allow us to run *similar* tests with different inputs, results, and rules.

After we've constructed our Parser's `Runner`, we do something completely surprising: we run it, with <code><a href="http://fge.github.io/com/github/fge/grappa/run/ParsingResult.html">ParsingResult</a>&lt;Void&gt; articleResult = runner.run(article);</code>. Adding in the TestNG data provider, this means we're calling our parser with every one of those articles as a test, and checking to see if the parser's validity - shown by <code>articleResult.<a href="http://fge.github.io/com/github/fge/grappa/run/ParsingResult.html#isSuccess()">isSuccess()</a></code> - matches what we expect. 

> Incidentally, it occurred to me that I could ha

In most cases, it's pretty straightforward, since we are indeed passing in valid articles. Where we're not, the parser says that it's not a successful parse, such as when we pass it `me`. 

There are three cases where the result might be surprising: `" a"`, `"a "`, and `"afoo"`. The whitespace is significant, for the parser; for our test, the article with a *trailing* space *passes validation*, as does "`afoo`", while the article with the *leading* space does not.

The leading space is easy: our parser doesn't consume any whitespace, and Grappa assumes whitespace is significant unless told otherwise (by Rules, of course.) So that space doesn't match our article;  it fails to parse, because of that.

However, the *trailing* space (and `"afoo"`) is a little more odd. What's happening there is that Grappa is parsing as much of the input as is necessary to fulfill the grammar; once it's finished doing that, it doesn't care about anything that *follows* the grammar. So once it matches the initial text - the "`a`" - it doesn't care what the rest of the content is. It's not significant that `"foo"` follows the "`a`"; it matches the "`a`" and it's done.

We can fix that, of course, by specifying a better rule - one that includes a *terminal condition*. That introduces a core concept for Grappa, the "[`sequence()`](http://fge.github.io/com/github/fge/grappa/parsers/BaseParser.html#sequence(java.lang.Object[]))." (This will factor very heavily into our grammar when we add the ability to say "please" at the end of the tutorial.)

> Author's note: I use "terminal" to mean "ending." So a terminal *anything* is meant to indicate finality. However, a "terminal" is also used to describe something that doesn't delegate to anything else, in Grappa's terms. So for Grappa, the use of "terminal" might not be the same as *my* use of the word "terminal".

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

What we've done is added a new Rule - `articleTerminal()` - which contains a `sequence`. That `sequence` is "an article" -- which consumes "a," "an", or "the" - and then the special [`EOI`](http://fge.github.io/com/github/fge/grappa/parsers/BaseParser.html#EOI) rule, which stands for "end of input." That means that our simple article grammar won't consume leading or trailing spaces - the grammar will fail if any content exists besides our article. 

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
    ArticleParser parser = Grappa.createParser(ArticleParser.class);
    testArticleGrammar(article, status, parser.articleTerminal());
}</pre>
 
Now our test performs as we'd expect: it matches the article, and *only* the article - as soon as it has a single article, it expects the end of input. If it doesn't find that `sequence` exactly, it fails to match and `isSuccess()` returns `false`.

It's not really very kind for us to not accept whitespace, though: we probably want to parse `" a "` as a valid article, but not `" a the"` or anything like that.

It shouldn't be very surprising that we can use `sequence()` for that, too, along with a few new rules from Grappa itself. Here's our `Rule` for articles with surrounding whitespace:
  
    public Rule articleWithWhitespace() {
        return sequence(
                zeroOrMore(wsp()),
                article(),
                zeroOrMore(wsp()),
                EOI
        );
    }

What we've done is added two extra parsing rules, around our  `article()` rule: `zeroOrMore(wsp())`. The [`wsp()`](http://fge.github.io/com/github/fge/grappa/parsers/BaseParser.html#wsp()) rule matches whitespace - spaces and tabs, for example. The [`zeroOrMore()`](http://fge.github.io/com/github/fge/grappa/parsers/BaseParser.html#zeroOrMore(java.lang.Object)) rule seems faintly self-explanatory, but just in case: it says "this rule will match if zero or more of the *contained* rules match."
 
Therefore, our new rule will match however much whitespace we have before an article, then the article,  and then any whitespace *after* the article - but nothing else. That's fun to say, I guess, but it's a lot more fun to show:
 
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
         ArticleParser parser = Grappa.createParser(ArticleParser.class);
         testArticleGrammar(article, status, parser.articleWithWhitespace());
     }

Believe it or not, we're actually most of the way to being able to build our full drink order parser - we need to figure out how to get data from our parser (hint: it's related to that <code>&lt;Void&gt;</code> in the parser's declaration), but that's actually the greatest burden we have remaining.

One other thing that's worth noting as we go: our code so far actually runs twenty-three tests. On my development platform, it takes 64 milliseconds to run all twenty-three - the first one takes 49, where it's building the parser for the first time. The rest take somewhere between 0 and 4 milliseconds - and I'm pretty sure that 4ms reading is an outlier. Our grammar isn't complex, and I imagine we could write something *without* a grammar that would be faster - maybe <code>HashSet&lt;String&gt;.contains(input.trim())</code> - but we're about to step into territory that would end up being a lot less maintainable as our grammar grows.

> I ran the tests one hundred times each and the same pattern showed up: every now and then you'd see a test that ran slower. My initial guess is that this is related to garbage collection or some other housekeeping chore on the JVM's part, but I haven't verified it.)

## Getting Data out of our Parser

Grappa uses an internal stack of values to track and expose information. We can tell it the type of the stack values - and in fact, we already did so in our `ArticleParser`. It's the <code>&lt;Void&gt;</code> we used - that says that we have a stack of `Void` values, which is a cute way of saying "no value at all." (If you remember carefully, we pointed that out when we first started describing the `ArticleParser` - this is where that information is useful!)

Therefore, all we need to do is expose a type, and then manipulate that stack of values. We do so with a special type of `Rule`, a function that returns a boolean that indicates whether the `Rule` was successful.

Our goal with the article is to parse drink orders, of the general form of "a VESSEL of DRINK." We already worked on a parser that demonstrates parsing the "a" there - it's time to think about parsing the next term, which we'll call a "vessel." Or, since we're using Java, a `Vessel` - which we'll encapsulate in an `Enum` so we can easily add `Vessel`s.

The `Vessel` itself is pretty simple:

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

Given that we've said that a parser can be constructed with the "return type", that tells us that our `VesselParser` wants to extend <code>BaseParser&lt;Vessel&gt;</code>, and so it does. In fact, our `VesselParser` isn't even very surprising, given what we've learned from our `ArticleParser`:

<pre>public class VesselParser extends BaseParser&lt;Vessel&gt; {
    static final Collection&lt;String&gt; vessels = Stream
            .of(Vessel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    public Rule vessel() {
        return trieIgnoreCase(vessels);
    }
}</pre>

What does this do? Well, most of it is building a `List` of the `Vessel` values, by extracting the values from `Vessel`. It's marked `static final` so it will only initialize that `List` once; the `Rule` (`vessel()`) simply uses the exact same technique we used in parsing articles. It doesn't actually  do anything with the match, though. It would simply fail if it was handed text that did not match a `Vessel` type.

> Incidentally, the [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se8/html/index.html) suggests the order of `static final`, in section 8.3.1, [Field Modifiers](https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.3.1).
 
Let's try it out, using the same sort of generalized pattern we saw in our `ArticleParser` tests. (We're going to add a new generalized test method, when we add in the type that should be returned, but this will do for now.)
 
<pre>public class VesselTest {
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
        VesselParser parser = Grappa.createParser(VesselParser.class);
        testGrammar(corpus, valid, parser.vessel());
    }
}</pre>

The idiom that Grappa uses - and that I will use, in any event - involves the use of the [`push()`](http://fge.github.io/com/github/fge/grappa/parsers/BaseActions.html#push(V)) and [`match()`](http://fge.github.io/com/github/fge/grappa/parsers/BaseActions.html#match()) methods.
 
Basically, when we match a `Vessel` - using that handy `vessel()` rule - what we will do is  `push()` a value corresponding to the `Vessel` whose name corresponds to the `Rule` we just wrote. We can get the text of the `Rule` we just matched, with the rather-handily-named `match()` method.
 
It's actually simpler to program than it is to describe:
 
     // in VesselParser.java
     public Rule VESSEL() {
         return sequence(
                 vessel(), 
                 push(Vessel.valueOf(match().toUpperCase()))
         );
     }

This is a rule that encapsulates the matching of the vessel name - thus, `vessel()` - and then, assuming the match is found, calls `push()` with the `Vessel` whose text is held in `match()`.
 
That's fine to say, but much better to show. Here's a test of our `VESSEL()` rule, following the same sort of generalized pattern we saw for parsing articles, along with a new generalized test runner that examines the returned value if the input data is valid according to the grammar:

<pre>private void testGrammarResult(String corpus, boolean status, Vessel value, Rule rule) {
    ListeningParseRunner&lt;Vessel&gt; runner
            = new ListeningParseRunner&lt;&gt;(rule);
    ParsingResult&lt;Vessel&gt; result = runner.run(corpus);
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
    VesselParser parser = Grappa.createParser(VesselParser.class);
    testGrammarResult(corpus, valid, value, parser.VESSEL());
}</pre>

Note that we're testing with a `Rule` of `parser.VESSEL()` - the one that simply matches a vessel name is named `parser.vessel()`, and the one that updates the parser's value stack is `parser.VESSEL()`. 

> This is a personal idiom. I reserve the right to change my mind if sanity demands it. In fact, I predict that I will have done just this by the end of this article.

So what this does is very similar to our prior test - except it also tests the value on the parser's stack (accessed via 
<code>result.<a href="http://fge.github.io/com/github/fge/grappa/run/ParsingResult.html#getTopStackValue()">getTopStackValue()</a></code>) against the value that our `DataProvider` says should be returned, as long as the parse was expected to be valid.

All this is well and good - we can hand it `"glass"` and get `Vessel.GLASS` -- but we haven't fulfilled everything we want out of a `VesselParser`. We want to be able to ask for `"   a pint "` -- note the whitespace! -- and get `Vessel.PINT`. We need to add in our article parsing.

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
        VesselParser parser = Grappa.createParser(VesselParser.class);
        testGrammarResult(corpus, valid, value, parser.ARTICLEVESSEL());
    }

Our tests should be able to ignore the leading article and any whitespace. Any wrongful formation (as you see in `" a an magnum"`) should fail, and any vessel type that isn't valid (`"hatful"` and `"the stein"`) should fail.

Our `Rule` is going to *look like* a monster, because it has to handle a set of possibilities, but it's actually pretty simple. Let's take a look, then walk through the grammar:
 
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

First, we added our `article()` `Rule`, from our `ArticleParser`. It might be tempting to copy all the whitespace  handling from that parser as well, but we shouldn't - all we care about is the articles themselves ("lexemes," if we're trying to look all nerdy.) 
 
It's the `ARTICLEVESSEL()` `Rule` that's fascinating. What that is describing is a sequence, consisting of:
 
 * Perhaps some whitespace, expressed as `zeroOrMore(wsp())`.
 * An [optional](http://fge.github.io/com/github/fge/grappa/parsers/BaseParser.html#optional(java.lang.Object)) sequence, consisting of:
     * An article.
     * At least one whitespace character.
 * A vessel (which, since we're using `VESSEL()`, means the parser's stack is updated.)
 * Perhaps some whitespace.
 * The end of input.
 
Any input that doesn't follow that exact sequence (`"spoon bottle"`, for example) fails.
 
Believe it or not, we're now very much on the downhill slide for our bar-tending program.
 
We need to add a preposition ("of") and then generalized text handling for the type of drink, and we need to add the container type - but of this, only the type of drink will add any actual complexity to our parser.
 
## Rounding out the Bartender
 
Our `VesselParser` is actually a pretty good model for the `DrinkOrderParser` that our `Bartender` will use. What we need to add is matching for two extra tokens: "of," as mentioned, and then a  generalized description of a drink.
 
We're not going to be picky about the description; we could validate it (just like we've done for  `Vessel`) but there are actual better lessons to be gained by leaving it free-form.
 
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

The very first thing we're going to do is create a `DrinkOrder` class, that contains the information about our drink order. 
 
<pre>public class DrinkOrder {
    Vessel vessel;
    String description;
    boolean terminal;
}</pre>
      
I'm actually using [Lombok](https://projectlombok.org/) in the project (and the `@Data` annotation) but for the sake of example, imagine that we have the standard boilerplate accessors and mutators for each of those attributes. Thus, we can call `setDescription()`, et al, even though we're not showing that code. We're also going to have `equals()` and `hashCode()` created (via Lombok), as well as a no-argument constructor and another constructor for all properties. 

In other words, it's a fairly standard Javabean, but we're not showing all of the boilerplate code - and thanks to Lombok, we don't even *need* the boilerplate code. Lombok makes it for us.
 
> If you do need the code for `equals()`, `hashCode()`, `toString()`, or the mutators, accessors, and constructors shown, you may be reading the wrong tutorial. How did you make it this far?
 
Before we dig into the parser - which has only one really interesting addition to the things we've seen so far - let's take a look at our test. This is the *full* test, so it's longer than some of our code has been. The `DrinkOrderParser` will be much longer.

<pre>public class DrinkOrderParserTest {
    private void testGrammarResult(String corpus, boolean status, DrinkOrder value, Rule rule) {
        ListeningParseRunner&lt;DrinkOrder&gt; runner
                = new ListeningParseRunner&lt;&gt;(rule);
        ParsingResult&lt;DrinkOrder&gt; result = runner.run(corpus);
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
        DrinkOrderParser parser = Grappa.createParser(DrinkOrderParser.class);
        testGrammarResult(corpus, valid, result, parser.DRINKORDER());
    }
}</pre>

Most of this should be fairly simple; it's the same pattern we've seen used in our other tests.

> I don't actually drink, myself, so... I keep imagining some biker bar in the American southwest selling a beer called "Old 66," and in my imagination "duck vomit" is the kind of wine that comes in a resealable plastic bag.

A lot of the `DrinkOrderParser` will be very familiar. Let's dive in and take a look at *all* of it and then break it down:

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
        return ignoreCase("of");
    }

    public Rule NOTHING() {
        return sequence(
                trieIgnoreCase("nothing", "nada", "zilch", "done"),
                EOI,
                setTerminal()
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
                                optional
                                        ARTICLE(),
                                        oneOrMore(wsp())
                                ),
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

We're reusing the mechanism for creating a collection of `Vessel` references. We're also repeating the `Rule` used to detect an article.

We're adding a `Rule` for the detection of the preposition "of", which is a mandatory element in our grammar. We use [`ignoreCase()`](http://fge.github.io/com/github/fge/grappa/parsers/BaseParser.html#ignoreCase(java.lang.String)), because we respect the rights of drunkards to shout at their barkeeps:

    public Rule OF() {
        return ignoreCase("of");
    }

> Note how I'm skirting my own rule about naming. I said I was reserving the right to change my mind, and apparently I've done so even while writing this article. According to the naming convention I described earlier, it should be `of()` and not `OF()` because it doesn't alter the parser's stack. The same rule applies to `ARTICLE()`. It's my content, I'll write it how I want to unless I decide to fix it later.

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

These are a little interesting, in that they use [`peek()`](http://fge.github.io/com/github/fge/grappa/parsers/BaseActions.html#peek()). The actual base rule in our grammar is `DRINKORDER()`, which *immediately* pushes a `DrinkOrder` reference onto the parser stack. That means that there is a `DrinkOrder` that other rules can modify at will; `peek()` gives us that reference. Since it's typed via Java's generics, we can call any method that `DrinkOrder` exposes.

These utilities all return `true`. None of them can fail, because they won't be called unless a prior rule has matched; these methods are for convenience only.  Actually, let's show the `NOTHING()` and `VESSEL()` rules, so we can see how these methods are invoked:

    public Rule NOTHING() {
        return sequence(
                trieIgnoreCase("nothing", "nada", "zilch", "done"),
                EOI,
                setTerminal(),
        );
    }

    public Rule VESSEL() {
        return sequence(
                trieIgnoreCase(vessels),
                assignVessel()
        );
    }

This leaves two new rules to explain: `DRINK()` and `DRINKORDER()`. Here's `DRINK()`:

    public Rule DRINK() {
        return sequence(
                join(oneOrMore(firstOf(alpha(), digit())))
                        .using(oneOrMore(wsp()))
                        .min(1),
                assignDrink()
        );
    }

This rule basically builds a list of words. It's a sequence of operations; the first builds the match of the words, and the second operation assigns the matched content to the `DrinkOrder`'s description.

The match of the words is really just a sequence of alphanumeric characters. It requires at least *one* such sequence to exist, but will consume as many as there are in the input.

Now for the `Rule` that does most of the work: `DRINKORDER()`.

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

Again, we have a sequence. It works something like this:

* First, push a new `DrinkOrder` onto the stack, to keep track of our order's state.
* Consume any leading whitespace.
* Either:
    * Check for the terminal condition ("nothing", for example), or
    * Check for a new sequence, of the following form:
        * An optional sequence:
            * An article
            * Any trailing whitespace after the article
        * A vessel
        * One or more whitespace characters
        * The lexeme matching "of"
        * One or more whitespace characters
        * The drink description
* Any trailing whitespace
* The end of input

We've basically built most of this through our parsers, bit by bit; armed with the ability to `peek()` and `push()`, we can build some incredibly flexible parsers with fairly simple code.

## Adding Politesse

All this has been great, so far. We can actually "order" from a `Bartender`, giving us this scintillating conversation:

    $ java -jar bartender-1.0-SNAPSHOT.jar
    What're ya havin'? a glass of water
    Here's your glass of water. Please drink responsibly!
    What're ya havin'? a toeful of shoe polish
    I'm sorry, I don't understand. Try again?
    What're ya havin'? a pint of indigo ink
    Here's your pint of indigo ink. Please drink responsibly!
    What're ya havin'? nothing
    $

The only problem is that it's not very humane or polite. We can't say "please," we can't be very flexible. What we need is to add [politesse](http://www.merriam-webster.com/dictionary/politesse) to our grammar.

What we really want to do is modify our `DrinkOrderParser` so that we can ask for "a cup of pinot noir, 1986 vintage, please?" It should be able to tell us that we've ordered "pinot noir, 1986" and not "pinot noir, 1986, please?" -- that'd be silly.

However, we need to alter our grammar in some core ways - particularly in how we match the drink names -- and use a new Rule, `testNot`. First, though, let's take a look at our test code, because that's going to give us a workable indicator of whether we've succeeded or not.

<pre>public class PoliteDrinkOrderParserTest {
    private void testGrammarResult(String corpus, boolean status, DrinkOrder value, Rule rule) {
        ListeningParseRunner&lt;DrinkOrder&gt; runner
                = new ListeningParseRunner&lt;&gt;(rule);
        ParsingResult&lt;DrinkOrder&gt; result = runner.run(corpus);
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
                {"glass of pinot noir, 1986, ok?", true, new DrinkOrder(Vessel.GLASS, "pinot noir, 1986", false)},
                {"cup , pinot noir, 1986 vintage, ok?", true, new DrinkOrder(Vessel.CUP, "pinot noir, 1986 vintage", false)},
                {"cup,pinot noir, 1986,ok!", true, new DrinkOrder(Vessel.CUP, "pinot noir, 1986", false)},
                {"a    pint  of duck   vomit   ", true, new DrinkOrder(Vessel.PINT, "duck vomit", false)},
                {"a    pint  of duck   vomit  , please ", true, new DrinkOrder(Vessel.PINT, "duck vomit", false)},
                {" pint , duck   vomit please  ", true, new DrinkOrder(Vessel.PINT, "duck vomit", false)},
                {"a shoeful of motor oil", false, null},
                {"nothing", true, new DrinkOrder(null, null, true)},
        };
    }

    @Test(dataProvider = "drinkOrderProvider")
    public void testDrinkOrderParser(String corpus, boolean valid, DrinkOrder result) {
        PoliteDrinkOrderParser parser = Grappa.createParser(PoliteDrinkOrderParser.class);
        testGrammarResult(corpus, valid, result, parser.ORDER());
    }
}</pre>

You'll notice that we've changed some other things, too. Our original grammar was pretty simple in formal-ish terms:

<pre>DRINKORDER ::= nothing | article? vessel `of` drink
article ::= a | an | the
vessel ::= pint | bowl | glass | cup | pitcher | magnum | bottle | spoon
drink ::= .*
nothing ::= nothing | nada | zilch | done</pre>

Note that this isn't an actual formal grammar - I'm cheating. It just looks as if it might be something near formal, with a particular failure in the "drink" term.

Our new one seems to be more flexible:

<pre>DRINKORDER ::= (nothing | article? vessel of drink) interjection? eos?
article ::= a | an | the
vessel ::= pint | bowl | glass | cup | pitcher | magnum | bottle | spoon
of ::= , | of
drink ::= !interjection
interjection ::= ','? please | ok | okay | pls | yo 
eos ::= '.' | '!' | '?'
nothing ::= nothing | nada | zilch | done</pre>

Here, we're no more actually formal than we were - the "<code>!interjection</code>" is trying to say that a drink is everything where a drink would be appropriate, *up to* the interjection.

> I don't care for Backus-Naur form, and I'm using something that looks like it because I thought it might help. Your mileage may vary as to whether I was correct or not.

At any rate, our new grammar should allow us to say "please" and eliminate the unnecessary "of" - although I'm not willing to concede that a bartender should respond well to "<code>pint beer.</code>" "<code>pint, beer.</code>" I can accept - but that comma is significant, by golly.

> I'll leave it as an exercise for the reader to make the comma not necessary - and to write the test that proves it.

However, one thing remains: we haven't seen our grammar. Most of it's the same: the article, the vessel, and the action rules (the things that construct our returned drink order) haven't changed, but we have a slew of new rules (for the end of the sentence and the interjection) and we've modified some old ones (drink, and of). Let's take a look at the changes held in [`PoliteDrinkOrderParser`](https://github.com/jottinger/grappaexample/blob/master/src/main/java/com/autumncode/bartender/PoliteDrinkOrderParser.java):

    public Rule OF() {
        return firstOf(
                sequence(
                        zeroOrMore(wsp()),
                        COMMA(),
                        zeroOrMore(wsp())
                ),
                sequence(
                        oneOrMore(wsp()),
                        ignoreCase("of"),
                        oneOrMore(wsp())
                )
        );
    }

    public Rule DRINK() {
        return sequence(
                oneOrMore(
                        testNot(INTERJECTION()),
                        ANY
                ),
                assignDrink());
    }

    public Rule DRINKORDER() {
        return sequence(
                optional(sequence(
                        ARTICLE(),
                        oneOrMore(wsp())
                )),
                VESSEL(),
                OF(),
                DRINK()
        );
    }

    public Rule COMMA() {
        return ch(',');
    }

    public Rule INTERJECTION() {
        return sequence(
                zeroOrMore(wsp()),
                optional(COMMA()),
                zeroOrMore(wsp()),
                trieIgnoreCase("please", "pls", "okay", "yo", "ok"),
                TERMINAL()
        );
    }

    public Rule EOS() {
        return anyOf(".!?");
    }

    public Rule TERMINAL() {
        return sequence(zeroOrMore(wsp()),
                optional(EOS()),
                zeroOrMore(wsp()),
                EOI
        );
    }

    public Rule ORDER() {
        return sequence(
                push(new DrinkOrder()),
                zeroOrMore(wsp()),
                firstOf(DRINKORDER(), NOTHING()),
                optional(INTERJECTION()),
                TERMINAL()
        );
    }

We've had to move whitespace handling around a little, too, because of the use of `OF()` to serve as a connector rather than the simple word "of."

`OF()` now has to serve as a syntax rule for a single comma - with no whitespace - as you'd see in the string "<code>pint,beer</code>". It also has to handle whitespace - as you'd find in `pint , beer`. 

However, it needs to *mandate* whitespace for the actual word `of` - because `pintofbeer` doesn't work.

> Another exercise for the reader: fix `OF()` to handle "`pint, of beer`". 

`DRINK()` has a new `sequence` - `oneOrMore(testNot(INTERJECTION()), ANY)`. 

> Pay attention to this.

This means to match everything (as per the `ANY`) that does *not* match the `INTERJECTION()` rule. The sequence order is important - it tries to match the rules in order, so it checks the tokens (by looking ahead) against `INTERJECTION()` first, and failing that check (and therefore succeeding in the match - remember, we're looking for something that is *not* an `INTERJECTION()`) it checks to see if the text matches `ANY`.

Given that `ANY` matches anything, it succeeds - as long as the tokens are not tokens that match the `INTERJECTION()` rule.

And what does `INTERJECTION()` look like? Well, it's a normal rule - this is where Grappa really shines. Our `INTERJECTION()` has optional whitespace and punctuation, and it's own case insensitive matching:

    public Rule INTERJECTION() {
        return sequence(
                zeroOrMore(wsp()),
                optional(COMMA()),
                zeroOrMore(wsp()),
                trieIgnoreCase("please", "pls", "okay", "yo", "ok"),
                TERMINAL()
        );
    }

It *also* has the terminal condition for the order, because something might *look like* an interjection but wouldn't be. Consider this input: "`glass,water,please fine`." The `,please` matches an `INTERJECTION()`, but because the `INTERJECTION()` includes the `TERMINAL()` rule - which means "optional whitespace, an optional end-of-sentence, optional whitespace, and then a *definite* end-of-input" - "`,please fine`" fails the `INTERJECTION()` match, and falls back to `ANY`.

> `EOI` can match legally several times. That's why we can match it in our `INTERJECTION()` rule while still having it match the end of our `ORDER()` rule. The nature of `TERMINAL()` - being a series of optional elements - means that if it's matched as part of `INTERJECTION()` it won't match at the end of `ORDER()`. Such is life.

We can also order something like this: "`glass,water, please ok?`" -- and our drink would be a glass of "`water, please`" because "`ok`" would match the `INTERJECTION()` rule.

Our bartender's a great guy, but he likes his grammar.

Our [PoliteBartender](https://github.com/jottinger/grappaexample/blob/master/src/main/java/com/autumncode/bartender/PoliteBartender.java) class is different from our [Bartender](https://github.com/jottinger/grappaexample/blob/master/src/main/java/com/autumncode/bartender/Bartender.java) only in the [Parser](https://github.com/jottinger/grappaexample/blob/master/src/main/java/com/autumncode/bartender/PoliteBartender.java#L30) it uses and the originating [Rule](https://github.com/jottinger/grappaexample/blob/master/src/main/java/com/autumncode/bartender/PoliteBartender.java#L33) - and, of course, in the flexibility of the orders it accepts.

    $ java -cp bartender-1.0-SNAPSHOT.jar com.autumncode.bartender.PoliteBartender
    What're ya havin'? a glass of water
    Here's your glass of water. Please drink responsibly!
    What're ya havin'? a toeful of shoe polish
    I'm sorry, I don't understand. Try again?
    What're ya havin'? a pint of indigo ink, please
    Here's your pint of indigo ink. Please drink responsibly!
    What're ya havin'? A SPOON OF DOM PERIGNON, 1986, OK?
    Here's your spoon of dom perignon, 1986. Please drink responsibly!
    What're ya havin'? magnum,water,pls, please
    Here's your magnum of water,pls. Please drink responsibly!
    What're ya havin'? nothing
    $

## Colophon

By the way, much appreciation goes to the following individuals, who helped me write this in various important ways, and in no particular order:

* [Francis Galiegue](https://github.com/fge), who helped by reviewing the text, by pointing out various errors in my grammars, and by writing [Grappa](https://github.com/fge/grappa) in the first place
* [Chris Brenton](https://github.com/ChrisBrenton), who reviewed (a lot!) and helped me tune the messaging
* [Andreas Kirschbaum](https://github.com/akirschbaum), who also reviewed quite a bit for the article, especially in early form