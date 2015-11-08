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

    void 'serial execution order'() {
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
                assert httpClient.response.body.text.contains('Richard E. Grant said "I wouldn\'t advise a haircut')
            }

    }

    void '1st (failed) attempt at parallel execution order'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()
                        Promise<String> promisedQuote = quoteService.fetchQuote()
                        Promise<String> promisedActor = quoteService.fetchActor()

                        String quote
                        String actor

                        println '1'
                        promisedQuote.then {
                            println '2'
                            quote = it
                            if (actor && quote) {
                                ctx.response.send "$actor said \"$quote\""
                            }
                        }

                        promisedActor.then {
                            println '3'
                            actor = it
                            if (actor && quote) {
                                ctx.response.send "$actor said \"$quote\""
                            }
                        }

                        println '4'
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text.contains('Richard E. Grant said "I wouldn\'t advise a haircut')
            }
    }

    void '2nd (successful) attempt at parallel execution order'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()

                        Promise<String> promisedQuote = quoteService.fetchQuote()
                        Promise<String> promisedActor = quoteService.fetchActor()

                        String quote
                        String actor

                        println '1'
                        promisedQuote.then {
                            println '2'
                            quote = it
                            if (actor && quote) {
                                ctx.response.send "$actor said \"$quote\""
                            }
                        }

                        Execution.current().fork().start {
                            promisedActor.then {
                                println '3'
                                actor = it
                                if (actor && quote) {
                                    ctx.response.send "$actor said \"$quote\""
                                }
                            }
                        }

                        println '4'
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text.contains('Richard E. Grant said "I wouldn\'t advise a haircut')
            }
    }

    void '3rd (failed) attempt at parallel execution order using Rx'() {
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
                assert httpClient.response.body.text.contains('Richard E. Grant said "I wouldn\'t advise a haircut')
            }
    }

    void '4th (successful) attempt at parallel execution order using Rx'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        MyAsyncFilmQuoteService quoteService = new MyAsyncFilmQuoteService()

                        Observable<String> promisedQuote = RxRatpack.bindExec(RxRatpack.observe(quoteService.fetchQuote()))
                        Observable<String> promisedActor = RxRatpack.bindExec(RxRatpack.observe(quoteService.fetchActor()))

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
                assert httpClient.response.body.text.contains('Richard E. Grant said "I wouldn\'t advise a haircut')
            }
    }
}
