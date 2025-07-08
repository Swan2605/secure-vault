import java.net.http.*;
import java.net.URI;
import java.nio.file.*;
import java.io.File;

public class SupabaseManager {
    private static final String SUPABASE_URL = "https://gosxvdiccdenyrsqkbgr.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imdvc3h2ZGljY2Rlbnlyc3FrYmdyIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NDUxNzcyNiwiZXhwIjoyMDYwMDkzNzI2fQ.9kjEIK-lapg-4tS6-MJWInD0ExQmVx_hQWHfZ3-mVnw"; // Your key
    private static final String BUCKET = "encrypted-files";
    

    public static void uploadFile(File file) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String url = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + file.getName();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/octet-stream")
            .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath()))
            .build();

        HttpResponse<String> response = client.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Upload failed: " + response.body());
        }
    }

    public static void downloadFile(String filename, File destination) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String url = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + filename;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + API_KEY)
            .GET()
            .build();

        HttpResponse<Path> response = client.send(
            request, HttpResponse.BodyHandlers.ofFile(destination.toPath())
        );

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Download failed: " + response.statusCode());
        }
    }
}