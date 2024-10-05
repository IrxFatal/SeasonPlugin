package com.irxfatal.seasonplugin;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public final class SeasonPlugin extends JavaPlugin {

    private Connection connection;
    private String currentSeason = "SPRING";

    @Override
    public void onEnable() {
        setupDatabase();
        try {
            loadSeasonFromDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        startCycle();

    }

    @Override
    public void onDisable() {
        closeDatabase();
    }

    private void setupDatabase() {
        try {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:seasons.db");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS season (id INTEGER PRIMARY KEY, season TEXT)");
            statement.executeUpdate("INSERT OR IGNORE INTO season (id,season) VALUES (1,'SPRING')");
            statement.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSeasonFromDatabase() throws SQLException {
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT season FROM season WHERE id = 1");

            if (rs.next()){
                currentSeason = rs.getString("season");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveSeasonToDatabase() throws SQLException {
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE season SET season = ? WHERE id = 1");
            stmt.setString(1, currentSeason);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void startCycle() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            switch (currentSeason) {
                case "SPRING":
                    try {
                        setSeason("SUMMER");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "SUMMER":
                    try {
                        setSeason("AUTUMN");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "AUTUMN":
                    try {
                        setSeason("WINTER");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "WINTER":
                    try {
                        setSeason("SPRING");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        }, 20L * 60 * 60, 20L * 60 * 60);
    }

    private void setSeason(String newSeason) throws SQLException {
        currentSeason = newSeason;
        saveSeasonToDatabase();
        applySeason();
    }

    private void applySeason(){
        try {
            World world = Bukkit.getWorld("world");
            assert  world != null;
            switch (currentSeason) {
                case "SPRING":
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 5);
                    world.setStorm(true);
                    Bukkit.broadcastMessage("Началась весна!");
                    break;
                case "SUMMER":
                    world.setClearWeatherDuration(6000);
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 4);
                    world.setStorm(false);
                    Bukkit.broadcastMessage("Началось лето!");
                    break;
                case "AUTUMN":
                    world.setStorm(true);
                    world.setClearWeatherDuration(0);
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 2);
                    Bukkit.broadcastMessage("Началась осень!");
                    break;
                case "WINTER":
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 1);
                    world.setThundering(true);
                    Bukkit.broadcastMessage("Началась зима!");
                    break;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("setseason")) {
            if (sender.hasPermission("seasons.setseason")) {
                if (args.length > 0) {
                    String season = args[0].toUpperCase();
                    if (season.equals("SPRING") || season.equals("SUMMER") || season.equals("AUTUMN") || season.equals("WINTER")) {
                        try {
                            setSeason(season);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        sender.sendMessage("Сезон изменен на:" + season);
                    } else {
                        sender.sendMessage("Неправильное название сезона! Укажите: <SPRING | SUMMER | AUTUMN | WINTER>");
                    }

                } else {
                    sender.sendMessage("Используйте: /setseason <SPRING | SUMMER | AUTUMN | WINTER>");
                }
            } else {
                sender.sendMessage("У вас нету прав для выполнения этой команды.");
            } return true;
        }

        if (label.equalsIgnoreCase("season") && sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage("Текущий сезон: " + currentSeason);
            return true;
        }
        return false;
    }

    private void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

