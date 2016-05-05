package r4r

import com.google.inject.Binder
import ratpack.groovy.Groovy
import ratpack.test.MainClassApplicationUnderTest
import spock.lang.Specification

import javax.inject.Inject

class GuiceExampleSpec extends Specification {

    MainClassApplicationUnderTest aut = new MainClassApplicationUnderTest(MyGuiceyApp)

    void 'guice example'() {
        expect:
            aut.httpClient.put('my-stuff')
    }

}

class MyGuiceyApp {
    public static void main(String[] args) {
        Groovy.ratpack {
            bindings {
                bind(MyThing1)
                binder { Binder binder ->
                    binder.bind(MyThingInterface).to(MyThing3)
                }

                multiBind(MyThing3)
            }

            handlers {
                put('my-stuff') { MyThing1 one, MyThing2 two ->
                    MyThing3 three = get(MyThing3)
                    println "I have $one, $two, $three"
                    response.status(200).send()
                }
            }
        }
    }
}

class MyThing1 {}

class MyThing2 {
    private final MyThing1 myThing1

    @Inject
    MyThing2(MyThing1 myThing1) {
        this.myThing1 = myThing1
    }
}

interface MyThingInterface {}
class MyThing3 implements MyThingInterface {}
