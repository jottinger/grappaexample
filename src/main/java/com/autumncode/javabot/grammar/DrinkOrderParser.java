package com.autumncode.javabot.grammar;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrinkOrderParser extends BaseParser<DrinkOrder> {
    Collection<String> vessels = Stream.of(Vessel.values()).map(Enum::name).collect(Collectors.toList());

public boolean assignVessel() {
    peek().vessel=Vessel.valueOf(match().toUpperCase());
    return true;
}

    public boolean assignDrink() {
        peek().description=match();
        return true;
    }

    public Rule ARTICLE() {
        return sequence(
                zeroOrMore(wsp()),
                trieIgnoreCase("a", "an", "the"),
                oneOrMore(wsp())
        );
    }

    public Rule OF() {
        return sequence(
                wsp(),
                ignoreCase("of"),
                wsp()
        );
    }

    public Rule DRINK() {
        return join(oneOrMore(alpha())).using(oneOrMore(wsp())).min(1);
    }

    public Rule vesselType() {
        return trieIgnoreCase(vessels);
    }

    public Rule DRINKORDER() {
        return sequence(
                push(new DrinkOrder()),
                optional(ARTICLE()),
                vesselType(),
                assignVessel(),
                OF(),
                DRINK(),
                assignDrink()
        );
    }
}
