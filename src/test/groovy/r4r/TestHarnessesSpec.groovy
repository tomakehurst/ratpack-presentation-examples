package r4r

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
                new FakeAsyncFilmQuoteService().fetchActor()
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
