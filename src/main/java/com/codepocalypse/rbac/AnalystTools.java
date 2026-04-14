package com.codepocalypse.rbac;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Tools available to ANALYST role and above (ADMIN inherits via role hierarchy).
 *
 * Each @Tool method is protected by @PreAuthorize -- if a VIEWER user's LLM
 * tries to call these tools, Spring Security blocks execution at the method
 * level BEFORE any tool logic runs. The user gets an access denied error,
 * not a hallucinated result. That's the whole point.
 */
@Component
public class AnalystTools {

    private static final String CFP_URL = "https://developers.events/all-cfps.json";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @PreAuthorize("hasRole('ANALYST')")
    @Tool(description = "Get the current date and time")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @PreAuthorize("hasRole('ANALYST')")
    @Tool(description = "Get the current weather for a city (simulated)")
    public String getWeather(@ToolParam(description = "City name") String city) {
        return "Weather in " + city + ": 22\u00b0C, sunny with light clouds";
    }

    @PreAuthorize("hasRole('ANALYST')")
    @Tool(description = "Search for developer conferences with open CFPs by keyword. Returns names, locations, deadlines, and links.")
    public String searchCfps(
            @ToolParam(description = "Search keyword like 'java', 'kubernetes', 'AI', 'devops'") String keyword,
            @ToolParam(description = "Max results (default 5)", required = false) Integer limit) {
        int max = (limit != null && limit > 0) ? limit : 5;
        try {
            String kw = keyword.toLowerCase();
            List<Map<String, Object>> matches = fetchOpenCfps().stream()
                    .filter(c -> confName(c).toLowerCase().contains(kw))
                    .limit(max)
                    .toList();
            return format("CFPs matching '" + keyword + "'", matches);
        } catch (Exception e) {
            return "Error searching CFPs: " + e.getMessage();
        }
    }

    // --- Feed parsing (reused from jclaw-spring-ai ConferenceTools) ---

    private List<Map<String, Object>> fetchOpenCfps() throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create(CFP_URL)).GET().build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        List<Map<String, Object>> all = mapper.readValue(resp.body(),
                new TypeReference<List<Map<String, Object>>>() {});
        long now = System.currentTimeMillis();
        return all.stream()
                .filter(c -> "open".equals(confStatus(c)) && deadlineMillis(c) > now)
                .toList();
    }

    private String format(String title, List<Map<String, Object>> cfps) {
        if (cfps.isEmpty()) return title + ": No results found.";
        var sb = new StringBuilder(title + " (" + cfps.size() + " results):\n\n");
        for (int i = 0; i < cfps.size(); i++) {
            var c = cfps.get(i);
            sb.append(i + 1).append(". ").append(confName(c)).append("\n");
            sb.append("   Location: ").append(confLocation(c)).append("\n");
            sb.append("   CFP Deadline: ").append(str(c, "until")).append("\n");
            sb.append("   CFP Link: ").append(str(c, "link")).append("\n");
            sb.append("   Website: ").append(confHyperlink(c)).append("\n");
            long start = confStartDate(c);
            if (start > 0) {
                sb.append("   Conference Date: ")
                        .append(Instant.ofEpochMilli(start).atOffset(ZoneOffset.UTC).toLocalDate())
                        .append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> conf(Map<String, Object> cfp) {
        Object c = cfp.get("conf");
        return c instanceof Map ? (Map<String, Object>) c : Map.of();
    }

    private String confName(Map<String, Object> cfp) { return str(conf(cfp), "name"); }
    private String confLocation(Map<String, Object> cfp) { return str(conf(cfp), "location"); }
    private String confStatus(Map<String, Object> cfp) { return str(conf(cfp), "status"); }
    private String confHyperlink(Map<String, Object> cfp) { return str(conf(cfp), "hyperlink"); }

    @SuppressWarnings("unchecked")
    private long confStartDate(Map<String, Object> cfp) {
        Object d = conf(cfp).get("date");
        if (d instanceof List<?> list && !list.isEmpty()) {
            return ((Number) list.get(0)).longValue();
        }
        return 0;
    }

    private long deadlineMillis(Map<String, Object> cfp) {
        Object ud = cfp.get("untilDate");
        return ud instanceof Number ? ((Number) ud).longValue() : 0;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
