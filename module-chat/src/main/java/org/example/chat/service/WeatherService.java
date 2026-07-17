package org.example.chat.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

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
    private final ObjectMapper objectMapper;

    public WeatherService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${weather.api-key:}") String apiKey,
            @Value("${weather.api-host:}") String apiHost,
            @Value("${weather.weather-url}") String weatherUrl,
            @Value("${weather.geo-url}") String geoUrl) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.weatherUrl = resolveUrl(apiHost, weatherUrl, "/v7/weather/now");
        this.geoUrl = resolveUrl(apiHost, geoUrl, "/geo/v2/city/lookup");
    }

    public WeatherResponse weather(String city) {
        String safeCity = normalizeCity(city);
        if (!hasApiKey()) {
            return demoWeather(safeCity, "未配置 QWEATHER_API_KEY，当前使用演示天气");
        }

        try {
            GeoResponse geo = getJson(geoUri(safeCity), GeoResponse.class);
            if (geo == null) {
                return demoWeather(safeCity, "城市查询接口无响应，当前使用演示天气");
            }
            if (!"200".equals(geo.code())) {
                return demoWeather(safeCity, "城市查询失败，和风天气 code=" + geo.code());
            }
            if (geo.location() == null || geo.location().isEmpty()) {
                return demoWeather(safeCity, "未找到城市「" + safeCity + "」，当前使用演示天气");
            }

            GeoLocation location = geo.location().get(0);
            WeatherNowResponse nowResponse = getJson(weatherUri(location.id()), WeatherNowResponse.class);

            if (nowResponse == null) {
                return demoWeather(safeCity, "实时天气接口无响应，当前使用演示天气");
            }
            if (!"200".equals(nowResponse.code())) {
                return demoWeather(safeCity, "实时天气查询失败，和风天气 code=" + nowResponse.code());
            }
            if (nowResponse.now() == null) {
                return demoWeather(safeCity, "实时天气结果为空，当前使用演示天气");
            }

            WeatherNow now = nowResponse.now();
            return new WeatherResponse(
                    displayCity(location),
                    iconFor(now.icon()),
                    now.temp() + "°",
                    now.text(),
                    greeting(),
                    "real",
                    now.obsTime(),
                    "和风天气实时数据",
                    degree(now.feelsLike()),
                    now.windDir(),
                    now.windScale(),
                    now.windSpeed(),
                    now.humidity(),
                    now.precip(),
                    now.pressure(),
                    now.vis(),
                    now.cloud(),
                    now.dew(),
                    nowResponse.updateTime(),
                    nowResponse.fxLink()
            );
        } catch (Exception e) {
            log.warn("QWeather request failed for city={}, fallback to demo weather", safeCity, e);
            return demoWeather(safeCity, "天气接口调用异常，当前使用演示天气");
        }
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(apiKey) && !"your_api_key_here".equals(apiKey);
    }

    private <T> T getJson(URI uri, Class<T> responseType) throws IOException {
        byte[] body = restClient.get()
                .uri(uri)
                .header("X-QW-Api-Key", apiKey)
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip, identity")
                .retrieve()
                .body(byte[].class);
        if (body == null || body.length == 0) {
            return null;
        }
        byte[] jsonBytes = isGzip(body) ? unzip(body) : body;
        return objectMapper.readValue(new String(jsonBytes, StandardCharsets.UTF_8), responseType);
    }

    private static boolean isGzip(byte[] body) {
        return body.length >= 2 && (body[0] & 0xff) == 0x1f && (body[1] & 0xff) == 0x8b;
    }

    private static byte[] unzip(byte[] body) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
            return gzip.readAllBytes();
        }
    }

    private static String resolveUrl(String apiHost, String fallbackUrl, String path) {
        if (!StringUtils.hasText(apiHost)) {
            return fallbackUrl;
        }
        String host = trimTrailingSlash(apiHost.trim());
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "https://" + host;
        }
        return host + path;
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
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

    private static String degree(String value) {
        return StringUtils.hasText(value) ? value + "°" : "";
    }

    private static WeatherResponse demoWeather(String city, String message) {
        WeatherSnapshot snapshot = demoSnapshot(city);
        return new WeatherResponse(
                city,
                snapshot.icon(),
                snapshot.temp(),
                snapshot.text(),
                greeting(),
                "demo",
                "",
                message,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );
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
            String obsTime,
            String message,
            String feelsLike,
            String windDir,
            String windScale,
            String windSpeed,
            String humidity,
            String precip,
            String pressure,
            String vis,
            String cloud,
            String dew,
            String updateTime,
            String fxLink) {
    }

    private record WeatherSnapshot(String icon, String temp, String text) {
    }

    private record GeoResponse(String code, List<GeoLocation> location) {
    }

    private record GeoLocation(String name, String id, String adm1) {
    }

    private record WeatherNowResponse(String code, String updateTime, String fxLink, WeatherNow now) {
    }

    private record WeatherNow(
            String obsTime,
            String temp,
            String feelsLike,
            String icon,
            String text,
            String windDir,
            String windScale,
            String windSpeed,
            String humidity,
            String precip,
            String pressure,
            String vis,
            String cloud,
            String dew) {
    }
}
