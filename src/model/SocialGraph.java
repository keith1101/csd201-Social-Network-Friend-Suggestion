package model;

import java.util.ArrayList;
import java.util.LinkedList;

public class SocialGraph {
    // Lưu thông tin định danh (Đỉnh)
    private ArrayList<User> vertices;
    
    // Danh sách kề lưu quan hệ bạn bè (Cạnh)
    // Index của adjList tương ứng với index của vertices
    private ArrayList<LinkedList<Integer>> adjList;

    public SocialGraph() {
        this.vertices = new ArrayList<>();
        this.adjList = new ArrayList<>();
    }

    // Thêm User mới vào đồ thị
    public void addUser(User user) {
        vertices.add(user);
        adjList.add(new LinkedList<>()); // Khởi tạo danh sách bạn bè rỗng
    }

    // Lấy index của User trong mảng vertices dựa vào ID
    private int getIndexById(int userId) {
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).getId() == userId) {
                return i;
            }
        }
        return -1; // Không tìm thấy
    }

    // Thêm cạnh vô hướng (Kết bạn)
    public boolean addEdge(int uId, int vId) {
        int uIndex = getIndexById(uId);
        int vIndex = getIndexById(vId);

        if (uIndex == -1 || vIndex == -1) return false; // Lỗi: Không tồn tại user

        // Lấy danh sách kề của 2 user
        LinkedList<Integer> uFriends = adjList.get(uIndex);
        LinkedList<Integer> vFriends = adjList.get(vIndex);

        // Kiểm tra trùng lặp
        if (!uFriends.contains(vId)) {
            uFriends.add(vId);
            vFriends.add(uId);
            return true;
        }
        return false; // Đã là bạn bè
    }

    // Lấy danh sách ID bạn bè của 1 user
    public LinkedList<Integer> getFriendsOf(int userId) {
        int index = getIndexById(userId);
        if (index != -1) return adjList.get(index);
        return new LinkedList<>();
    }

    public ArrayList<User> getVertices() {
        return vertices;
    }
}
