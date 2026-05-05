package src.main.java.com.app.common.entity;

public abstract class User extends BaseEntity {
    private String username, password, email;
//    public static final int permissionLevel = 3;

    public User(String username, String password, String email){
        this.username = username;
        this.password = password;
        this.email = email;
        System.out.println("Tai khoan da duoc tao."); // Sau nay luu log lai
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
