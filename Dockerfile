# Sử dụng Tomcat 9 chạy trên nền Java 8 (JRE 8) làm môi trường gốc
FROM tomcat:9.0-jre8-openjdk-slim

# Xóa các ứng dụng mặc định của Tomcat để giải phóng bộ nhớ (tùy chọn)
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy file .war được tạo từ Ant vào thư mục webapps của Tomcat
# Đổi tên thành ROOT.war để khi truy cập web không cần gõ đuôi (ví dụ: mysite.onrender.com/)
COPY dist/SocialNetworkFriendSuggestion.war /usr/local/tomcat/webapps/ROOT.war

# Render sử dụng cổng mặc định là 8080 cho Tomcat, thông báo cho Render biết
EXPOSE 8080

# Lệnh khởi chạy Tomcat
CMD ["catalina.sh", "run"]