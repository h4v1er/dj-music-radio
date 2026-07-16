package org.example.chat.service;

import java.net.URI;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherService {

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("100", "☀️"),
            Map.entry("101", "🌤️"),
            Map.entry("102", "⛅"),
            Map.entry("103", "🌥️"),
            Map.entry("104", "☁️"),
            Map.entry("300", "🌦️"),
            Map.entry("301", "🌧️"),
            Map.entry("302", "⛈️"),
            Map.entry("305", "🌧️"),
            Map.entry("306", "🌧️"),
            Map.entry("307", "🌧️"),
            Map.entry("400", "🌨️"),
            Map.entry("401", "🌨️"),
            Map.entry("402", "❄️"),
            Map.entry("500", "🌫️"),
            Map.entry("501", "🌫️"),
            Map.entry("502", "🌫️")
    );

    private final RestClient restClient;
    private final String apiKey;
    private final String weatherUrl;
    private final String geoUrl;

    public WeatherService(
            RestClient.Builder restClientBuilder,
            @Value("${weather.api-key:}") String apiKey,
            @Value("${weather.weather-url}") String weatherUrl,
            @Value("${weather.geo-url}") String geoUrl) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.weatherUrl = weatherUrl;
        this.geoUrl = geoUrl;
    }

    public WeatherResponse weather(String city) {
        String safeCity = normalizeCity(city);
        if (!hasApiKey()) {
            return demoWeather(safeCity);
        }

        try {
            GeoResponse geo = restClient.get()
                    .uri(geoUri(safeCity))
                    .header("X-QW-Api-Key", apiKey)
                    .retrieve()
                    .body(GeoResponse.class);
            if (geo == null || geo.location() == null || geo.location().isEmpty()) {
                return demoWeather(safeCity);
            }

            GeoLocation location = geo.location().get(0);
            WeatherNowResponse nowResponse = restClient.get()
                    .uri(weatherUri(location.id()))
                    .header("X-QW-Api-Key", apiKey)
                    .retrieve()
                    .body(WeatherNowResponse.class);

            if (nowResponse == null || nowResponse.now() == null) {
                return demoWeather(safeCity);
            }

            WeatherNow now = nowResponse.now();
            return new WeatherResponse(
                    displayCity(location),
                    iconFor(now.icon()),
                    now.temp() + "°",
                    now.text(),
                    greeting(),
                    "real",
                    now.obsTime()
            );
        } catch (Exception e) {
            return demoWeather(safeCity);
        }
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(apiKey) && !"your_api_key_here".equals(apiKey);
    }

    private URI geoUri(String city) {
        return UriComponentsBuilder.fromUriString(geoUrl)
                .queryParam("location", city)
                .queryParam("range", "cn")
                .queryParam("number", 1)
                .queryParam("lang", "zh")
                .build()
                .toUri();
    }

    private URI weatherUri(String locationId) {
        return UriComponentsBuilder.fromUriString(weatherUrl)
                .queryParam("location", locationId)
                .queryParam("lang", "zh")
                .build()
                .toUri();
    }

    private static String displayCity(GeoLocation location) {
        if (StringUtils.hasText(location.adm1()) && !location.adm1().equals(location.name())) {
            return location.adm1() + " " + location.name();
        }
        return location.name();
    }

    private static String iconFor(String code) {
        return ICONS.getOrDefault(code, "🌡️");
    }

    private static WeatherResponse demoWeather(String city) {
        WeatherSnapshot snapshot = demoSnapshot(city);
        return new WeatherResponse(city, snapshot.icon(), snapshot.temp(), snapshot.text(), greeting(), "demo", "");
    }

    private static WeatherSnapshot demoSnapshot(String city) {
        if (city.contains("上海")) {
            return new WeatherSnapshot("🌦️", "30°", "多云");
        }
        if (city.contains("广州") || city.contains("深圳")) {
            return new WeatherSnapshot("🌧️", "31°", "阵雨");
        }
        if (city.contains("成都")) {
            return new WeatherSnapshot("🌥️", "27°", "阴");
        }
        return new WeatherSnapshot("☀️", "28°", "晴");
    }

    private static String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            return "北京";
        }
        return city.trim();
    }

    private static String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 6) {
            return "夜深了，来点轻一点的音乐？";
        }
        if (hour < 12) {
            return "早上好，来点有活力的音乐？";
        }
        if (hour < 18) {
            return "下午好，想听点什么？";
        }
        return "晚上好，今天想用什么歌收尾？";
    }

    public record WeatherResponse(
            String city,
            String icon,
            String temp,
            String text,
            String greeting,
            String source,
            String obsTime) {
    }

    private record WeatherSnapshot(String icon, String temp, String text) {
    }

    private record GeoResponse(String code, List<GeoLocation> location) {
    }

    private record GeoLocation(String name, String id, String adm1) {
    }

    private record WeatherNowResponse(String code, WeatherNow now) {
    }

    private record WeatherNow(String obsTime, String temp, String icon, String text) {
    }
}
