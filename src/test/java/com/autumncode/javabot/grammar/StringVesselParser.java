package com.autumncode.javabot.grammar;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;


public class StringVesselParser extends BaseParser<String> {
    public Rule VESSEL() {
        return firstOf("pint", "bowl");
    }
}
