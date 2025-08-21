package com.veroud.bOMMinecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public final class BOMMinecraft extends JavaPlugin {

    private String stationProduct;
    private String stationId;
    private String lastWeather = "";
    private String lastStationName = "";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .build();
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

    // OkHttp-based fetch
    private String fetchWeatherJson() throws Exception {
        String bomUrl = "https://www.bom.gov.au/fwo/" + stationProduct + "/" + stationProduct + "." + stationId + ".json";

        Request request = new Request.Builder()
                .url(bomUrl)
                // Use a real browser User-Agent to bypass 403
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .header("Connection", "keep-alive")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to fetch BOM data: " + response.code());
            }
            return response.body().string();
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

        String weather = data.get("weather").isJsonNull() ? "" : data.get("weather").getAsString().toLowerCase();
        double rain = data.get("rain_trace").isJsonNull() ? 0.0 : data.get("rain_trace").getAsDouble();

        // Skip if weather is "-"
        if (weather.equals("-")) {
            getLogger().info("Weather is '-', skipping update.");
            return;
        }

        World world = Bukkit.getWorlds().get(0);

        String newWeather;

        if (weather.contains("thunderstorm")) {
            world.setStorm(true);
            world.setThundering(true);
            newWeather = "Thunderstorm";
        } else if (rain >= 5.0) {
            world.setStorm(true);
            world.setThundering(false);
            newWeather = "Heavy Rain";
        } else if (rain >= 1.0) {
            world.setStorm(true);
            world.setThundering(false);
            newWeather = "Moderate Rain";
        } else if (rain > 0.0) {
            world.setStorm(true);
            world.setThundering(false);
            newWeather = "Light Rain";
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
