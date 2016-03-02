package com.autumncode.javabot.grammar;

import com.google.common.base.MoreObjects;

public class DrinkOrder {
    public Vessel vessel;
    public String description;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("vessel", vessel)
                .add("description", description)
                .toString();
    }
}
