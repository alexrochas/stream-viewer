package alex
import spark.Request
import spark.Response

import static spark.Spark.get

class StreamViewerMain {

    public static void main(String[] args) {
          get("/hello", {Request req, Response res -> "Hello!"});
    }
}
