package dispatcher;

import controller.SocialNetworkController;
import view.ConsoleView;

public class Main {
    private static final SocialNetworkController CONTROLLER = new SocialNetworkController();

    private Main() {
    }

    public static void main(String[] args) {
        ConsoleView.displayMessage("Social Network Friend Suggestion");

        boolean running = true;
        while (running) {
            ConsoleView.displayMenu();
            String choice = ConsoleView.promptMenuChoice().trim();

            switch (choice) {
                case "1":
                    CONTROLLER.registerUser(
                            ConsoleView.promptUserId("Enter user ID"),
                            ConsoleView.promptUserName("Enter full name"));
                    break;
                case "2":
                    CONTROLLER.makeFriend(
                            ConsoleView.promptUserId("Enter first user ID"),
                            ConsoleView.promptUserId("Enter second user ID"));
                    break;
                case "3":
                    CONTROLLER.unFriend(
                            ConsoleView.promptUserId("Enter first user ID"),
                            ConsoleView.promptUserId("Enter second user ID"));
                    break;
                case "4":
                    CONTROLLER.suggestMutualFriends(
                            ConsoleView.promptUserId("Enter user ID"));
                    break;
                case "5":
                    CONTROLLER.showUsers();
                    break;
                case "6":
                    CONTROLLER.showRelationshipGraph();
                    break;
                case "7":
                    CONTROLLER.removeUser(ConsoleView.promptUserId("Enter user ID"));
                    break;
                case "0":
                    running = false;
                    ConsoleView.displayMessage("Goodbye.");
                    break;
                default:
                    ConsoleView.displayMessage("Invalid menu option.");
                    break;
            }
        }
    }
}


