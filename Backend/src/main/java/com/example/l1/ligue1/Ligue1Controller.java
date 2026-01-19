package com.example.l1.ligue1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "api/v1/ligue1")
public class Ligue1Controller {
    private final Ligue1Service ligue1Service;

    @Autowired
    public Ligue1Controller(Ligue1Service ligue1Service) {
        this.ligue1Service = ligue1Service;
    }

    @GetMapping("/standings")
    public StandingsResponse standings() {
        return ligue1Service.getStandings();
    }

    @GetMapping("/h2h")
    public List<H2HRow> h2h(@RequestParam String team) {
        return ligue1Service.getHeadToHead(team);
    }

    public static class LeagueTable {
        private List<String> columns;
        private List<Row> rows;

        public LeagueTable() {
        }

        public LeagueTable(List<String> columns, List<Row> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }

        public List<Row> getRows() {
            return rows;
        }

        public void setRows(List<Row> rows) {
            this.rows = rows;
        }
    }

    public static class Row {
        private java.util.Map<String, String> values;

        public Row() {
        }

        public Row(java.util.Map<String, String> values) {
            this.values = values;
        }

        public java.util.Map<String, String> getValues() {
            return values;
        }

        public void setValues(java.util.Map<String, String> values) {
            this.values = values;
        }
    }

    public static class StandingsResponse {
        private LeagueTable overall;
        private LeagueTable homeAway;

        public StandingsResponse() {
        }

        public StandingsResponse(LeagueTable overall, LeagueTable homeAway) {
            this.overall = overall;
            this.homeAway = homeAway;
        }

        public LeagueTable getOverall() {
            return overall;
        }

        public void setOverall(LeagueTable overall) {
            this.overall = overall;
        }

        public LeagueTable getHomeAway() {
            return homeAway;
        }

        public void setHomeAway(LeagueTable homeAway) {
            this.homeAway = homeAway;
        }
    }

    public static class H2HRow {
        private String opponent;
        private int mp;
        private int w;
        private int d;
        private int l;
        private int gf;
        private int ga;
        private int gd;
        private int pts;

        public H2HRow() {
        }

        public H2HRow(String opponent, int mp, int w, int d, int l, int gf, int ga, int gd, int pts) {
            this.opponent = opponent;
            this.mp = mp;
            this.w = w;
            this.d = d;
            this.l = l;
            this.gf = gf;
            this.ga = ga;
            this.gd = gd;
            this.pts = pts;
        }

        public String getOpponent() {
            return opponent;
        }

        public void setOpponent(String opponent) {
            this.opponent = opponent;
        }

        public int getMp() {
            return mp;
        }

        public void setMp(int mp) {
            this.mp = mp;
        }

        public int getW() {
            return w;
        }

        public void setW(int w) {
            this.w = w;
        }

        public int getD() {
            return d;
        }

        public void setD(int d) {
            this.d = d;
        }

        public int getL() {
            return l;
        }

        public void setL(int l) {
            this.l = l;
        }

        public int getGf() {
            return gf;
        }

        public void setGf(int gf) {
            this.gf = gf;
        }

        public int getGa() {
            return ga;
        }

        public void setGa(int ga) {
            this.ga = ga;
        }

        public int getGd() {
            return gd;
        }

        public void setGd(int gd) {
            this.gd = gd;
        }

        public int getPts() {
            return pts;
        }

        public void setPts(int pts) {
            this.pts = pts;
        }
    }
}

