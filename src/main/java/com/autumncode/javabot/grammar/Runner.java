package com.autumncode.javabot.grammar;

import java.util.Scanner;

public class Runner {
    public static void main(String[] args) {
        new Runner().run();
    }

    private void run() {
        Scanner lines=new Scanner(this.getClass().getResourceAsStream("/input.txt"));
        while(lines.hasNext()) {
            String line=lines.nextLine();
            System.out.printf("'%s'%n",line);
        }
    }
}
