package dispatcher;

import controller.SocialNetworkController;
import view.ConsoleView;

public class Main {
    public static SocialNetworkController controller = new SocialNetworkController();
    public static void main(String[] args) {
        ConsoleView.displayMessage("Social Network Friend Suggestion");
        
    }
}
