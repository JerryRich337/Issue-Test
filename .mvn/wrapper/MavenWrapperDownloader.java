import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class MavenWrapperDownloader {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("Usage: MavenWrapperDownloader <wrapperUrl> <destJar>");
      System.exit(1);
    }

    String wrapperUrl = args[0];
    File dest = new File(args[1]);
    dest.getParentFile().mkdirs();

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(URI.create(wrapperUrl)).GET().build();
    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Failed to download wrapper jar: HTTP " + response.statusCode());
    }

    try (FileOutputStream out = new FileOutputStream(dest)) {
      out.write(response.body());
    }

    if (!Files.exists(dest.toPath()) || Files.size(dest.toPath()) == 0) {
      throw new IOException("Downloaded wrapper jar is empty");
    }

    System.out.println("Downloaded: " + dest.getAbsolutePath());
  }
}
