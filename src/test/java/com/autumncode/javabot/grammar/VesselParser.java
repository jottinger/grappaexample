package com.autumncode.javabot.grammar;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

import java.util.ArrayList;
import java.util.List;


public class VesselParser extends BaseParser<Vessel> {
    public Rule VESSEL() {

        return firstOf("pint", "bowl");
    }
}
