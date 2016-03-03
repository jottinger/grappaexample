package com.autumncode.bartender;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class VesselParser extends BaseParser<Vessel> {
    final static Collection<String> vessels = Stream
            .of(Vessel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    public Rule vessel() {
        return trieIgnoreCase(vessels);
    }

    public Rule VESSEL() {
        return sequence(
                vessel(),
                push(Vessel.valueOf(match().toUpperCase()))
        );
    }

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
}
