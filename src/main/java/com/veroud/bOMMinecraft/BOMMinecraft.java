package com.veroud.bOMMinecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public final class BOMMinecraft extends JavaPlugin {

    private String stationProduct;
    private String stationId;
    private String lastWeather = ""; // track last applied weather
    private String lastStationName = ""; // track station name

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadStationInfo();

        getLogger().info("BOMMinecraft enabled! Using station " + stationProduct + " " + stationId);

        // Scheduled weather update every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::updateWeather, 0L, 6000L);
    }

    private void loadStationInfo() {
        FileConfiguration config = getConfig();
        stationProduct = config.getString("station_product", "IDQ60901");
        stationId = config.getString("station_id", "94576");
    }

    private String fetchWeatherJson() throws Exception {
        String bomUrl = "http://www.bom.gov.au/fwo/" + stationProduct + "/" + stationProduct + "." + stationId + ".json";
        URL url = new URL(bomUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        // Add a User-Agent to avoid 403
        con.setRequestProperty("User-Agent", "Minecraft-BOM-Plugin/1.0 (https://github.com/Austayo/BOMMinecraft)");

        int responseCode = con.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Server returned HTTP response code: " + responseCode);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            return in.lines().collect(Collectors.joining());
        }
    }


    private void updateWeather() {
        try {
            String json = fetchWeatherJson();
            applyWeather(json);
        } catch (Exception e) {
            getLogger().warning("Failed to fetch BOM data: " + e.getMessage());
        }
    }

    private void applyWeather(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject observations = root.getAsJsonObject("observations");

            JsonObject data = observations.getAsJsonArray("data").get(0).getAsJsonObject();
            String stationName = observations.getAsJsonArray("header")
                    .get(0).getAsJsonObject()
                    .get("product_name").getAsString();

            String cloud = data.get("cloud").getAsString().toLowerCase();
            double rain = data.get("rain_trace").isJsonNull() ? 0 : data.get("rain_trace").getAsDouble();

            World world = Bukkit.getWorlds().get(0);

            String newWeather;

            if (cloud.contains("thunder") || cloud.contains("thunderstorm") || rain > 5) {
                // Heavy rain / thunderstorm
                world.setStorm(true);
                world.setThundering(true);
                newWeather = "Thunderstorm";
            } else if ((cloud.contains("rain") || cloud.contains("showers") || cloud.contains("drizzle")) && rain > 1) {
                // Moderate rain
                world.setStorm(true);
                world.setThundering(false);
                newWeather = "Moderate Rain";
            } else if (rain > 0) {
                // Light rain
                world.setStorm(true);
                world.setThundering(false);
                newWeather = "Light Rain";
            } else if (cloud.contains("fog") || cloud.contains("haze")) {
                world.setStorm(false);
                world.setThundering(false);
                newWeather = "Foggy/Clear";
            } else if (cloud.contains("snow") || cloud.contains("hail")) {
                world.setStorm(true);
                world.setThundering(false);
                newWeather = "Snow/Hail"; // optional: spawn snow particles
            } else {
                world.setStorm(false);
                world.setThundering(false);
                newWeather = "Clear";
            }

            // Only broadcast if weather or station changed
            if (!newWeather.equals(lastWeather) || !stationName.equals(lastStationName)) {
                Bukkit.broadcastMessage("§b[BOM - " + stationName + "] Weather has changed to: §e" + newWeather + "§b!");
                lastWeather = newWeather;
                lastStationName = stationName;
            }

        } catch (Exception e) {
            getLogger().warning("Error applying weather: " + e.getMessage());
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("setidr")) {
            if (!sender.hasPermission("bomminecraft.setidr")) {
                sender.sendMessage("§cYou don't have permission to run this command.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("§eUsage: /setidr <product> <stationId>");
                return true;
            }

            stationProduct = args[0];
            stationId = args[1];

            getConfig().set("station_product", stationProduct);
            getConfig().set("station_id", stationId);
            saveConfig();

            sender.sendMessage("§aBOM station updated to: " + stationProduct + " " + stationId);
            getLogger().info("BOM station changed to: " + stationProduct + " " + stationId);

            // Immediately fetch and apply weather for the new station
            Bukkit.getScheduler().runTaskAsynchronously(this, this::updateWeather);

            return true;
        }
        return false;
    }
}
