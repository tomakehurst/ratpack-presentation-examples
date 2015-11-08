import com.google.common.base.Stopwatch
import r4r.MyAsyncFilmQuoteService
import ratpack.exec.Execution
import ratpack.exec.Promise

import static ratpack.groovy.Groovy.ratpack

ratpack {

    bindings {
        bind MyAsyncFilmQuoteService
    }

    handlers {


        files { dir "public" }
    }
}
