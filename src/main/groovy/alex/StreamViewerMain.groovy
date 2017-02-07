package alex
import groovy.util.logging.Slf4j
import rx.subjects.PublishSubject
import spark.Request
import spark.Response
import spark.Spark

@Slf4j
class StreamViewerMain {

    public static void main(String[] args) {
        Spark.staticFileLocation("/public") //index.html is served at localhost:4567 (default port)
        Spark.webSocket("/eventbus", EchoWebSocket.class)

        Spark.get("/node/*", {req, res ->
            String path = req.pathInfo().replaceFirst("/node/", "node_modules/");
            return serveFile(path,req,res)
        })

        Spark.post("/ping", {req, res ->
            EchoWebSocket.getSessions().stream().filter({s -> s.open}).forEach({ session ->
                session.getRemote().sendString("${new Date().format('HH:mm:ss')} - Pong")
            })
            res.status(200)
        })

        PublishSubject pongSubject = PublishSubject.create()

        new Timer().schedule({
            pongSubject.onNext("You received a new message from Websocket!")
        } as TimerTask, 1000, 3000) //magic numbers are initial-delay & repeat-interval

        pongSubject.subscribe({event ->
            EchoWebSocket.getSessions().stream().filter({s -> s.open}).forEach({ session ->
                session.getRemote().sendString("${new Date().format('HH:mm:ss')} - ${event}")
            })
        })
    }

    private static serveFile(String path, Request req, Response res){

        //InputStream inputStream = getClass().getResourceAsStream(path)
        def file = new File(path)
        if (!file.exists()){
            log.warn("File not found: " + path)
            return null
        }
        InputStream inputStream = new FileInputStream(file)

        if (inputStream != null) {
            res.status(200)

            byte[] buf = new byte[1024]
            OutputStream os = res.raw().getOutputStream()
            OutputStreamWriter outWriter = new OutputStreamWriter(os)
            int count = 0
            while ((count = inputStream.read(buf)) >= 0) {
                os.write(buf, 0, count)
            }
            inputStream.close()
            outWriter.close()

            return ""
        }
        return null
    }

}
