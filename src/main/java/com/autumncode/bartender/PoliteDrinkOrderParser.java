package com.autumncode.bartender;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PoliteDrinkOrderParser extends BaseParser<DrinkOrder> {
    Collection<String> vessels = Stream
            .of(Vessel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    protected boolean assignDrink() {
        peek().setDescription(match().toLowerCase().trim().replaceAll("\\s+", " "));
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
                join(
                        sequence(
                                oneOrMore(ANY),
                                testNot(INTERJECTION())
                        )
                ).using(oneOrMore(wsp()))
                        .min(1),
                assignDrink());
        //return join(oneOrMore(ANY, testNot(INTERJECTION())));, assignDrink());
    }

    public Rule DRINKORDER() {
        return sequence(
                optional(sequence(
                        ARTICLE(),
                        oneOrMore(wsp())
                )),
                VESSEL(),
                oneOrMore(wsp()),
                OF(),
                oneOrMore(wsp()),
                DRINK()
        );
    }

    public Rule INTERJECTION() {
        return sequence(
                zeroOrMore(wsp()),
                optional(string(",")),
                zeroOrMore(wsp()),
                ignoreCase("please")
        );
    }

    public Rule ORDER() {
        return sequence(
                push(new DrinkOrder()),
                zeroOrMore(wsp()),
                firstOf(DRINKORDER(), NOTHING()),
                optional(INTERJECTION()),
                zeroOrMore(wsp()),
                EOI
        );
    }
}
