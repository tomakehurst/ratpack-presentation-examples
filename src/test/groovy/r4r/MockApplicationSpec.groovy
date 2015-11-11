package r4r

import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.Specification

class MockApplicationSpec extends Specification {

    GroovyRatpackMainApplicationUnderTest aut
    TestHttpClient client

    void setup() {
        aut = new GroovyRatpackMainApplicationUnderTest()
        client = aut.httpClient
    }

    void 'simple handler returns a message'() {
        when:
            client.get('simple-handler')

        then:
            client.response.body.text == 'Just this text'
    }
}
