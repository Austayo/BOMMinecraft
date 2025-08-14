# BOMMinecraft

**BOMMinecraft** is a Minecraft Paper plugin that syncs your server weather with real-world weather from the Australian Bureau of Meteorology (BOM). Players will see in-game weather that reflects local conditions such as clear skies, rain, and thunderstorms.

---

## Features

- Fetches real-time weather data from BOM using station product and ID.
- Supports light, moderate, and heavy rain intensity.
- Thunderstorms are triggered based on BOM observations.
- Broadcasts weather changes in chat with the station name.
- Command `/setidr <product> <stationId>` to set the weather station and apply immediately.

---

## Installation

1. Make sure your server runs **Paper** (or compatible Spigot/Paper fork) version 1.21+.
2. Download the latest `BOMMinecraft.jar`.
3. Place the jar in your server's `plugins/` folder.
4. Start the server to generate the default `config.yml`.
5. Stop the server and configure your default station if desired.

---

## Configuration (`config.yml`)

```yaml
station_product: IDQ60901   # BOM product code for the station
station_id: 94576           # BOM station ID
```

These values are used to fetch weather data from BOM.

The plugin will periodically update the weather every 5 minutes.

## Commands
/setidr <product> <stationId>

Sets the BOM station for weather syncing.

Example:

/setidr IDQ60901 94576


## Notes:

Only users with the permission bomminecraft.setidr can run this command.

The command immediately fetches and applies weather for the new station.

Chat messages will show the station name from BOM, e.g.:

[BOM - Brisbane Eagle Farm] Weather has changed to: Rain!

## Permissions
Permission	Default	Description
bomminecraft.setidr	OP	Allows changing the BOM station used by the plugin
## Weather Mapping

The plugin maps BOM data to Minecraft weather as follows:

BOM Description / Data	Minecraft Weather
thunder / thunderstorm / heavy rain	Thunderstorm
rain / showers / drizzle	Light or Moderate Rain
fog / haze	Foggy / Clear
clear / fine / mostly clear	Clear
snow / hail	Snow / Hail (optional)

Rain intensity is determined from BOM's rain_trace:

0 mm → Clear

0.1–1 mm → Light Rain

1–5 mm → Moderate Rain

>5 mm → Thunderstorm

## Notes

The plugin requires internet access to fetch BOM data.

Weather updates automatically every 5 minutes but can be triggered immediately using /setidr.

Only the first loaded world is affected. Additional worlds are not currently supported.

##License

MIT License – free to use and modify.
