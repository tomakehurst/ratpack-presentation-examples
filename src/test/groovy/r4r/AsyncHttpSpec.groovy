package r4r

import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.handling.Context
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import spock.lang.Specification

class AsyncHttpSpec extends Specification {

    void 'async HTTP example'() {
        expect:
            GroovyEmbeddedApp.of {
                handlers {
                    get('things-from-the-web') { HttpClient client, Context ctx ->
                        Promise<ReceivedResponse> googlePromise =
                            client.get('http://www.google.com'.toURI())
                        googlePromise.map { googleResponse ->
                            googleResponse.body.text
                        }.flatMap { googleBody ->
                            Promise<ReceivedResponse> twitterPromise =
                                client.get('http://www.twitter.com'.toURI())
                            twitterPromise.map { twitterResponse ->
                                twitterResponse.body.text + googleBody
                            }
                        }.then { text ->
                            ctx.response.send(text)
                        }
                    }
                }
            }.test { testHttpClient ->
                testHttpClient.get('things-from-the-web')
                assert testHttpClient.response.statusCode == 200
                assert testHttpClient.response.body.text.contains('Google')
                assert testHttpClient.response.body.text.contains('Twitter')
            }
    }
}


