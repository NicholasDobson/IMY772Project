package za.co.tuks.amrdashboard.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(BackendApplication.class, args);
	}

	private static void loadDotEnv() {
		Path[] candidates = {
				Paths.get(".env"),
				Paths.get("backend/.env"),
				Paths.get("../.env"),
		};
		for (Path p : candidates) {
			if (!Files.isRegularFile(p)) continue;
			try {
				for (String raw : Files.readAllLines(p)) {
					String line = raw.trim();
					if (line.isEmpty() || line.startsWith("#")) continue;
					int eq = line.indexOf('=');
					if (eq <= 0) continue;
					String key = line.substring(0, eq).trim();
					String val = line.substring(eq + 1).trim();
					if (val.length() >= 2 &&
						((val.startsWith("\"") && val.endsWith("\"")) ||
						 (val.startsWith("'")  && val.endsWith("'")))) {
						val = val.substring(1, val.length() - 1);
					}
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, val);
					}
				}
				System.out.println("[env] Loaded " + p.toAbsolutePath());
				return;
			} catch (Exception e) {
				System.err.println("[env] Failed to read " + p + ": " + e.getMessage());
			}
		}
	}

}
