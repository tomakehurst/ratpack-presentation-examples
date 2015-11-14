package r4r

import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

class TestHarnessesSpec extends Specification {

    void setup() {
        Timer.start()
    }

    void cleanup() {
        Timer.stop()
    }

    void 'unit testing with ExecHarness'() {
        when:
            String actor = ExecHarness.yieldSingle {
                Promise<String> actor =
                        new MyAsyncFilmQuoteService().fetchActor()
                return actor
            }.valueOrThrow

        then:
            actor == 'Ralph Brown'

    }

    void 'embedded app test harness'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('hello-world') {
                        render 'Greetings!'
                    }
                }
            }.test { httpClient ->
                assert httpClient.get('hello-world').body.text == 'Greetings!'
            }

    }
}
