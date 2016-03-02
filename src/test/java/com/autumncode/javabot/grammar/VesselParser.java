package com.autumncode.javabot.grammar;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class VesselParser extends BaseParser<Vessel> {
    Collection<String> vessels= Stream.of(Vessel.values()).map(Enum::name).collect(Collectors.toList());

    public Rule vesselType() {
        return trieIgnoreCase(vessels);
    }

    public Rule VESSEL() {
        return sequence(vesselType(),push(Vessel.valueOf(match().toUpperCase())));
    }
}
