package r4r

import ratpack.exec.Promise

interface AsyncFilmQuoteService {

    Promise<String> fetchQuote()
    Promise<String> fetchActor()
}

