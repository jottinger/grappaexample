package com.autumncode.bartender;

import lombok.Data;

@Data
public class DrinkOrder {
    Vessel vessel;
    String description;
    boolean terminal;
}
