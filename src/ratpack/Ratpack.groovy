import r4r.SecurityCheckHandler

import static ratpack.groovy.Groovy.ratpack

ratpack {

    bindings {
        bind SecurityCheckHandler
    }

    handlers {
        get('simple-handler') {
            render "Just this text"
        }

        get('secured-feature', chain { chain ->
            get(SecurityCheckHandler)
            get {
                render "You got through"
            }
        })

        files { dir "public" }
    }
}
