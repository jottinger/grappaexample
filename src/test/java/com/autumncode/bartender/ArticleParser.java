package com.autumncode.bartender;

import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;

public class ArticleParser extends BaseParser<Void> {
    public Rule article() {
        return trieIgnoreCase("a", "an", "the");
    }

    public Rule articleTerminal() {
        return sequence(
                article(),
                EOI
        );
    }

    public Rule articleWithWhitespace() {
        return sequence(
                zeroOrMore(wsp()),
                article(),
                zeroOrMore(wsp()),
                EOI
        );
    }
}
