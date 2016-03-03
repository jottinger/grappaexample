package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;

import java.util.Scanner;

public class Runner {
    public static void main(String[] args) {
        new Runner().run();
    }

    private void run() {
        DrinkOrderParser parser = Grappa.createParser(DrinkOrderParser.class);
        Scanner lines = new Scanner(this.getClass().getResourceAsStream("/input.txt"));
        while (lines.hasNext()) {
            String line = lines.nextLine();

            ListeningParseRunner<DrinkOrder> runner = new ListeningParseRunner<>(parser.DRINKORDER());
            ParsingResult<DrinkOrder> order = runner.run(line);
            System.out.printf("Parsing: '%s': ", line);
            if (order.isSuccess()) {
                System.out.println(order.getTopStackValue());
            }
        }
    }
}
