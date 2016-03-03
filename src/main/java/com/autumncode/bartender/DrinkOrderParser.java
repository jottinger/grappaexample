package com.autumncode.bartender;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrinkOrderParser extends BaseParser<DrinkOrder> {
    Collection<String> vessels = Stream
            .of(Vessel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    public boolean assignDrink() {
        peek().setDescription(match().toLowerCase().replaceAll("\\s+", " "));
        return true;
    }

    public boolean setTerminal() {
        peek().setTerminal(true);
        return true;
    }

    public Rule NOTHING() {
        return sequence(
                trieIgnoreCase("nothing", "nada", "zilch", "done"),
                setTerminal(),
                EOI
        );
    }

    public Rule ARTICLE() {
        return trieIgnoreCase("a", "an", "the");
    }

    public Rule OF() {
        return
                ignoreCase("of");
    }


    public boolean assignVessel() {
        peek().setVessel(Vessel.valueOf(match().toUpperCase()));
        return true;
    }

    public Rule DRINK() {
        return sequence(
                join(oneOrMore(firstOf(alpha(), digit())))
                        .using(oneOrMore(wsp()))
                        .min(1),
                assignDrink()
        );
    }

    public Rule vesselType() {
        return sequence(
                trieIgnoreCase(vessels),
                assignVessel()
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
                                vesselType(),
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
