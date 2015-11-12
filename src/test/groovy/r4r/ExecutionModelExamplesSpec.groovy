package r4r

import ratpack.exec.Blocking
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.rx.RxRatpack
import ratpack.test.exec.ExecHarness
import rx.Observable
import spock.lang.Specification

class ExecutionModelExamplesSpec extends Specification {

    final String expectedQuoteAttribution = 'Ralph Brown said \"I don\'t advise a haircut man. All hairdressers are in the employment of the government.\"'

    AsyncFilmQuoteService quoteService = new FakeAsyncFilmQuoteService()

    void setupSpec() {
        RxRatpack.initialize()
    }

    void setup() {
        Timer.start()
    }

    void cleanup() {
        Timer.stop()
    }

    void 'execution model guarantee'() {
        expect:
            List<Integer> steps = []

            GroovyEmbeddedApp.of {
                handlers {
                    get {
                        steps.add 1
                        Blocking.get {
                            steps.add 2
                            return 'some stuff'
                        }.then { stuff ->
                            steps.add 3
                            render stuff
                        }

                        sleep 1000
                        steps.add 4
                    }
                }
            }.test { httpClient ->
                httpClient.get()
                assert steps == [1, 4, 2, 3]
            }
    }

    void 'serial execution "immutable style"'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
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
                assert httpClient.response.body.text == expectedQuoteAttribution
            }

    }

    void 'serial execution "node style"'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
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
                assert httpClient.response.body.text == expectedQuoteAttribution
            }
    }

    void 'concurrent execution "node style"'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
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
                assert httpClient.response.body.text == expectedQuoteAttribution
            }
    }

    void 'serial execution using Rx'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
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
                assert httpClient.response.body.text == expectedQuoteAttribution
            }
    }

    void 'concurrent execution "immutable style" with Rx'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('quotes') { ctx ->
                        Observable<String> promisedQuote = forkedObservable(quoteService.fetchQuote())
                        Observable<String> promisedActor = forkedObservable(quoteService.fetchActor())

                        RxRatpack.bindExec(Observable.combineLatest(
                                promisedActor, promisedQuote, { actor, quote ->
                            "$actor said \"$quote\""
                        })).subscribe { attribution ->
                            ctx.response.send attribution
                        }
                    }
                }
            }.test { httpClient ->
                httpClient.get('quotes')
                assert httpClient.response.statusCode == 200
                assert httpClient.response.body.text == expectedQuoteAttribution
            }
    }

    static <T> Observable<T> forkedObservable(Promise<T> promise) {
        Observable.create { subscriber ->
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
        }
    }


    void 'promises'() {
        expect:
            ExecHarness.runSingle {
                Promise<String> promisedMessage = Blocking.get {
                    '1...2...cough...3'
                }

                promisedMessage.map { message ->
                    "The message was: $message"
                }.then { text ->
                    println text
                }
            }
    }

    void 'Rx'() {
        expect:
            ExecHarness.runSingle {
                Promise<String> promisedMessage = Blocking.get {
                    '1...2...cough...3'
                }

                Observable<String> observableMessage = RxRatpack.observe(promisedMessage)

                observableMessage.map { message ->
                    "The message was: $message"
                }.subscribe { text ->
                    println text
                }
            }
    }



}
