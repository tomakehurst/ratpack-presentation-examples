package r4r

import ratpack.handling.Context
import ratpack.handling.Handler

class SecurityCheckHandler implements Handler {

    @Override
    void handle(Context ctx) throws Exception {
        if (ctx.request.headers.get('Authorization') == 'super_secret') {
            ctx.next()
        } else {
            ctx.response.status(401).send()
        }
    }
}
