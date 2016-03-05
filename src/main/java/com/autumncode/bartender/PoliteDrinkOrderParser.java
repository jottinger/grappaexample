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
                zeroOrMore(wsp()),
                optional(EOS()),
                zeroOrMore(wsp()),
                EOI
                );
    }

    public Rule EOS() {
        return anyOf(".!?");
    }

    public Rule ORDER() {
        return sequence(
                push(new DrinkOrder()),
                zeroOrMore(wsp()),
                firstOf(DRINKORDER(), NOTHING()),
                optional(INTERJECTION()),
                zeroOrMore(wsp()),
                optional(EOS()),
                EOI
        );
    }
}
