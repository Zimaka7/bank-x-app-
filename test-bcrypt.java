import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Test the passwords
        String[] passwords = {"admin123", "operator123", "auditor123"};
        String[] hashes = {
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.",
            "$2a$10$VEjxf0jfc2WnNzKWHBTtVebtFtkdz2IdAotTstVWoalQ1lhHgdWW2",
            "$2a$10$ZenpA7.WCVX2GceAomUOaOkjrjzgEWLBaYBzubeisf4zklNYz1QAa"
        };
        
        for (int i = 0; i < passwords.length; i++) {
            boolean matches = encoder.matches(passwords[i], hashes[i]);
            System.out.println("Password: " + passwords[i] + " -> " + matches);
        }
        
        // Generate new hashes
        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("New hash for " + password + ": " + hash);
        }
    }
}