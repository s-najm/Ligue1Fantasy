package com.example.l1.ligue1;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

@Service
public class Ligue1Service {
    private static final String LIGUE_1_URL = "https://fbref.com/en/comps/13/2024-2025/2024-2025-Ligue-1-Stats";
    private static final String CSV_FILE_NAME = "football_matches_2024_2025.csv";
    private static final int TIMEOUT_MS = 20000;
    private static final long CACHE_TTL_MS = 60L * 60L * 1000L;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);

    private volatile long matchesCacheAtMs = 0;
    private volatile List<CsvMatch> matchesCache = null;

    public Ligue1Controller.StandingsResponse getStandings() {
        return getStandingsFromCsv();
    }

    public List<Ligue1Controller.H2HRow> getHeadToHead(String team) {
        return getHeadToHeadFromCsv(team);
    }

    private Document fetch(String url) {
        try {
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true);
            return connection.get();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch: " + url, e);
        }
    }

    private String fetchText(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreContentType(true)
                    .execute();
            return new String(response.bodyAsBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch: " + url, e);
        }
    }

    private Ligue1Controller.StandingsResponse getStandingsFromCsv() {
        List<CsvMatch> matches = getCsvMatches();

        Map<String, TeamAggregate> byTeam = new HashMap<>();
        for (CsvMatch match : matches) {
            if (match == null) {
                continue;
            }
            String home = match.homeTeam;
            String away = match.awayTeam;
            if (home == null || away == null) {
                continue;
            }

            TeamAggregate homeAgg = byTeam.computeIfAbsent(home, t -> new TeamAggregate());
            TeamAggregate awayAgg = byTeam.computeIfAbsent(away, t -> new TeamAggregate());

            homeAgg.overall.applyMatch(match.homeGoals, match.awayGoals);
            awayAgg.overall.applyMatch(match.awayGoals, match.homeGoals);

            homeAgg.home.applyMatch(match.homeGoals, match.awayGoals);
            awayAgg.away.applyMatch(match.awayGoals, match.homeGoals);
        }

        List<Map.Entry<String, TeamAggregate>> entries = new ArrayList<>(byTeam.entrySet());
        entries.sort((a, b) -> compareTeamAgg(a.getKey(), a.getValue().overall, b.getKey(), b.getValue().overall));

        List<String> overallColumns = Arrays.asList("Rk", "Squad", "MP", "W", "D", "L", "GF", "GA", "GD", "Pts", "Pts/MP");
        List<Ligue1Controller.Row> overallRows = new ArrayList<>();
        int rk = 1;
        for (Map.Entry<String, TeamAggregate> e : entries) {
            String team = e.getKey();
            TeamAggregate agg = e.getValue();
            Map<String, String> values = new LinkedHashMap<>();
            values.put("Rk", String.valueOf(rk));
            values.put("Squad", team);
            values.put("MP", String.valueOf(agg.overall.mp));
            values.put("W", String.valueOf(agg.overall.w));
            values.put("D", String.valueOf(agg.overall.d));
            values.put("L", String.valueOf(agg.overall.l));
            values.put("GF", String.valueOf(agg.overall.gf));
            values.put("GA", String.valueOf(agg.overall.ga));
            values.put("GD", String.valueOf(agg.overall.gf - agg.overall.ga));
            values.put("Pts", String.valueOf(agg.overall.pts));
            values.put("Pts/MP", agg.overall.mp == 0 ? "" : String.format(Locale.ROOT, "%.2f", ((double) agg.overall.pts) / agg.overall.mp));
            overallRows.add(new Ligue1Controller.Row(values));
            rk++;
        }

        List<String> homeAwayColumns = Arrays.asList(
                "Rk", "Squad",
                "Home MP", "Home W", "Home D", "Home L", "Home GF", "Home GA", "Home GD", "Home Pts",
                "Away MP", "Away W", "Away D", "Away L", "Away GF", "Away GA", "Away GD", "Away Pts"
        );
        List<Ligue1Controller.Row> homeAwayRows = new ArrayList<>();
        rk = 1;
        for (Map.Entry<String, TeamAggregate> e : entries) {
            String team = e.getKey();
            TeamAggregate agg = e.getValue();
            Map<String, String> values = new LinkedHashMap<>();
            values.put("Rk", String.valueOf(rk));
            values.put("Squad", team);
            values.put("Home MP", String.valueOf(agg.home.mp));
            values.put("Home W", String.valueOf(agg.home.w));
            values.put("Home D", String.valueOf(agg.home.d));
            values.put("Home L", String.valueOf(agg.home.l));
            values.put("Home GF", String.valueOf(agg.home.gf));
            values.put("Home GA", String.valueOf(agg.home.ga));
            values.put("Home GD", String.valueOf(agg.home.gf - agg.home.ga));
            values.put("Home Pts", String.valueOf(agg.home.pts));
            values.put("Away MP", String.valueOf(agg.away.mp));
            values.put("Away W", String.valueOf(agg.away.w));
            values.put("Away D", String.valueOf(agg.away.d));
            values.put("Away L", String.valueOf(agg.away.l));
            values.put("Away GF", String.valueOf(agg.away.gf));
            values.put("Away GA", String.valueOf(agg.away.ga));
            values.put("Away GD", String.valueOf(agg.away.gf - agg.away.ga));
            values.put("Away Pts", String.valueOf(agg.away.pts));
            homeAwayRows.add(new Ligue1Controller.Row(values));
            rk++;
        }

        Ligue1Controller.LeagueTable overall = new Ligue1Controller.LeagueTable(overallColumns, overallRows);
        Ligue1Controller.LeagueTable homeAway = new Ligue1Controller.LeagueTable(homeAwayColumns, homeAwayRows);
        return new Ligue1Controller.StandingsResponse(overall, homeAway);
    }

    private List<Ligue1Controller.H2HRow> getHeadToHeadFromCsv(String team) {
        List<CsvMatch> matches = getCsvMatches();
        String wanted = normalize(team);

        Map<String, Aggregation> byOpponent = new HashMap<>();
        for (CsvMatch match : matches) {
            if (match == null) {
                continue;
            }
            String t1 = match.homeTeam;
            String t2 = match.awayTeam;
            if (t1 == null || t2 == null) {
                continue;
            }

            boolean isHome = isSameTeam(wanted, t1);
            boolean isAway = isSameTeam(wanted, t2);
            if (!isHome && !isAway) {
                continue;
            }

            String opponent = isHome ? t2 : t1;
            int gf = isHome ? match.homeGoals : match.awayGoals;
            int ga = isHome ? match.awayGoals : match.homeGoals;
            Outcome outcome = outcomeFrom(null, gf, ga);

            Aggregation agg = byOpponent.computeIfAbsent(opponent, k -> new Aggregation());
            agg.mp++;
            agg.gf += gf;
            agg.ga += ga;
            if (outcome == Outcome.W) {
                agg.w++;
                agg.pts += 3;
            } else if (outcome == Outcome.D) {
                agg.d++;
                agg.pts += 1;
            } else {
                agg.l++;
            }
        }

        List<Ligue1Controller.H2HRow> out = new ArrayList<>();
        for (Map.Entry<String, Aggregation> e : byOpponent.entrySet()) {
            Aggregation a = e.getValue();
            out.add(new Ligue1Controller.H2HRow(
                    e.getKey(),
                    a.mp,
                    a.w,
                    a.d,
                    a.l,
                    a.gf,
                    a.ga,
                    a.gf - a.ga,
                    a.pts
            ));
        }
        out.sort((a, b) -> {
            int byPts = Integer.compare(b.getPts(), a.getPts());
            if (byPts != 0) return byPts;
            int byGd = Integer.compare(b.getGd(), a.getGd());
            if (byGd != 0) return byGd;
            int byGf = Integer.compare(b.getGf(), a.getGf());
            if (byGf != 0) return byGf;
            return normalize(a.getOpponent()).compareTo(normalize(b.getOpponent()));
        });
        return out;
    }

    private int compareTeamAgg(String nameA, TeamStats a, String nameB, TeamStats b) {
        int byPts = Integer.compare(b.pts, a.pts);
        if (byPts != 0) return byPts;
        int gdA = a.gf - a.ga;
        int gdB = b.gf - b.ga;
        int byGd = Integer.compare(gdB, gdA);
        if (byGd != 0) return byGd;
        int byGf = Integer.compare(b.gf, a.gf);
        if (byGf != 0) return byGf;
        return normalize(nameA).compareTo(normalize(nameB));
    }

    private List<CsvMatch> getCsvMatches() {
        long now = System.currentTimeMillis();
        List<CsvMatch> cached = matchesCache;
        if (cached != null && (now - matchesCacheAtMs) < CACHE_TTL_MS) {
            return cached;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            cached = matchesCache;
            if (cached != null && (now - matchesCacheAtMs) < CACHE_TTL_MS) {
                return cached;
            }

            List<CsvMatch> matches = parseCsvMatches();
            matchesCache = matches;
            matchesCacheAtMs = now;
            return matches;
        }
    }

    private List<CsvMatch> parseCsvMatches() {
        List<CsvMatch> out = new ArrayList<>();
        
        try {
            Path csvPath = findCsvFile();
            if (csvPath == null) {
                System.err.println("CSV file not found: " + CSV_FILE_NAME);
                return out;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    return out;
                }
                
                String[] headers = parseCsvLine(headerLine);
                int compCodeIdx = findColumnIndex(headers, "competition_code");
                int homeTeamIdx = findColumnIndex(headers, "home_team");
                int awayTeamIdx = findColumnIndex(headers, "away_team");
                int homeGoalsIdx = findColumnIndex(headers, "fulltime_home");
                int awayGoalsIdx = findColumnIndex(headers, "fulltime_away");
                int statusIdx = findColumnIndex(headers, "status");
                
                if (compCodeIdx < 0 || homeTeamIdx < 0 || awayTeamIdx < 0 || homeGoalsIdx < 0 || awayGoalsIdx < 0) {
                    System.err.println("Required columns not found in CSV");
                    return out;
                }
                
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = parseCsvLine(line);
                    if (values.length <= Math.max(Math.max(compCodeIdx, homeTeamIdx), Math.max(awayTeamIdx, Math.max(homeGoalsIdx, awayGoalsIdx)))) {
                        continue;
                    }
                    
                    String compCode = values[compCodeIdx];
                    if (!"FL1".equals(compCode)) {
                        continue;
                    }
                    
                    if (statusIdx >= 0 && statusIdx < values.length) {
                        String status = values[statusIdx];
                        if (!"FINISHED".equals(status)) {
                            continue;
                        }
                    }
                    
                    String homeTeam = cleanTeamName(values[homeTeamIdx]);
                    String awayTeam = cleanTeamName(values[awayTeamIdx]);
                    
                    try {
                        int homeGoals = Integer.parseInt(values[homeGoalsIdx].trim());
                        int awayGoals = Integer.parseInt(values[awayGoalsIdx].trim());
                        out.add(new CsvMatch(homeTeam, awayTeam, homeGoals, awayGoals));
                    } catch (NumberFormatException e) {
                        // Skip matches with invalid scores
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
        
        return out;
    }
    
    private Path findCsvFile() {
        // Try absolute path first (project root)
        Path absolutePath = Paths.get("c:/Users/Saad/Desktop/PLWebsite-main/" + CSV_FILE_NAME);
        if (Files.exists(absolutePath)) {
            return absolutePath;
        }
        
        // Try multiple possible locations
        String[] possiblePaths = {
            CSV_FILE_NAME,
            "../" + CSV_FILE_NAME,
            "../../" + CSV_FILE_NAME,
            "../../../" + CSV_FILE_NAME
        };
        
        Path currentDir = Paths.get("").toAbsolutePath();
        for (String relativePath : possiblePaths) {
            Path path = currentDir.resolve(relativePath).normalize();
            if (Files.exists(path)) {
                return path;
            }
        }
        
        // Try from user directory
        String userDir = System.getProperty("user.dir");
        Path parent = Paths.get(userDir);
        for (int i = 0; i < 5; i++) {
            Path candidate = parent.resolve(CSV_FILE_NAME);
            if (Files.exists(candidate)) {
                return candidate;
            }
            parent = parent.getParent();
            if (parent == null) break;
        }
        
        return null;
    }
    
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }
    
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        
        return values.toArray(new String[0]);
    }
    
    private String cleanTeamName(String name) {
        if (name == null) return null;
        String cleaned = name.trim();
        
        // Map full CSV names to display names used in frontend
        Map<String, String> teamNameMap = new HashMap<>();
        teamNameMap.put("Paris Saint-Germain FC", "Paris Saint-Germain");
        teamNameMap.put("Olympique de Marseille", "Marseille");
        teamNameMap.put("AS Monaco FC", "Monaco");
        teamNameMap.put("OGC Nice", "Nice");
        teamNameMap.put("Lille OSC", "Lille");
        teamNameMap.put("Olympique Lyonnais", "Lyon");
        teamNameMap.put("RC Strasbourg Alsace", "Strasbourg");
        teamNameMap.put("Racing Club de Lens", "Lens");
        teamNameMap.put("Stade Brestois 29", "Brest");
        teamNameMap.put("Toulouse FC", "Toulouse");
        teamNameMap.put("AJ Auxerre", "Auxerre");
        teamNameMap.put("Stade Rennais FC 1901", "Rennes");
        teamNameMap.put("FC Nantes", "Nantes");
        teamNameMap.put("Angers SCO", "Angers");
        teamNameMap.put("Le Havre AC", "Le Havre");
        teamNameMap.put("Stade de Reims", "Reims");
        teamNameMap.put("AS Saint-Étienne", "Saint-Etienne");
        teamNameMap.put("Montpellier HSC", "Montpellier");
        
        if (teamNameMap.containsKey(cleaned)) {
            return teamNameMap.get(cleaned);
        }
        
        // Fallback: basic cleanup
        return cleaned
            .replace(" FC", "")
            .replace(" AC", "")
            .replace(" HSC", "")
            .replace(" OSC", "")
            .replace(" SCO", "")
            .trim();
    }

    private List<Element> collectTables(Document doc) {
        List<Element> tables = new ArrayList<>(doc.select("table"));

        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof Comment) {
                    String data = ((Comment) node).getData();
                    if (data != null && data.contains("<table")) {
                        Document parsed = Jsoup.parse(data);
                        tables.addAll(parsed.select("table"));
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
            }
        }, doc);

        return tables;
    }

    private Element findOverallTable(List<Element> tables) {
        for (Element table : tables) {
            List<String> headers = extractHeaderNames(table);
            if (headers.isEmpty()) {
                continue;
            }
            if (headers.contains("Squad")
                    && headers.contains("MP")
                    && headers.contains("W")
                    && headers.contains("D")
                    && headers.contains("L")
                    && headers.contains("GF")
                    && headers.contains("GA")
                    && headers.contains("GD")
                    && headers.contains("Pts")) {
                return table;
            }
        }
        return null;
    }

    private Element findHomeAwayTable(List<Element> tables) {
        for (Element table : tables) {
            List<String> headers = extractHeaderNames(table);
            if (headers.isEmpty()) {
                continue;
            }
            if (!headers.contains("Squad")) {
                continue;
            }
            int mpCount = 0;
            for (String h : headers) {
                if ("MP".equals(h)) {
                    mpCount++;
                }
            }
            if (headers.size() >= 25
                    && mpCount >= 2
                    && headers.contains("W")
                    && headers.contains("D")
                    && headers.contains("L")
                    && headers.contains("GF")
                    && headers.contains("GA")) {
                return table;
            }
        }
        return null;
    }

    private Ligue1Controller.LeagueTable toLeagueTable(Element table, boolean includeLinks) {
        List<String> rawHeaders = extractHeaderNames(table);
        List<String> headers = makeHeadersUnique(rawHeaders);
        List<Ligue1Controller.Row> rows = new ArrayList<>();

        for (Element tr : table.select("tbody tr")) {
            if (tr.hasClass("spacer")) {
                continue;
            }
            if ("thead".equalsIgnoreCase(tr.parent() != null ? tr.parent().tagName() : "")) {
                continue;
            }
            List<Element> cells = tr.select("th, td");
            if (cells.isEmpty()) {
                continue;
            }

            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < headers.size() && i < cells.size(); i++) {
                String header = headers.get(i);
                values.put(header, text(cells.get(i)));
                if (includeLinks && "Squad".equals(header)) {
                    Element a = cells.get(i).selectFirst("a[href]");
                    if (a != null) {
                        String href = a.absUrl("href");
                        if (href != null && !href.trim().isEmpty()) {
                            values.put("_squadUrl", href);
                        }
                    }
                }
            }

            if (!values.containsKey("Squad") || values.get("Squad") == null || values.get("Squad").trim().isEmpty()) {
                continue;
            }

            rows.add(new Ligue1Controller.Row(values));
        }

        return new Ligue1Controller.LeagueTable(headers, rows);
    }

    private List<String> makeHeadersUnique(List<String> rawHeaders) {
        Map<String, Integer> total = new HashMap<>();
        for (String h : rawHeaders) {
            total.put(h, total.getOrDefault(h, 0) + 1);
        }

        boolean looksHomeAway = rawHeaders.size() >= 25 && total.getOrDefault("MP", 0) >= 2;
        Map<String, Integer> seen = new HashMap<>();
        List<String> out = new ArrayList<>(rawHeaders.size());
        for (String h : rawHeaders) {
            int idx = seen.getOrDefault(h, 0);
            seen.put(h, idx + 1);

            if (!looksHomeAway || total.getOrDefault(h, 0) <= 1) {
                if (idx == 0) {
                    out.add(h);
                } else {
                    out.add(h + " " + (idx + 1));
                }
                continue;
            }

            if ("Rk".equals(h) || "Squad".equals(h)) {
                out.add(h);
                continue;
            }

            if (total.getOrDefault(h, 0) == 2) {
                out.add((idx == 0 ? "Home " : "Away ") + h);
            } else {
                out.add(h + " " + (idx + 1));
            }
        }
        return out;
    }

    private List<String> extractHeaderNames(Element table) {
        Element thead = table.selectFirst("thead");
        if (thead == null) {
            return Collections.emptyList();
        }

        Element headerRow = thead.select("tr").last();
        if (headerRow == null) {
            return Collections.emptyList();
        }

        List<String> headers = new ArrayList<>();
        for (Element th : headerRow.select("th")) {
            headers.add(text(th));
        }
        return headers;
    }

    private Element findMatchLogsTable(Document doc) {
        List<Element> allTables = collectTables(doc);
        for (Element table : allTables) {
            List<String> headers = extractHeaderNames(table);
            if (headers.contains("Opponent") && headers.contains("GF") && headers.contains("GA")) {
                return table;
            }
        }
        return null;
    }

    private String findSquadUrl(Ligue1Controller.LeagueTable overall, String team) {
        if (overall == null || overall.getRows() == null) {
            return null;
        }
        String wanted = normalize(team);
        for (Ligue1Controller.Row row : overall.getRows()) {
            if (row == null || row.getValues() == null) {
                continue;
            }
            String squad = row.getValues().get("Squad");
            String url = row.getValues().get("_squadUrl");
            if (squad == null || url == null) {
                continue;
            }
            if (normalize(squad).equals(wanted)) {
                return url;
            }
        }
        for (Ligue1Controller.Row row : overall.getRows()) {
            String squad = row.getValues().get("Squad");
            String url = row.getValues().get("_squadUrl");
            if (squad == null || url == null) {
                continue;
            }
            if (normalize(squad).contains(wanted) || wanted.contains(normalize(squad))) {
                return url;
            }
        }
        return null;
    }

    private Integer firstIndexOf(List<String> headers, List<String> candidates) {
        for (String c : candidates) {
            for (int i = 0; i < headers.size(); i++) {
                if (c.equals(headers.get(i))) {
                    return i;
                }
            }
        }
        return null;
    }

    private String text(Element el) {
        if (el == null) {
            return "";
        }
        return el.text() != null ? el.text().trim() : "";
    }

    private Integer parseIntSafe(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        cleaned = cleaned.replaceAll("[^0-9-]", "");
        if (cleaned.isEmpty() || "-".equals(cleaned)) {
            return null;
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents
                .toLowerCase(Locale.ROOT)
                .replace("&", "and")
                .replaceAll("[^a-z0-9]+", "");
    }

    private boolean isSameTeam(String normalizedWanted, String teamName) {
        if (normalizedWanted == null) {
            normalizedWanted = "";
        }
        if (normalizedWanted.isEmpty()) {
            return false;
        }
        String normalizedTeam = normalize(teamName);
        return normalizedTeam.equals(normalizedWanted)
                || normalizedTeam.contains(normalizedWanted)
                || normalizedWanted.contains(normalizedTeam);
    }

    private Outcome outcomeFrom(String result, int gf, int ga) {
        if (result != null) {
            String r = result.trim().toUpperCase(Locale.ROOT);
            if (r.startsWith("W")) {
                return Outcome.W;
            }
            if (r.startsWith("D")) {
                return Outcome.D;
            }
            if (r.startsWith("L")) {
                return Outcome.L;
            }
        }
        if (gf > ga) {
            return Outcome.W;
        }
        if (gf == ga) {
            return Outcome.D;
        }
        return Outcome.L;
    }

    private enum Outcome {
        W,
        D,
        L
    }

    private static class TeamAggregate {
        TeamStats overall = new TeamStats();
        TeamStats home = new TeamStats();
        TeamStats away = new TeamStats();
    }

    private static class TeamStats {
        int mp;
        int w;
        int d;
        int l;
        int gf;
        int ga;
        int pts;

        void applyMatch(int gf, int ga) {
            mp++;
            this.gf += gf;
            this.ga += ga;
            if (gf > ga) {
                w++;
                pts += 3;
            } else if (gf == ga) {
                d++;
                pts += 1;
            } else {
                l++;
            }
        }
    }

    private static class CsvMatch {
        final String homeTeam;
        final String awayTeam;
        final int homeGoals;
        final int awayGoals;

        CsvMatch(String homeTeam, String awayTeam, int homeGoals, int awayGoals) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.homeGoals = homeGoals;
            this.awayGoals = awayGoals;
        }
    }

    private static class Aggregation {
        int mp;
        int w;
        int d;
        int l;
        int gf;
        int ga;
        int pts;
    }
}
