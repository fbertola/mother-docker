package org.github.fbertola.motherdocker.utils

import com.github.rholder.retry.RetryerBuilder
import com.google.common.base.Predicate
import com.spotify.docker.client.*
import com.spotify.docker.client.messages.ProgressMessage
import groovy.util.logging.Slf4j

import java.util.concurrent.TimeUnit

import static com.github.rholder.retry.BlockStrategies.threadSleepStrategy
import static com.github.rholder.retry.StopStrategies.neverStop
import static com.github.rholder.retry.WaitStrategies.fixedWait
import static java.util.concurrent.CompletableFuture.supplyAsync
import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
class DockerUtils {

    public static
    def waitForExecFuture(DockerClient client, String execId, Long pauseBetweenRetries = 5, TimeUnit timeUnit = SECONDS) {
        def retryer = RetryerBuilder.newBuilder()
                .retryIfResult({ !it } as Predicate)
                .withBlockStrategy(threadSleepStrategy())
                .withWaitStrategy(fixedWait(pauseBetweenRetries, timeUnit))
                .withStopStrategy(neverStop())
                .build()

        return supplyAsync {
            retryer.call {
                def execStatus = client.execInspect(execId)
                return (!execStatus.running() && execStatus.exitCode() == 0)
            }
        }
    }

    public static def waitForLogMessageFuture(LogStream logStream, String message) {
        return supplyAsync {
            def messageFound = false

            while (!messageFound && logStream.hasNext()) {
                def logMessage = logStream.next()
                messageFound = logContainsMessage(logMessage, message)
            }

            return messageFound
        }
    }

    public static ProgressHandler progressHandler() {
        //if (log.isDebugEnabled()) {
            return new AnsiProgressHandler(System.err)
        /*} else {
            return new ProgressHandler() {

                @Override
                void progress(ProgressMessage message) throws DockerException {
                    // NO-OP
                }

            }
        }*/
    }

    private static def logContainsMessage(LogMessage logMessage, String message) {
        def content = logMessage.content()
        def buf = new byte[content.capacity()]

        content.get(buf, 0, buf.length)

        try {
            def messageString = new String(buf, 'utf-8')

            log.debug(messageString)

            return messageString.contains(message)
        } catch (final Exception e) {
            log.error('Error while trying to read a log message', e);
        }

        return false;
    }


}
