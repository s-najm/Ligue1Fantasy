package com.example.l1;

import com.example.l1.player.Player;
import com.example.l1.player.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private PlayerRepository playerRepository;

    @Override
    public void run(String... args) throws Exception {
        loadPlayerData();
    }

    private void loadPlayerData() {
        String csvFile = "../ligue1_stats.csv";
        String line;
        String csvSplitBy = ",";
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy);
                
                if (data.length >= 15) {
                    try {
                        Player player = new Player();
                        player.setName(data[0]);
                        player.setNation(data[1]);
                        player.setPos(data[2]);
                        player.setAge(parseIntSafe(data[3]));
                        player.setMp(parseIntSafe(data[4]));
                        player.setStarts(parseIntSafe(data[5]));
                        player.setMin(parseDoubleSafe(data[6]));
                        player.setGls(parseDoubleSafe(data[7]));
                        player.setAst(parseDoubleSafe(data[8]));
                        player.setPk(parseDoubleSafe(data[9]));
                        player.setCrdy(parseDoubleSafe(data[10]));
                        player.setCrdr(parseDoubleSafe(data[11]));
                        player.setXg(parseDoubleSafe(data[12]));
                        player.setXag(parseDoubleSafe(data[13]));
                        player.setTeam(data[14]);
                        
                        playerRepository.save(player);
                        count++;
                    } catch (Exception e) {
                        System.err.println("Error parsing line: " + line);
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Loaded " + count + " players from CSV");
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int parseIntSafe(String value) {
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
