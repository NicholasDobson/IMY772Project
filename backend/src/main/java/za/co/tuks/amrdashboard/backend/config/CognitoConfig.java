package za.co.tuks.amrdashboard.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

    @Value("${cognito.region}")
    private String region;

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String accessKey = resolve(dotenv, "AWS_ACCESS_KEY_ID");
        String secretKey = resolve(dotenv, "AWS_SECRET_ACCESS_KEY");

        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    private String resolve(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        return (value != null && !value.isBlank()) ? value : System.getenv(key);
    }
}
