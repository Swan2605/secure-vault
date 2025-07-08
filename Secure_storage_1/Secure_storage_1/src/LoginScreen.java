import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginScreen extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private final SecureStorage secureStorageApp;

    public LoginScreen(SecureStorage secureStorageApp) {
        this.secureStorageApp = secureStorageApp;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Secure Storage Login");
        setSize(350, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Email Field
        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);

        // Password Field
        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(this::handleLogin);
        panel.add(loginButton);

        // Admin Shortcut (remove in production)
        JButton adminBtn = new JButton("Use Admin");
        adminBtn.addActionListener(e -> {
            emailField.setText("admin");
            passwordField.setText("admin");
        });
        panel.add(adminBtn);

        add(panel);
    }

    private void handleLogin(ActionEvent e) {
        String email = emailField.getText();
        char[] password = passwordField.getPassword();
        
        if (email.isEmpty() || password.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both email and password", 
                "Login Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (SupabaseManager.authenticate(email, new String(password))) {
                secureStorageApp.setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Invalid credentials", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Login Error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            // Clear password field for security
            passwordField.setText("");
        }
    }

    public static void main(String[] args) {
        // Initialize the main application first
        SecureStorage secureStorage = new SecureStorage();
        
        // Create and show login screen
        SwingUtilities.invokeLater(() -> {
            LoginScreen loginScreen = new LoginScreen(secureStorage);
            loginScreen.setVisible(true);
        });
    }
}