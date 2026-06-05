package dispatcher;

import controller.SocialNetworkController;
import model.SuggestedFriend;
import utils.DataSynchronizer;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        // 1. Initialize the controller (it contains an empty graph)
        SocialNetworkController controller = new SocialNetworkController();

        // 2. Trigger automatic data loading during startup
        DataSynchronizer.loadDataOnStartup(controller);

        // 3. Start the server for client requests (for example, a console menu)
        System.out.println("\n[SERVER READY] The social network system is ready!");

        // 3. Test the friend suggestion feature for Alice (ID: 1)
        System.out.println("\n--- Friend suggestions for Alice ---");
        ArrayList<SuggestedFriend> suggestions = controller.suggestMutualFriends(1);

        for (SuggestedFriend sf : suggestions) {
            System.out.println("Suggested user ID " + sf.getUserId() + " | Mutual friends: " + sf.getMutualFriendsCount());
        }
    }
}
