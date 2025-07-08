import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.http.*;
import java.net.URI;
import java.nio.file.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URLEncoder;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;

public class SecureStorage {
    // Encryption constants
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int AES_KEY_SIZE = 256;

    // Folder paths
    private static final String NORMAL_FOLDER = "normal_files/";
    private static final String ENCRYPTED_FOLDER = "encrypted_files/";
    private static final String DECRYPTED_FOLDER = "decrypted_files/";
    private static final String KEY_FOLDER = "keys/";

    // Supabase configuration
    private static final String SUPABASE_URL = "https://gosxvdiccdenyrsqkbgr.supabase.co";
    private static final String PUBLIC_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imdvc3h2ZGljY2Rlbnlyc3FrYmdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ1MTc3MjYsImV4cCI6MjA2MDA5MzcyNn0.uczaWb29l-olnxmCFo7GGeE5BC2vOu4BjsIFinIXjHg";
    private static final String SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imdvc3h2ZGljY2Rlbnlyc3FrYmdyIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NDUxNzcyNiwiZXhwIjoyMDYwMDkzNzI2fQ.9kjEIK-lapg-4tS6-MJWInD0ExQmVx_hQWHfZ3-mVnw";
    private static final String BUCKET = "encrypted-files";

    private JFrame frame;
    private JTextArea logArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SecureStorage app = new SecureStorage();
            new LoginScreen(app).setVisible(true);
        });
    }

    public SecureStorage() {
        initialize();
        createFolders();
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    private void initialize() {
        frame = new JFrame("Secure File Vault");
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createButton("🔑 View Keys", e -> viewKeys(), buttonPanel);
        createButton("🔒 Encrypt File", e -> encryptFile(), buttonPanel);
        createButton("🔓 Decrypt File", e -> decryptFile(), buttonPanel);
        createButton("📁 Add Local File", e -> addLocalFile(), buttonPanel);
        createButton("☁ Upload to Cloud", e -> uploadToCloud(), buttonPanel);
        createButton("☁️ Download from Cloud", e -> downloadFromCloud(), buttonPanel);
        createButton("🗂 View Encrypted", e -> viewAndDeleteFiles(ENCRYPTED_FOLDER), buttonPanel);
        createButton("🗂 View Decrypted", e -> viewAndDeleteFiles(DECRYPTED_FOLDER), buttonPanel);
        createButton("🗂 View Normal", e -> viewAndDeleteFiles(NORMAL_FOLDER), buttonPanel);
        createButton("☁️ View Cloud Files", e -> viewAndDeleteCloudFiles(), buttonPanel);

        frame.add(buttonPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        logArea.setBackground(new Color(240, 248, 255));
        logArea.setForeground(Color.DARK_GRAY);
        logArea.setMargin(new Insets(10, 10, 10, 10));
        logArea.setEditable(false);

        // Create a split pane to show log and file explorer view side-by-side
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Left side: log output
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Vault Logs"));

        // Right side: file explorer-style folder viewer
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Vault Files");
        addFolderToTree(new File(NORMAL_FOLDER), "Normal", root);
        addFolderToTree(new File(ENCRYPTED_FOLDER), "Encrypted", root);
        addFolderToTree(new File(DECRYPTED_FOLDER), "Decrypted", root);

        JTree tree = new JTree(root);
        JScrollPane treeScrollPane = new JScrollPane(tree);
        treeScrollPane.setPreferredSize(new Dimension(250, 600));
        treeScrollPane.setBorder(BorderFactory.createTitledBorder("Vault Explorer"));

        splitPane.setLeftComponent(treeScrollPane);
        splitPane.setRightComponent(logScrollPane);
        splitPane.setDividerLocation(300);

        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    private void addFolderToTree(File folder, String label, DefaultMutableTreeNode root) {
        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(label);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                folderNode.add(new DefaultMutableTreeNode(file.getName()));
            }
        }
        root.add(folderNode);
    }

    private void createButton(String text, ActionListener listener, JPanel panel) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.addActionListener(listener);
        panel.add(button);
    }

    private void createFolders() {
        new File(NORMAL_FOLDER).mkdirs();
        new File(ENCRYPTED_FOLDER).mkdirs();
        new File(DECRYPTED_FOLDER).mkdirs();
        new File(KEY_FOLDER).mkdirs();
    }

    private void log(String message) {
        logArea.append(message + "\n");
    }

    private void addLocalFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                Files.copy(selectedFile.toPath(),
                        Paths.get(NORMAL_FOLDER + selectedFile.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
                log("File added: " + selectedFile.getName());
            } catch (IOException e) {
                log("Error adding file: " + e.getMessage());
            }
        }
    }

    private void encryptFile() {
        File inputFile = new File(NORMAL_FOLDER);
        File[] files = inputFile.listFiles();

        if (files == null || files.length == 0) {
            log("No files to encrypt in " + NORMAL_FOLDER);
            return;
        }

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            SecretKey secretKey = keyGen.generateKey();

            String keyFileName = KEY_FOLDER + "key_" + System.currentTimeMillis() + ".key";
            try (FileOutputStream keyFos = new FileOutputStream(keyFileName)) {
                keyFos.write(secretKey.getEncoded());
            }

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            for (File file : files) {
                byte[] inputBytes = Files.readAllBytes(file.toPath());
                byte[] outputBytes = cipher.doFinal(inputBytes);

                String outputFileName = ENCRYPTED_FOLDER + file.getName() + ".enc";
                Files.write(Paths.get(outputFileName), outputBytes);

                log("Encrypted: " + file.getName() + " -> " + outputFileName);
            }
        } catch (Exception e) {
            log("Encryption error: " + e.getMessage());
        }
    }

    private void decryptFile() {
        File inputFile = new File(ENCRYPTED_FOLDER);
        File[] files = inputFile.listFiles((dir, name) -> name.endsWith(".enc"));

        if (files == null || files.length == 0) {
            log("No encrypted files found in " + ENCRYPTED_FOLDER);
            return;
        }

        try {
            File keyFolder = new File(KEY_FOLDER);
            File[] keyFiles = keyFolder.listFiles((dir, name) -> name.endsWith(".key"));

            if (keyFiles == null || keyFiles.length == 0) {
                log("No key files found in " + KEY_FOLDER);
                return;
            }

            Arrays.sort(keyFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            byte[] keyBytes = Files.readAllBytes(keyFiles[0].toPath());
            SecretKey secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            for (File file : files) {
                byte[] inputBytes = Files.readAllBytes(file.toPath());
                byte[] outputBytes = cipher.doFinal(inputBytes);

                String originalName = file.getName().replace(".enc", "");
                String outputFileName = DECRYPTED_FOLDER + originalName;
                Files.write(Paths.get(outputFileName), outputBytes);

                log("Decrypted: " + file.getName() + " -> " + outputFileName);
            }
        } catch (Exception e) {
            log("Decryption error: " + e.getMessage());
        }
    }

    private void viewKeys() {
        File keyFolder = new File(KEY_FOLDER);
        File[] keyFiles = keyFolder.listFiles((dir, name) -> name.endsWith(".key"));

        if (keyFiles == null || keyFiles.length == 0) {
            log("No key files found in " + KEY_FOLDER);
            return;
        }

        StringBuilder keyList = new StringBuilder("Generated Keys:\n");
        for (File keyFile : keyFiles) {
            keyList.append("• ").append(keyFile.getName()).append("\n");
        }

        JOptionPane.showMessageDialog(frame,
                keyList.toString(),
                "Encryption Keys",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void uploadToCloud() {
        File inputFile = new File(ENCRYPTED_FOLDER);
        File[] files = inputFile.listFiles();

        if (files == null || files.length == 0) {
            log("No files to upload in " + ENCRYPTED_FOLDER);
            return;
        }

        for (File file : files) {
            try {
                SupabaseManager.uploadFile(file);
                log("Uploaded to cloud: " + file.getName());
            } catch (Exception e) {
                log("Upload failed for " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void downloadFromCloud() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = SUPABASE_URL + "/storage/v1/object/list/" + BUCKET;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/list/" + BUCKET))
                .header("Authorization", "Bearer " + SERVICE_KEY)
                .header("apikey", PUBLIC_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"prefix\":\"\"}"))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                String[] entries = body.split("\\{");

                for (String entry : entries) {
                    if (entry.contains("\"name\":\"")) {
                        String filename = entry.split("\"name\":\"")[1].split("\"")[0];
                        if (!filename.trim().isEmpty()) {
                            File destination = new File(ENCRYPTED_FOLDER + filename.trim());
                            SupabaseManager.downloadFile(filename.trim(), destination);
                            log("Downloaded from cloud: " + filename);
                        }
                    }
                }
            } else {
                log("Failed to list files: " + response.body());
            }
        } catch (Exception e) {
            log("Download error: " + e.getMessage());
        }
    }

    private void viewAndDeleteFiles(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            log("No files found in " + folderPath);
            return;
        }

        String[] fileNames = Arrays.stream(files).map(File::getName).toArray(String[]::new);
        JList<String> fileList = new JList<>(fileNames);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        int option = JOptionPane.showConfirmDialog(frame, scrollPane, "Files in: " + folderPath,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                int confirm = JOptionPane.showConfirmDialog(frame, "Delete file: " + selectedFile + "?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    File file = new File(folderPath + selectedFile);
                    if (file.delete()) {
                        log("Deleted file: " + selectedFile);
                    } else {
                        log("Failed to delete: " + selectedFile);
                    }
                }
            }
        }
    }

    private void viewAndDeleteCloudFiles() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = SUPABASE_URL + "/storage/v1/object/list/" + BUCKET;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/list/" + BUCKET))
                .header("Authorization", "Bearer " + SERVICE_KEY)
                .header("apikey", PUBLIC_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"prefix\":\"\"}"))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log("Failed to list cloud files: " + response.body());
                return;
            }

            List<String> filenames = new ArrayList<>();
            String body = response.body();
            String[] entries = body.split("\\{");

            for (String entry : entries) {
                if (entry.contains("\"name\":\"")) {
                    String name = entry.split("\"name\":\"")[1].split("\"")[0];
                    filenames.add(name);
                }
            }

            if (filenames.isEmpty()) {
                log("No files found in cloud.");
                return;
            }

            String[] fileNames = filenames.toArray(new String[0]);
            JList<String> fileList = new JList<>(fileNames);
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(fileList);
            scrollPane.setPreferredSize(new Dimension(400, 200));

            int option = JOptionPane.showConfirmDialog(frame, scrollPane, "Cloud Files",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    int confirm = JOptionPane.showConfirmDialog(frame, "Delete cloud file: " + selectedFile + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        deleteCloudFile(selectedFile);
                        log("Deleted cloud file: " + selectedFile);
                    }
                }
            }
        } catch (Exception e) {
            log("Cloud file view/delete error: " + e.getMessage());
        }
    }

    private void deleteCloudFile(String filename) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String url = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + java.net.URLEncoder.encode(filename, "UTF-8");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + SERVICE_KEY)
                .header("apikey", PUBLIC_API_KEY)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Failed to delete cloud file: " + response.body());
        }
    }

    private static class SupabaseManager {
        public static void uploadFile(File file) throws Exception {
            HttpClient client = HttpClient.newHttpClient();
            String url = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + java.net.URLEncoder.encode(file.getName(), "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + SERVICE_KEY)
                    .header("apikey", PUBLIC_API_KEY)
                    .header("Content-Type", "application/octet-stream")
                    .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Upload failed: " + response.body());
            }
        }

        public static void downloadFile(String filename, File destination) throws Exception {
            HttpClient client = HttpClient.newHttpClient();
            String url = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + java.net.URLEncoder.encode(filename, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/storage/v1/object/list/" + BUCKET))
                .header("Authorization", "Bearer " + SERVICE_KEY)
                .header("apikey", PUBLIC_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"prefix\":\"\"}"))
                .build();

            HttpResponse<Path> response = client.send(
                    request, HttpResponse.BodyHandlers.ofFile(destination.toPath()));

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Download failed: " + response.statusCode());
            }
        }
    }
}
