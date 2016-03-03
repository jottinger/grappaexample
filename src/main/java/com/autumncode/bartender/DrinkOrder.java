package com.autumncode.bartender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrinkOrder {
    Vessel vessel;
    String description;
    boolean terminal;
}
