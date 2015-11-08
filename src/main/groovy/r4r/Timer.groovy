package r4r

import com.google.common.base.Stopwatch

import static java.util.concurrent.TimeUnit.MILLISECONDS

class Timer {

    static Stopwatch stopwatch

    static start() {
        stopwatch = Stopwatch.createStarted()
    }

    static stop() {
        stopwatch.stop()
    }

    static logWithTime(String message) {
        long time = stopwatch.elapsed(MILLISECONDS)
        println "$message at $time"
    }
}
