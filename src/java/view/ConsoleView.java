package view;

import java.util.ArrayList;
import model.SuggestedFriend;
import model.User;

public class ConsoleView {
    public static void displayMenu() {
        int choice;
        do {
            System.out.println("\n=== SOCIAL NETWORK SYSTEM (CSD201) ===");
            System.out.println("1. Login with User ID");
            System.out.println("2. View Friend List");
            System.out.println("3. Suggest Friends (Using Max-Heap)");
            System.out.println("9. [Dev Only] Run Performance Benchmarking");
            System.out.println("0. Exit");
            System.out.print("Select an option: ");

            // It's recommended to use the team's InputValidator here instead of raw nextInt()
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    // Logic for entering ID
                    break;
                case 2:
                    // Call method to print friend list from controller
                    break;
                case 3:
                    // Call method for friend suggestion
                    break;
                case 9:
                    runBenchmarkTest(); // This is your core task!
                    break;
                case 0:
                    System.out.println("Thank you for using the system!");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        } while (choice != 0);
    }

    public static void displayFriendList(ArrayList<User> users) {
        // chua lam
    }

    public static void displaySuggestedFriends(ArrayList<SuggestedFriend> result) {
        // chua lam
    }

    public static void displayMessage(String message) {
        System.out.println(message);
    }
}
