import java.util.Arrays;

public class ParsedAuthorizationData
{
    public final String registry;
    public final String decodedAuthorizationToken;

    public ParsedAuthorizationData(String registry, byte[] decodedAuthorizationToken)
    {
        this.registry = registry;
        this.decodedAuthorizationToken = new String(decodedAuthorizationToken);
    }

    @Override
    public String toString()
    {
        return "ParsedAuthorizationData{" +
                "registry='" + registry + '\'' +
                ", decodedAuthorizationToken=" + decodedAuthorizationToken +
                '}';
    }
}
