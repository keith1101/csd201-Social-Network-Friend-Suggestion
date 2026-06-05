package model;

public class User {
    private int id;
    private String fullName;

    public User(int id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    
    @Override
    public String toString() {
        return id + " - " + fullName;
    }
}
