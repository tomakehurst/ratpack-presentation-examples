package r4r

import ratpack.exec.Blocking
import ratpack.exec.Promise

import static r4r.Timer.logWithTime

class MyAsyncFilmQuoteService {

    Promise<String> fetchQuote() {
        Blocking.get {
            logWithTime "Start fetching quote"
            Thread.sleep(200)
            logWithTime "Finish fetching quote"
            "I wouldn't advise a haircut man. Hairdressers are in the employment of the government."
        }
    }

    Promise<String> fetchActor() {
        Blocking.get {
            logWithTime "Start fetching actor"
            Thread.sleep(100)
            logWithTime "Finish fetching actor"
            "Richard E. Grant"
        }
    }
}
