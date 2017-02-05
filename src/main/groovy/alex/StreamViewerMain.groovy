package alex

import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.buffer.Buffer
import io.vertx.rxjava.core.http.HttpServerResponse
import io.vertx.rxjava.core.http.ServerWebSocket
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler
import rx.Observable

class StreamViewerMain {

    public static void main(String[] args) {
        ArrayList<ServerWebSocket> sockets = []
        def vertx = Vertx.vertx()

        Router router = Router.router(vertx)
        router.route("/").handler({routingContext ->
            HttpServerResponse response = routingContext.response();
            if (routingContext.request().uri() == "/") response.sendFile 'index.html'
        })

        router.route(HttpMethod.POST, "/ping/*").handler({
            routingContext ->
                sockets
                        .stream()
                        .forEach({
                            s -> s.writeFinalTextFrame("pong")
                        })
                HttpServerResponse response = routingContext.response();
                response.setStatusCode(200).end()
        })

        def options = [
                inboundPermitteds:[[addressRegex:".+"]],
                outboundPermitteds:[[addressRegex:".+"]]
        ]
        def handler = SockJSHandler.create(vertx).bridge(new BridgeOptions(new JsonObject(options)))
        router.route("/eventbus/*").handler(handler)

        def server = vertx.createHttpServer()
                .requestHandler(
                {req ->
                    router.accept(req)
                })

        Observable<ServerWebSocket> socketObservable = server.websocketStream().toObservable()
        socketObservable.subscribe(
                { socket ->
                    println("Web socket connect")
                    Observable<Buffer> dataObs = socket.toObservable()
                    sockets.add(socket)
                    dataObs.subscribe({ buffer ->
                        socket.write(buffer)
                        println("Got message ${buffer.toString("UTF-8")}")
                    })
                },
                { failure -> println("Should never be called") },
                { println("Subscription ended or server closed") }
        )

        server.listen(8080)
    }
}
