package com.autumncode.bartender;

import com.autumncode.bartender.Vessel;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ArticleVesselParser extends BaseParser<Vessel> {
    Collection<String> vessels = Stream.of(Vessel.values()).map(Enum::name).collect(Collectors.toList());

    public Rule ARTICLE() {
        return sequence(
                optional(oneOrMore(wsp())),
                trieIgnoreCase("a", "an", "the"),
                oneOrMore(wsp())
        );
    }

    public Rule vesselType() {
        return trieIgnoreCase(vessels);
    }

    public Rule VESSEL() {
        return sequence(
                optional(ARTICLE()),
                vesselType(),
                push(Vessel.valueOf(match().toUpperCase()))
        );
    }
}
