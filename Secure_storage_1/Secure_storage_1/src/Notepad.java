import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Notepad extends JFrame {

    private JTextArea textArea;
    private final String secretPhrase = "this is good"; // You can update this phrase
    private final JFileChooser fileChooser = new JFileChooser();

    public Notepad() {
        setTitle("Simple Notepad");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        createMenuBar();

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String content = textArea.getText().trim().toLowerCase();
                    if (content.contains(secretPhrase.toLowerCase())) {
                        launchSecureStorage();
                    } else {
                        JOptionPane.showMessageDialog(null, "Access Denied. Not a valid phrase.");
                    }
                }
            }
        });
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JLabel fileLabel = new JLabel("  File  ");
        JLabel viewLabel = new JLabel("  View  ");
        JLabel editLabel = new JLabel("  Edit  ");

        menuBar.add(fileLabel);
        menuBar.add(viewLabel);
        menuBar.add(editLabel);

        setJMenuBar(menuBar);
    }

    private void launchSecureStorage() {
        try {
            SecureStorage.main(null);  // Launch Secure Vault
            dispose(); // Close the Notepad window
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to launch Secure Vault.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Notepad notepad = new Notepad();
            notepad.setVisible(true);
        });
    }
}