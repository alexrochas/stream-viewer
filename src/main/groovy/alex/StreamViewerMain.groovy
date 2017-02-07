package alex

import spark.Spark

class StreamViewerMain {

    public static void main(String[] args) {
        Spark.staticFileLocation("/public") //index.html is served at localhost:4567 (default port)
        Spark.webSocket("/eventbus", EchoWebSocket.class)
        Spark.post("/ping", {req, res ->
            EchoWebSocket.getSessions().stream().filter({s -> s.open}).forEach({ session ->
                session.getRemote().sendString("pong")
            })
            res.status(200)
            res.type("text/event-sourcing")
        })
    }
}
