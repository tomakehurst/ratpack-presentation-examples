import r4r.FakeAsyncFilmQuoteService

import static ratpack.groovy.Groovy.ratpack

ratpack {

    bindings {
        bind FakeAsyncFilmQuoteService
    }

    handlers {


        files { dir "public" }
    }
}
