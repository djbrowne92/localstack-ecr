import software.amazon.awssdk.services.ecr.model.AuthorizationData;

import java.util.Base64;

public class AuthorizationDataParser
{
    public static ParsedAuthorizationData parse(AuthorizationData authorizationData)
    {
        System.out.println("AuthorizationData: " + authorizationData);
        String registry = authorizationData.proxyEndpoint().replace("https://", "");

        byte[] decodedAuthorizationToken = new String(
                Base64.getDecoder()
                        .decode(authorizationData.authorizationToken().getBytes()))
                .replaceAll("^AWS:", "")
                .getBytes();

        return new ParsedAuthorizationData(registry, decodedAuthorizationToken);
    }
}
