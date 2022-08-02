import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.AuthorizationData;
import software.amazon.awssdk.services.ecr.model.GetAuthorizationTokenResponse;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.EC2;

public class LocalStackEcrTest
{
    static final String HTTPS_PROXY_LOWER_KEY = "https_proxy";
    static final String HTTPS_PROXY_UPPER_KEY = "HTTPS_PROXY";
    static final String HTTP_PROXY_LOWER_KEY = "http_proxy";
    static final String HTTP_PROXY_UPPER_KEY = "HTTP_PROXY";
    static final String NO_PROXY_UPPER_KEY = "NO_PROXY";
    static final String NO_PROXY_LOWER_KEY = "no_proxy";
    static final String LOCALSTACK_CONTAINER = "localstack/localstack:1.0.1";
    static final String LOCALSTACK_API_KEY_ENV_VAR = "LOCALSTACK_API_KEY";

    static final LocalStackContainer.EnabledService ECR = LocalStackContainer.EnabledService.named("ecr");

    static LocalStackContainer container;

    @BeforeClass
    public static void beforeClass()
    {
        System.out.println("BeforeClass");
        System.out.println("Using container: " + LOCALSTACK_CONTAINER);

        boolean useLegacyMode = false;
        container = new LocalStackContainer(DockerImageName.parse(LOCALSTACK_CONTAINER), useLegacyMode)
                .withServices(S3, STS, EC2, ECR)
                .withEnv(LOCALSTACK_API_KEY_ENV_VAR, /*INSERT API KEY HERE*/ "PLACEHOLDER")
                .withEnv("DEBUG", "1")
                .withEnv(HTTPS_PROXY_UPPER_KEY, System.getenv(HTTPS_PROXY_UPPER_KEY))
                .withEnv(HTTP_PROXY_UPPER_KEY, System.getenv(HTTP_PROXY_UPPER_KEY))
                .withEnv(HTTPS_PROXY_LOWER_KEY, System.getenv(HTTPS_PROXY_LOWER_KEY))
                .withEnv(HTTP_PROXY_LOWER_KEY, System.getenv(HTTP_PROXY_LOWER_KEY))
                .withEnv(NO_PROXY_UPPER_KEY, System.getenv(NO_PROXY_UPPER_KEY))
                .withEnv(NO_PROXY_LOWER_KEY, System.getenv(NO_PROXY_LOWER_KEY))
                .withEnv("JAVA_OPTS", System.getenv("JAVA_OPTS"))
                .withStartupTimeout(Duration.of(3, ChronoUnit.MINUTES));

        System.out.println("Starting LocalStack container");
        container.start();
        System.out.println("LocalStack container started");

        System.out.println("LocalStack S3 Endpoint: " + container.getEndpointOverride(S3));
        System.out.println("LocalStack STS Endpoint: " + container.getEndpointOverride(STS));
        System.out.println("LocalStack EC2 Endpoint: " + container.getEndpointOverride(EC2));
        System.out.println("LocalStack ECR Endpoint: " + container.getEndpointOverride(ECR));
        System.out.println("env: " + container.getEnvMap());
    }

    @Test
    public void test() throws TimeoutException
    {
        List<Region> regions = Arrays.asList(Region.US_EAST_1, Region.US_WEST_2, Region.AP_SOUTH_1, Region.ME_SOUTH_1);
        regions.forEach(region -> {
            try (S3Client s3 = getS3Client(region))
            {
                System.out.printf("Creating LocalStack S3 Bucket in %s%n", region);
                s3.createBucket(b -> b.bucket("a-bucket-" + region));
            }

            try (EcrClient ecr = getEcrClient(region))
            {
                System.out.printf("Creating LocalStack ECR verafin/remoteagent repository in %s%n", region);
                ecr.createRepository(r -> r.repositoryName("our-ecr-repo"));

                System.out.println(ecr.describeRepositories());
            }
        });

        for (Region region : regions)
        {
            EcrClient ecrClient = getEcrClient(region);
            GetAuthorizationTokenResponse token = ecrClient.getAuthorizationToken();
            for (AuthorizationData authorizationData : token.authorizationData())
            {
                ParsedAuthorizationData parsedAuthData = AuthorizationDataParser.parse(authorizationData);
                System.out.println(parsedAuthData);
                String dockerLoginCommand = String.format("docker login --username AWS --password %s %s",
                                                          parsedAuthData.decodedAuthorizationToken,
                                                          parsedAuthData.registry);
                System.out.println("Docker login command: " + dockerLoginCommand);
                try
                {
                    CommandLineExecutor.execute(dockerLoginCommand, container.getEnvMap());
                    System.out.println("Docker login succeeded against " + authorizationData.proxyEndpoint());
                }
                catch (Exception e)
                {
                    System.out.println("Docker login failed against " + authorizationData.proxyEndpoint());
                }
            }
        }
    }

    @AfterClass
    public static void afterClass()
    {
        System.out.println("AfterClass");
        if (container != null)
        {
            System.out.println("Stopping LocalStack container");
            container.stop();
            System.out.println("LocalStack container stopped");
        }
    }

    private S3Client getS3Client(Region region)
    {
        return S3Client
                .builder()
                .endpointOverride(container.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        container.getAccessKey(), container.getSecretKey()
                )))
                .region(region)
                .build();
    }

    private EcrClient getEcrClient(Region region)
    {
        return EcrClient
                .builder()
                .endpointOverride(container.getEndpointOverride(ECR))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        container.getAccessKey(), container.getSecretKey()
                )))
                .region(region)
                .build();
    }
}
