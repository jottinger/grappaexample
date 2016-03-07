package com.autumncode.bartender;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrinkOrderParser extends BaseParser<DrinkOrder> {
    static final Collection<String> vessels = Stream
            .of(Vessel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

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
                                optional(
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
}
