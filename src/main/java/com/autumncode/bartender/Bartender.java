package com.autumncode.bartender;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParsingResult;

import java.util.Scanner;

public class Bartender {
    public static void main(String[] args) {
        new Bartender().run();
    }

    public void run() {
        final Scanner scanner = new Scanner(System.in);
        boolean done = false;
        do {
            writePrompt();
            String order = scanner.nextLine();
            done = order == null || handleOrder(order);
        } while (!done);
    }

    private void writePrompt() {
        System.out.print("Your order? ");
        System.out.flush();
    }

    private boolean handleOrder(String order) {
    DrinkOrderParser parser
            = Grappa.createParser(DrinkOrderParser.class);
    ListeningParseRunner<DrinkOrder> runner
            = new ListeningParseRunner<>(parser.DRINKORDER());
    ParsingResult<DrinkOrder> result = runner.run(order);
    DrinkOrder drinkOrder;
    boolean done = false;
    if (result.isSuccess()) {
        drinkOrder = result.getTopStackValue();
        done = drinkOrder.isTerminal();
        if (!done) {
            System.out.printf("Here's your %s of %s. Please drink responsibly!%n",
                    drinkOrder.getVessel().toString().toLowerCase(),
                    drinkOrder.getDescription());
        }
    } else {
        System.out.println("I'm sorry, I don't understand. Try again?");
    }
    return done;
    }
}
