package view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import model.SuggestedFriend;
import model.User;

public class ConsoleView {
    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleView() {
    }

    public static void displayMenu() {
        System.out.println();
        System.out.println("========== Social Network Friend Suggestion ==========");
        System.out.println("1. Register user");
        System.out.println("2. Make friend");
        System.out.println("3. Unfriend");
        System.out.println("4. Suggest friends");
        System.out.println("5. Show all users");
        System.out.println("6. Show relationship graph");
        System.out.println("7. Remove user");
        System.out.println("0. Exit");
        System.out.println("======================================================");
    }

    public static String promptMenuChoice() {
        return prompt("Choose an option");
    }

    public static String promptUserId(String message) {
        return prompt(message);
    }

    public static String promptUserName(String message) {
        return prompt(message);
    }

    private static String prompt(String message) {
        System.out.print(message + ": ");
        return SCANNER.nextLine();
    }

    public static void displayFriendList(ArrayList<User> users) {
        System.out.println();
        System.out.println("Friend List");
        System.out.println("-----------");

        if (users == null || users.isEmpty()) {
            System.out.println("No friends found.");
            return;
        }

        for (User user : users) {
            System.out.println(user);
        }
    }


    public static void displayUserList(ArrayList<User> users) {
        System.out.println();
        System.out.println("User List");
        System.out.println("---------");

        if (users == null || users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        System.out.printf("%-10s %s%n", "User ID", "Full Name");
        System.out.println("------------------------------");
        for (User user : users) {
            System.out.printf("%-10d %s%n", user.getId(), user.getFullName());
        }
    }

    public static void displayRelationshipGraph(
            ArrayList<User> users,
            Map<Integer, ArrayList<Integer>> relationships) {
        System.out.println();
        System.out.println("User Relationship Graph");
        System.out.println("-----------------------");

        if (users == null || users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        Map<Integer, String> namesById = new HashMap<>();
        for (User user : users) {
            namesById.put(user.getId(), user.getFullName());
        }

        for (User user : users) {
            ArrayList<Integer> friendIds = relationships.get(user.getId());
            System.out.print("[" + user.getId() + "] " + user.getFullName() + " -> ");

            if (friendIds == null || friendIds.isEmpty()) {
                System.out.println("(no connections)");
                continue;
            }

            for (int i = 0; i < friendIds.size(); i++) {
                int friendId = friendIds.get(i);
                if (i > 0) {
                    System.out.print(", ");
                }
                System.out.print(
                        "[" + friendId + "] "
                                + namesById.getOrDefault(friendId, "Unknown User"));
            }
            System.out.println();
        }
    }
    public static void displaySuggestedFriends(ArrayList<SuggestedFriend> result) {
        System.out.println();
        System.out.println("Suggested Friends");
        System.out.println("-----------------");

        if (result == null || result.isEmpty()) {
            System.out.println("No friend suggestions found.");
            return;
        }

        for (SuggestedFriend suggestedFriend : result) {
            System.out.println(
                    "User ID: " + suggestedFriend.getSuggestedId()
                            + " | Mutual friends: " + suggestedFriend.getMutualCount());
        }
    }

    public static void displayMessage(String message) {
        System.out.println(message);
    }
}


