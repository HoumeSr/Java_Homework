
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

// curl.exe -X GET http://localhost:8000/login
// curl.exe -X GET -H "login: b4c0dd77-ddd2-4096-8ba9-744dd3935f9c" http://localhost:8000/image --output "output.png"
// curl.exe -X POST -H "login: f798b850-1f57-4d05-b627-cdec148236f8" -d "rock" http://localhost:8000/game
// curl.exe -X DELETE -H "login: ab38150e-5703-4ff8-b4d4-68e9226fd550" "http://localhost:8000/delete?path=" 
public class server {

    private static final HashSet<String> tokens = new HashSet<>();
    private static final String image_path = "C:/Users/Houme/Desktop/Neskvic.png";
    private static final Map<String, Integer> gameMap = Map.of(
            "rock", 0, "paper", 1, "scissors", 2
    );

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/login", new LoginHandler());
        server.createContext("/image", new ImageHandler());
        server.createContext("/game", new GameHandler());
        server.createContext("/delete", new FileDeleteHandler());

        server.start();

        System.err.println("Сервер был запущен");
    }

    public static String getImage_path() {
        return image_path;
    }

    static class LoginHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendError(exchange);
                return;
            }
            String response = generateUniqueToken();
            sendResponse(exchange, response);
        }
    }

    static class ImageHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET") || !checkToken(exchange)) {
                sendError(exchange);
                return;
            }
            File file = new File(image_path);
            if (!file.exists() || !file.isFile()) {
                System.out.println("NO file");
            }
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class GameHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST") || !checkToken(exchange)) {
                sendError(exchange);
                return;
            }
            String requestBody;
            InputStream is = exchange.getRequestBody();
            requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim().toLowerCase();
            is.close();

            Integer choice = gameMap.get(requestBody);
            if (choice == null) {
                sendResponse(exchange, "Wrong choice");
                return;
            }
            int gameChoice = (new Random()).nextInt(3);

            String response;
            int diff = choice - gameChoice;

            switch (diff) {
                case 0:
                    response = "draw";
                    break;
                case 1:
                case -2:
                    response = "you win";
                    break;
                default:
                    response = "you lose";
                    break;
            }
            sendResponse(exchange, response);
        }
    }

    static class FileDeleteHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("DELETE") || !checkToken(exchange)) {
                sendError(exchange);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParameters(query);
            String filePath = params.get("path");
            if (filePath == null || filePath.isEmpty()) {
                sendError(exchange);
                return;
            }
            try {
                Files.deleteIfExists(Paths.get("").toAbsolutePath().resolve(filePath));
            } catch (Exception e) {
                sendError(exchange);
            }
        }
    }

    private static void sendError(HttpExchange exchange) throws IOException {
        String response = "Something went wrong";
        exchange.sendResponseHeaders(403, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (tokens.contains(token));
        return token;
    }

    private static boolean checkToken(HttpExchange exchange) {
        return tokens.contains(exchange.getRequestHeaders().getFirst("token"));
    }

    private static Map<String, String> parseQueryParameters(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }
}
