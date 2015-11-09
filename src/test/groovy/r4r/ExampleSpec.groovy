package r4r

import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.rx.RxRatpack
import rx.Observable
import spock.lang.Specification

class ExampleSpec extends Specification {

    void setupSpec() {
        RxRatpack.initialize()
    }

    void setup() {
        Timer.start()
    }

    void cleanup() {
        Timer.stop()
    }

    void 'serial execution "immutable style"'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()
                        Promise<String> promisedQuote = quoteService.fetchQuote()
                        Promise<String> promisedActor = quoteService.fetchActor()

                        promisedQuote.flatMap { String quote ->
                            promisedActor.map { String actor ->
                                "$actor said \"$quote\""
                            }
                        }.then { attribution ->
                            ctx.response.send attribution
                        }
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text == 'Ralph Brown said \"I don\'t advise a haircut man. All hairdressers are in the employment of the government.\"'
            }

    }

    void 'serial execution "node style"'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()
                        Promise<String> promisedQuote = quoteService.fetchQuote()
                        Promise<String> promisedActor = quoteService.fetchActor()

                        String quote

                        promisedQuote.then {
                            quote = it
                        }

                        promisedActor.then { actor ->
                            if (actor && quote) {
                                ctx.response.send "$actor said \"$quote\""
                            }
                        }
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text == 'Ralph Brown said \"I don\'t advise a haircut man. All hairdressers are in the employment of the government.\"'
            }
    }

    void 'concurrent execution "node style"'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()

                        Promise<String> promisedQuote = quoteService.fetchQuote()
                        Promise<String> promisedActor = quoteService.fetchActor()

                        String quote
                        String actor

                        promisedQuote.then {
                            quote = it
                            if (actor && quote) {
                                ctx.response.send "$actor said \"$quote\""
                            }
                        }

                        Execution.current().fork().start {
                            promisedActor.then {
                                actor = it
                                if (actor && quote) {
                                    ctx.response.send "$actor said \"$quote\""
                                }
                            }
                        }
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text == 'Ralph Brown said \"I don\'t advise a haircut man. All hairdressers are in the employment of the government.\"'
            }
    }

    void 'serial execution using Rx'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()

                        Observable<String> promisedQuote = RxRatpack.observe(quoteService.fetchQuote())
                        Observable<String> promisedActor = RxRatpack.observe(quoteService.fetchActor())

                        Observable.combineLatest(promisedActor, promisedQuote, { actor, quote ->
                            "$actor said \"$quote\""
                        }).subscribe { attribution ->
                            ctx.response.send attribution
                        }
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text == 'Ralph Brown said \"I don\'t advise a haircut man. All hairdressers are in the employment of the government.\"'
            }
    }

    void 'concurrent execution "immutable style" with Rx'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()

                        Observable<String> promisedQuote = forkedObservable(quoteService.fetchQuote())
                        Observable<String> promisedActor = forkedObservable(quoteService.fetchActor())

                        Observable.combineLatest(promisedActor, promisedQuote, { actor, quote ->
                            "$actor said \"$quote\""
                        }).subscribe { attribution ->
                            ctx.response.send attribution
                        }
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text == 'Ralph Brown said \"I don\'t advise a haircut man. All hairdressers are in the employment of the government.\"'
            }
    }

    static <T> Observable<T> forkedObservable(Promise<T> promise) {
        RxRatpack.bindExec(Observable.create { subscriber ->
            Execution.fork().start {
                promise.then { value ->
                    try {
                        subscriber.onNext(value)
                        subscriber.onCompleted()
                    } catch (e) {
                        subscriber.onError(e)
                    }
                }
            }
        })
    }

}
