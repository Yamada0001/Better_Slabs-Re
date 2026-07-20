package org.little100.better_slabs.storage;

import org.bukkit.Material;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.SlabFace;
import org.little100.better_slabs.model.VerticalHalf;
import org.little100.better_slabs.model.VerticalSlabCell;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SlabStorage {

    private final BetterSlabs plugin;
    private final ConcurrentHashMap<String, VerticalSlabCell> cells = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> chunkIndex = new ConcurrentHashMap<>();
    private final File dbFile;
    private volatile Connection connection;

    public SlabStorage(BetterSlabs plugin) {
        this.plugin = plugin;
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder");
        }
        this.dbFile = new File(folder, "slabs");
    }

    private String jdbcUrl() {
        return "jdbc:h2:" + dbFile.getAbsolutePath().replace('\\', '/')
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
                + ";DB_CLOSE_DELAY=-1;AUTO_RECONNECT=TRUE";
    }

    private void loadDriver() throws ClassNotFoundException {
        try {
            Class.forName("org.h2.Driver");
            return;
        } catch (ClassNotFoundException ignored) {
        }
        Class.forName("org.little100.better_slabs.lib.h2.Driver");
    }

    public void load() {
        cells.clear();
        chunkIndex.clear();
        try {
            loadDriver();
            connection = DriverManager.getConnection(jdbcUrl(), "sa", "");
            try (Statement st = connection.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS vertical_cells (
                          world_id VARCHAR(36) NOT NULL,
                          x INT NOT NULL,
                          y INT NOT NULL,
                          z INT NOT NULL,
                          half_index TINYINT NOT NULL,
                          slab VARCHAR(64) NOT NULL,
                          face VARCHAR(16) NOT NULL,
                          PRIMARY KEY (world_id, x, y, z, half_index)
                        )
                        """);
                st.execute("""
                        CREATE INDEX IF NOT EXISTS idx_vcells_chunk
                        ON vertical_cells (world_id, x, z)
                        """);
            }
            int loaded = 0;
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT world_id, x, y, z, half_index, slab, face FROM vertical_cells ORDER BY world_id, x, y, z, half_index")) {
                while (rs.next()) {
                    UUID worldId;
                    try {
                        worldId = UUID.fromString(rs.getString("world_id"));
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    Material slab = Material.matchMaterial(rs.getString("slab"));
                    SlabFace face = SlabFace.fromString(rs.getString("face"));
                    if (slab == null || face == null) {
                        continue;
                    }
                    String key = worldId + ":" + x + ":" + y + ":" + z;
                    VerticalSlabCell cell = cells.computeIfAbsent(key, k -> new VerticalSlabCell(worldId, x, y, z));
                    cell.forceAddHalf(new VerticalHalf(slab, face));
                    addToChunkIndex(worldId, x, z, key);
                    loaded++;
                }
            }
            plugin.getLogger().info("H2 loaded " + cells.size() + " cells (" + loaded + " halves)");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to open H2 database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addToChunkIndex(UUID worldId, int x, int z, String cellKey) {
        String chunkKey = worldId + ":" + (x >> 4) + ":" + (z >> 4);
        chunkIndex.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(cellKey);
    }

    private void removeFromChunkIndex(UUID worldId, int x, int z, String cellKey) {
        String chunkKey = worldId + ":" + (x >> 4) + ":" + (z >> 4);
        Set<String> cells = chunkIndex.get(chunkKey);
        if (cells != null) {
            cells.remove(cellKey);
            if (cells.isEmpty()) {
                chunkIndex.remove(chunkKey);
            }
        }
    }

    public void save() {
        Connection conn = connection;
        if (conn == null) {
            return;
        }
        try {
            if (conn.isClosed()) {
                plugin.getLogger().warning("H2 connection closed, attempting reconnect");
                connection = DriverManager.getConnection(jdbcUrl(), "sa", "");
                conn = connection;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("H2 reconnect failed: " + e.getMessage());
            return;
        }
        try {
            synchronized (this) {
                conn.setAutoCommit(false);
                try (Statement wipe = conn.createStatement()) {
                    wipe.execute("DELETE FROM vertical_cells");
                }
                String sql = "INSERT INTO vertical_cells (world_id, x, y, z, half_index, slab, face) VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (VerticalSlabCell cell : cells.values()) {
                        if (cell.isEmpty()) {
                            continue;
                        }
                        int index = 0;
                        for (VerticalHalf half : cell.getHalves()) {
                            ps.setString(1, cell.getWorldId().toString());
                            ps.setInt(2, cell.getX());
                            ps.setInt(3, cell.getY());
                            ps.setInt(4, cell.getZ());
                            ps.setInt(5, index++);
                            ps.setString(6, half.getSlabMaterial().name());
                            ps.setString(7, half.getFace().name());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save H2 database: " + e.getMessage());
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public void saveCell(VerticalSlabCell cell) {
        if (connection == null || cell == null) {
            return;
        }
        final UUID worldId = cell.getWorldId();
        final int x = cell.getX();
        final int y = cell.getY();
        final int z = cell.getZ();
        final var halves = cell.getHalves();
        plugin.getScheduler().runAsync(() -> {
            Connection conn = connection;
            if (conn == null) {
                return;
            }
            try {
                synchronized (this) {
                    deleteCellSync(worldId, x, y, z);
                    if (halves.isEmpty()) {
                        return;
                    }
                    String sql = "INSERT INTO vertical_cells (world_id, x, y, z, half_index, slab, face) VALUES (?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        int index = 0;
                        for (VerticalHalf half : halves) {
                            ps.setString(1, worldId.toString());
                            ps.setInt(2, x);
                            ps.setInt(3, y);
                            ps.setInt(4, z);
                            ps.setInt(5, index++);
                            ps.setString(6, half.getSlabMaterial().name());
                            ps.setString(7, half.getFace().name());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("H2 saveCell failed: " + e.getMessage());
            }
        });
    }

    public void deleteCellAsync(UUID worldId, int x, int y, int z) {
        plugin.getScheduler().runAsync(() -> {
            try {
                synchronized (this) {
                    deleteCellSync(worldId, x, y, z);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("H2 deleteCell failed: " + e.getMessage());
            }
        });
    }

    private void deleteCellSync(UUID worldId, int x, int y, int z) throws SQLException {
        Connection conn = connection;
        if (conn == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM vertical_cells WHERE world_id=? AND x=? AND y=? AND z=?")) {
            ps.setString(1, worldId.toString());
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.executeUpdate();
        }
    }

    public void saveAsync() {
        plugin.getScheduler().runAsync(this::save);
    }

    public void close() {
        save();
        Connection conn = connection;
        connection = null;
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("H2 close: " + e.getMessage());
            }
        }
    }

    public VerticalSlabCell get(String key) {
        return cells.get(key);
    }

    public VerticalSlabCell getOrCreate(VerticalSlabCell template) {
        return cells.computeIfAbsent(template.key(), k -> template);
    }

    public void put(VerticalSlabCell cell) {
        if (cell == null || cell.isEmpty()) {
            if (cell != null) {
                cells.remove(cell.key());
                removeFromChunkIndex(cell.getWorldId(), cell.getX(), cell.getZ(), cell.key());
                deleteCellAsync(cell.getWorldId(), cell.getX(), cell.getY(), cell.getZ());
            }
            return;
        }
        cells.put(cell.key(), cell);
        addToChunkIndex(cell.getWorldId(), cell.getX(), cell.getZ(), cell.key());
        saveCell(cell);
    }

    public void remove(String key) {
        VerticalSlabCell removed = cells.remove(key);
        if (removed != null) {
            removeFromChunkIndex(removed.getWorldId(), removed.getX(), removed.getZ(), key);
            deleteCellAsync(removed.getWorldId(), removed.getX(), removed.getY(), removed.getZ());
        }
    }

    public Collection<VerticalSlabCell> all() {
        return cells.values();
    }

    public Collection<VerticalSlabCell> getCellsInChunk(UUID worldId, int chunkX, int chunkZ) {
        String chunkKey = worldId + ":" + chunkX + ":" + chunkZ;
        Set<String> cellKeys = chunkIndex.get(chunkKey);
        if (cellKeys == null || cellKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<VerticalSlabCell> result = new ArrayList<>();
        for (String key : cellKeys) {
            VerticalSlabCell cell = cells.get(key);
            if (cell != null) {
                result.add(cell);
            }
        }
        return result;
    }

    public int size() {
        return cells.size();
    }
}
