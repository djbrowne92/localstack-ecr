import java.util.Arrays;

public class ParsedAuthorizationData
{
    public final String registry;
    public final byte[] decodedAuthorizationToken;

    public ParsedAuthorizationData(String registry, byte[] decodedAuthorizationToken)
    {
        this.registry = registry;
        this.decodedAuthorizationToken = decodedAuthorizationToken;
    }

    @Override
    public String toString()
    {
        return "ParsedAuthorizationData{" +
                "registry='" + registry + '\'' +
                ", decodedAuthorizationToken=" + Arrays.toString(decodedAuthorizationToken) +
                '}';
    }
}
