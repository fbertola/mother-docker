package org.github.fbertola.motherdocker.utils

import com.spotify.docker.client.LogMessage
import com.spotify.docker.client.LogStream
import groovy.util.logging.Slf4j

import static java.util.concurrent.CompletableFuture.supplyAsync
import static java.util.concurrent.TimeUnit.MINUTES

@Slf4j
class DockerUtils {

    public static def waitForLogMessage(LogStream logStream, message, waitTime = 1, timeUnit = MINUTES) {
        return supplyAsync {
            def messageFound = false

            while (!messageFound && logStream.hasNext()) {
                def logMessage = logStream.next()
                messageFound = logContainsMessage(logMessage, message)
            }

            return messageFound
        }
    }

    private static def logContainsMessage(LogMessage logMessage, String message) {
        def content = logMessage.content()
        def buf = new byte[content.capacity()]

        content.get(buf, 0, buf.length)

        try {
            def messageString = new String(buf, 'utf-8')
            return messageString.contains(message)
        } catch (final Exception e) {
            log.error('Error while trying to read a log message', e);
        }

        return false;
    }


}
