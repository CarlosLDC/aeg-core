import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Matches: " + encoder.matches("aeg-r1", "$2a$10$8.UnVuG9HHgffUDAlk8q6uyQ6Z7.L6TqQZ/uIqS7S7/v0Tj8J1LTC"));
    }
}
