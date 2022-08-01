import org.gradle.api.Project;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecException;
import org.gradle.testfixtures.ProjectBuilder;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class CommandLineExecutor
{
    public static ExecResult execute(String command,
                              Map<String, String> environment) throws TimeoutException
    {
        List<String> commandLine = Arrays.asList("sh", "-x", "-e", "-c", command);

        System.out.printf("Running command: %s%n", commandLine);
        System.err.printf("Running command: %s%n", commandLine);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ExecResult> future = executor.submit(() ->
                newGradleProject().exec(execSpec -> {
                            execSpec.commandLine(commandLine)
                                    .setIgnoreExitValue(true /* We check it below */)
                                    .environment(environment);
                            execSpec.workingDir(System.getProperty("user.dir"));
                        }
                )
        );

        try
        {
            ExecResult execResult = future.get(3600L, TimeUnit.SECONDS);
            execResult.assertNormalExitValue();
            System.out.printf("Command returned successfully: %s%n", commandLine);
            return execResult;
        }
        catch (TimeoutException e)
        {
            future.cancel(true);
            throw e;
        }
        catch (InterruptedException | ExecutionException | ExecException e)
        {
            String failureMessage = String.format(
                    "Failed executing command '%s'. Error: %s%n",
                    commandLine.toString(),
                    e.toString()
            );
            System.out.println(failureMessage);
            throw new RuntimeException(failureMessage, e);
        }
    }

    private static Project newGradleProject()
    {
        return ProjectBuilder.builder().build();
    }
}
