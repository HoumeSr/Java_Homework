
import java.net.URI;
import java.net.http.*;

public class api {

    public static void main(String[] args) {
        String access_key = "e2e6780b-69a4-482f-b2f1-f1de2e11e718";

        String url = "https://api.weather.yandex.ru/v2/forecast?lat=55.7887&lon=49.1221";
        HttpClient cl = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("X-Yandex-API-Key", access_key).GET().build();
        try {
            HttpResponse<String> response = cl.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(extractValue(response.body(), "\"temp\":", ","));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractValue(String json, String startMarker, String endMarker) {
        int startIndex = json.indexOf(startMarker);
        if (startIndex == -1) {
            return "N/A";
        }

        startIndex += startMarker.length();
        int endIndex = json.indexOf(endMarker, startIndex);
        if (endIndex == -1) {
            endIndex = json.length();
        }

        return json.substring(startIndex, endIndex);
    }
}
