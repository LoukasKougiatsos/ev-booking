package com.evbooking.server.discovery.service;

import com.evbooking.server.discovery.dto.StationDiscoveryItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class StationDiscoveryService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ev.api-base-url:https://api.api-ninjas.com/v1/evcharger}")
    private String apiBaseUrl;

    @Value("${app.ev.api-key:}")
    private String apiKey;

    public List<StationDiscoveryItem> search(Double latitude, Double longitude, Integer radius, Integer limit) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "EV API key is not configured on the server.");
        }

        double lat = latitude != null ? latitude : 37.7749;
        double lon = longitude != null ? longitude : -122.4194;

        try {
            int distanceKm = sanitizeDistanceKm(radius);
            int maxItems = sanitizeLimit(limit);

            // Collect from center and nearby offsets to increase map coverage.
            List<double[]> points = searchPoints(lat, lon, distanceKm);
            Map<String, StationDiscoveryItem> uniqueStations = new LinkedHashMap<>();

            for (double[] point : points) {
                List<?> rawItems = fetchRawStations(point[0], point[1], distanceKm);
                for (Object item : rawItems) {
                    if (item instanceof Map<?, ?> rawMap) {
                        StationDiscoveryItem station = mapToStation(rawMap);
                        String key = stationKey(station);
                        if (!uniqueStations.containsKey(key)) {
                            uniqueStations.put(key, station);
                        }
                    }
                }
            }

            List<StationDiscoveryItem> stations = new ArrayList<>(uniqueStations.values());
            if (stations.size() > maxItems) {
                return stations.subList(0, maxItems);
            }
            return stations;
        } catch (HttpStatusCodeException ex) {
            String upstream = ex.getResponseBodyAsString();
            String message = StringUtils.hasText(upstream)
                    ? "Upstream EV API error: " + upstream
                    : "Upstream EV API request failed with status " + ex.getStatusCode().value();
            throw new ResponseStatusException(BAD_GATEWAY, message, ex);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to fetch charging stations from upstream API.", ex);
        }
    }

    private StationDiscoveryItem mapToStation(Map<?, ?> raw) {
        String id = firstNonBlank(
                asString(raw.get("id")),
                asString(raw.get("station_id")),
                asString(raw.get("station_name")),
                asString(raw.get("name"))
        );

        String name = firstNonBlank(
                asString(raw.get("name")),
                asString(raw.get("station_name")),
                asString(raw.get("address")),
                "Charging Station"
        );

        String city = asString(raw.get("city"));
        String region = firstNonBlank(asString(raw.get("region")), asString(raw.get("state")));
        String country = asString(raw.get("country"));
        String address = buildAddress(raw);

        Double latitude = asDouble(raw.get("latitude"));
        Double longitude = asDouble(raw.get("longitude"));
        Boolean active = asBoolean(raw.get("is_active"));

        ConnectionSummary connectionSummary = parseConnections(raw.get("connections"));

        String connector = firstNonBlank(connectionSummary.connectorName(), "Unknown");
        Integer powerKw = connectionSummary.maxPowerKw();
        Integer level = connectionSummary.level();
        Integer total = connectionSummary.totalConnectors();
        Integer available = null;

        return new StationDiscoveryItem(
                id,
                name,
                address,
                city,
                region,
                country,
                latitude,
                longitude,
                connector,
                powerKw,
                level,
                available,
                total,
                active
        );
    }

    private ConnectionSummary parseConnections(Object rawConnections) {
        if (!(rawConnections instanceof List<?> connections) || connections.isEmpty()) {
            return new ConnectionSummary(null, null, null, null);
        }

        String connectorName = null;
        Integer level = null;
        Integer maxPowerKw = null;
        int total = 0;

        for (Object connectionObj : connections) {
            if (!(connectionObj instanceof Map<?, ?> connection)) {
                continue;
            }

            if (!StringUtils.hasText(connectorName)) {
                connectorName = firstNonBlank(
                        asString(connection.get("type_name")),
                        asString(connection.get("type_official"))
                );
            }

            if (level == null) {
                level = asInteger(connection.get("level"));
            }

            Integer power = firstNonNull(
                    asInteger(connection.get("power_kw")),
                    asInteger(connection.get("max_electric_power")),
                    asInteger(connection.get("max_power_kw")),
                    asInteger(connection.get("kw"))
            );
            if (power != null && power > 0) {
                maxPowerKw = maxPowerKw == null ? power : Math.max(maxPowerKw, power);
            }

            Integer num = asInteger(connection.get("num_connectors"));
            if (num != null && num > 0) {
                total += num;
            }
        }

        return new ConnectionSummary(connectorName, level, total > 0 ? total : null, maxPowerKw);
    }

    private String buildAddress(Map<?, ?> raw) {
        String primaryAddress = asString(raw.get("address"));
        if (StringUtils.hasText(primaryAddress)) {
            return primaryAddress;
        }

        String street = asString(raw.get("street_address"));
        String city = asString(raw.get("city"));
        String region = firstNonBlank(asString(raw.get("region")), asString(raw.get("state")));
        String zip = asString(raw.get("zip"));

        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(street)) parts.add(street);
        if (StringUtils.hasText(city)) parts.add(city);
        if (StringUtils.hasText(region)) parts.add(region);
        if (StringUtils.hasText(zip)) parts.add(zip);

        return parts.isEmpty() ? "Address not available" : String.join(", ", parts);
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) return 60;
        return Math.max(1, Math.min(limit, 100));
    }

    private int sanitizeDistanceKm(Integer radius) {
        if (radius == null) return 25;
        return Math.max(2, Math.min(radius, 100));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double asDouble(Object value) {
        if (value == null) return null;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer asInteger(Object value) {
        if (value == null) return null;
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean asBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<?> fetchRawStations(double lat, double lon, int distanceKm) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("distance", distanceKm);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<List> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );
        return Objects.requireNonNullElse(response.getBody(), List.of());
    }

    private List<double[]> searchPoints(double lat, double lon, int distanceKm) {
        double stepKm = Math.max(6, distanceKm * 0.8);
        double latDelta = stepKm / 111.0;
        double lonDelta = stepKm / (111.0 * Math.max(0.2, Math.cos(Math.toRadians(lat))));

        List<double[]> points = new ArrayList<>();
        points.add(new double[]{lat, lon});
        points.add(new double[]{lat + latDelta, lon});
        points.add(new double[]{lat - latDelta, lon});
        points.add(new double[]{lat, lon + lonDelta});
        points.add(new double[]{lat, lon - lonDelta});
        return points;
    }

    private String stationKey(StationDiscoveryItem station) {
        String id = station.id();
        if (StringUtils.hasText(id)) {
            return id;
        }
        return station.name() + "|" + station.address() + "|" + station.latitude() + "|" + station.longitude();
    }

    private record ConnectionSummary(String connectorName, Integer level, Integer totalConnectors, Integer maxPowerKw) {
    }
}
