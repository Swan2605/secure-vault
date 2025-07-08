import javax.crypto.*;
import javax.crypto.spec.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

public class SecureStorage {
    // Encryption Constants
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int AES_KEY_SIZE = 32; // 256-bit AES
    
    // Folder Paths
    private static final File normalFolder = new File("data/normal_files");
    private static final File encryptedFolder = new File("data/encrypted_files");
    private static final File decryptedFolder = new File("data/decrypted_files"); 
    private static final File keyFolder = new File("data/keys");
    
    // Initialize folders
    static {
        normalFolder.mkdirs();
        encryptedFolder.mkdirs();
        decryptedFolder.mkdirs();
        keyFolder.mkdirs();
    }

    // Key Generation
    public static void generateRSAKeys(String privateKeyPath, String publicKeyPath) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        try (FileOutputStream fos = new FileOutputStream(new File(keyFolder, privateKeyPath))) {
            fos.write(keyPair.getPrivate().getEncoded());
        }
        try (FileOutputStream fos = new FileOutputStream(new File(keyFolder, publicKeyPath))) {
            fos.write(keyPair.getPublic().getEncoded());
        }
    }

    // Key Loading
    public static PublicKey loadPublicKey(String publicKeyPath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(keyFolder.getPath(), publicKeyPath));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(spec);
    }

    public static PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(keyFolder.getPath(), privateKeyPath));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(spec);
    }

    // Encryption
    public static void encryptFile(File file, PublicKey publicKey) throws Exception {
        String encryptedFileName = file.getName() + ".enc";
        File encryptedFile = new File(encryptedFolder, encryptedFileName);

        if (encryptedFile.exists()) {
            JOptionPane.showMessageDialog(null, "This file has already been encrypted!", "Duplicate File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Generate random AES key
        byte[] aesKey = new byte[AES_KEY_SIZE];
        new SecureRandom().nextBytes(aesKey);
        SecretKey secretKey = new SecretKeySpec(aesKey, "AES");
        
        // Initialize AES cipher
        Cipher aesCipher = Cipher.getInstance(AES_ALGORITHM);
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = aesCipher.getIV();

        // Encrypt file contents
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] encryptedFileBytes = aesCipher.doFinal(fileBytes);

        // Encrypt AES key with RSA
        Cipher rsaCipher = Cipher.getInstance(RSA_ALGORITHM);
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = rsaCipher.doFinal(aesKey);

        // Write encrypted file
        try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
            fos.write(encryptedKey);
            fos.write(iv);
            fos.write(encryptedFileBytes);
        }
    }

    // Decryption
    public static void decryptFile(File file, PrivateKey privateKey) throws Exception {
        String decryptedFileName = file.getName().replace(".enc", ".dec");
        File decryptedFile = new File(decryptedFolder, decryptedFileName);

        if (decryptedFile.exists()) {
            JOptionPane.showMessageDialog(null, "This file has already been decrypted!", "Duplicate File", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            // Read file structure
            byte[] encryptedKey = fis.readNBytes(256);
            byte[] iv = fis.readNBytes(16);
            byte[] encryptedFileBytes = fis.readAllBytes();

            // Decrypt AES key with RSA
            Cipher rsaCipher = Cipher.getInstance(RSA_ALGORITHM);
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKey = rsaCipher.doFinal(encryptedKey);

            // Decrypt file contents with AES
            Cipher aesCipher = Cipher.getInstance(AES_ALGORITHM);
            aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
            byte[] decryptedFileBytes = aesCipher.doFinal(encryptedFileBytes);

            // Write decrypted file
            try (FileOutputStream fos = new FileOutputStream(decryptedFile)) {
                fos.write(decryptedFileBytes);
            }
        }
    }

    // Cloud Operations
    private static void handleCloudUpload(JFrame parent) {
        JFileChooser chooser = new JFileChooser(encryptedFolder);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                SupabaseManager.uploadFile(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(parent, "File uploaded to cloud successfully!");
            } catch (Exception ex) {
                showError(parent, "Upload failed", ex);
            }
        }
    }

    private static void handleCloudDownload(JFrame parent) {
        String filename = JOptionPane.showInputDialog(parent, "Enter filename to download:");
        if (filename != null && !filename.trim().isEmpty()) {
            File dest = new File(encryptedFolder, filename);
            try {
                SupabaseManager.downloadFile(filename, dest);
                JOptionPane.showMessageDialog(parent, 
                    "Downloaded to:\n" + dest.getAbsolutePath());
            } catch (Exception ex) {
                showError(parent, "Download failed", ex);
            }
        }
    }

    private static void showError(Component parent, String title, Exception ex) {
        JTextArea textArea = new JTextArea(ex.getMessage(), 10, 40);
        JScrollPane scrollPane = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(parent, scrollPane, title, JOptionPane.ERROR_MESSAGE);
    }

    // GUI Setup
    public static void main(String[] args) {
        JFrame frame = new JFrame("Secure File Storage with Cloud Backup");
        frame.setSize(700, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(0, 2, 5, 5));

        // Core Functionality Buttons
        JButton generateKeysButton = new JButton("Generate RSA Keys");
        JButton encryptButton = new JButton("Encrypt File");
        JButton decryptButton = new JButton("Decrypt File");
        JButton addFileButton = new JButton("Add Local File");
        
        // Cloud Functionality Buttons
        JButton uploadButton = new JButton("Upload to Cloud");
        JButton downloadButton = new JButton("Download from Cloud");
        
        // Action Listeners
        generateKeysButton.addActionListener(e -> {
            try {
                generateRSAKeys("private.pem", "public.pem");
                JOptionPane.showMessageDialog(frame, "RSA Keys Generated Successfully!");
            } catch (Exception ex) {
                showError(frame, "Key Generation Failed", ex);
            }
        });

        encryptButton.addActionListener(e -> {
            File[] files = normalFolder.listFiles((dir, name) -> !name.endsWith(".enc") && !name.endsWith(".dec"));
            if (files == null || files.length == 0) {
                JOptionPane.showMessageDialog(frame, "No files to encrypt in normal_files folder.");
                return;
            }

            String[] fileNames = Arrays.stream(files).map(File::getName).toArray(String[]::new);
            String selected = (String) JOptionPane.showInputDialog(frame, 
                "Select file to encrypt:", "Encrypt", 
                JOptionPane.PLAIN_MESSAGE, null, fileNames, fileNames[0]);

            if (selected != null) {
                try {
                    PublicKey publicKey = loadPublicKey("public.pem");
                    encryptFile(new File(normalFolder, selected), publicKey);
                    JOptionPane.showMessageDialog(frame, "File encrypted successfully!");
                } catch (Exception ex) {
                    showError(frame, "Encryption Failed", ex);
                }
            }
        });

        decryptButton.addActionListener(e -> {
            File[] files = encryptedFolder.listFiles((dir, name) -> name.endsWith(".enc"));
            if (files == null || files.length == 0) {
                JOptionPane.showMessageDialog(frame, "No encrypted files found.");
                return;
            }

            String[] fileNames = Arrays.stream(files).map(File::getName).toArray(String[]::new);
            String selected = (String) JOptionPane.showInputDialog(frame, 
                "Select file to decrypt:", "Decrypt", 
                JOptionPane.PLAIN_MESSAGE, null, fileNames, fileNames[0]);

            if (selected != null) {
                try {
                    PrivateKey privateKey = loadPrivateKey("private.pem");
                    decryptFile(new File(encryptedFolder, selected), privateKey);
                    JOptionPane.showMessageDialog(frame, "File decrypted successfully!");
                } catch (Exception ex) {
                    showError(frame, "Decryption Failed", ex);
                }
            }
        });

        addFileButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                if (!selected.getName().endsWith(".enc") && !selected.getName().endsWith(".dec")) {
                    try {
                        Files.copy(selected.toPath(), new File(normalFolder, selected.getName()).toPath());
                        JOptionPane.showMessageDialog(frame, "File added successfully!");
                    } catch (IOException ex) {
                        showError(frame, "File Add Failed", ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, 
                        "Cannot add encrypted/decrypted files directly.");
                }
            }
        });

        uploadButton.addActionListener(e -> handleCloudUpload(frame));
        downloadButton.addActionListener(e -> handleCloudDownload(frame));

        // Add components to frame
        frame.add(generateKeysButton);
        frame.add(addFileButton);
        frame.add(encryptButton);
        frame.add(decryptButton);
        frame.add(uploadButton);
        frame.add(downloadButton);

        frame.setVisible(true);
    }
}