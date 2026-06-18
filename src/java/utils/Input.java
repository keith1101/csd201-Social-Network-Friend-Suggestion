package utils;

import java.util.Scanner;

public class Input {
    private final Scanner scanner;

    public Input() {
        this.scanner = new Scanner(System.in);
    }

    public String inputString(String mess) {
        System.out.println(mess+": ");
        return scanner.nextLine();
    }

    public int inputInt(String mess) {
        System.out.println(mess+": ");
        return Integer.parseInt(scanner.nextLine());
    }
}
