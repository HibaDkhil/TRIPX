package tn.esprit.services;

import javafx.scene.image.Image;
import tn.esprit.entities.RoomImage;
import tn.esprit.utils.MyDB;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoomImageService {
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private final Connection cnx;

    public RoomImageService() {
        this.cnx = MyDB.getConnection();
    }

    public List<RoomImage> getByRoomId(int roomId) {
        List<RoomImage> images = new ArrayList<>();
        String sql = """
                SELECT * FROM room_images
                WHERE room_id = ?
                ORDER BY is_primary DESC, display_order ASC, id ASC
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    images.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return images;
    }

    public RoomImage addImage(int roomId, File sourceFile) throws SQLException, IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Image file not found");
        }

        String extension = getExtension(sourceFile.getName());
        String uniqueName = UUID.randomUUID() + extension;
        Path destinationDir = Path.of(PROJECT_ROOT, "uploads", "images", "rooms", String.valueOf(roomId));
        Files.createDirectories(destinationDir);
        Path destinationPath = destinationDir.resolve(uniqueName);
        Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        int nextOrder = getNextDisplayOrder(roomId);
        boolean primary = !hasAnyImage(roomId);

        Image fxImage = null;
        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            fxImage = new Image(fis);
        } catch (Exception ignored) {
        }

        String dbPath = "/uploads/images/rooms/" + roomId + "/" + uniqueName;
        String insert = """
                INSERT INTO room_images
                (room_id, file_name, file_path, mime_type, file_size_bytes, width, height, is_primary, display_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, roomId);
            ps.setString(2, sourceFile.getName());
            ps.setString(3, dbPath);
            ps.setString(4, detectMimeType(sourceFile.toPath()));
            ps.setLong(5, sourceFile.length());
            if (fxImage != null && !fxImage.isError()) {
                ps.setInt(6, (int) Math.round(fxImage.getWidth()));
                ps.setInt(7, (int) Math.round(fxImage.getHeight()));
            } else {
                ps.setNull(6, Types.INTEGER);
                ps.setNull(7, Types.INTEGER);
            }
            ps.setBoolean(8, primary);
            ps.setInt(9, nextOrder);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getById(keys.getInt(1));
                }
            }
        }
        return null;
    }

    public void deleteImage(int imageId, int roomId) throws SQLException {
        RoomImage image = getById(imageId);
        if (image == null || image.getRoomId() != roomId) {
            return;
        }

        String delete = "DELETE FROM room_images WHERE id = ? AND room_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(delete)) {
            ps.setInt(1, imageId);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }

        deletePhysicalFile(image.getFilePath());

        // Ensure there is always one primary image if any images remain.
        List<RoomImage> remaining = getByRoomId(roomId);
        boolean anyPrimary = remaining.stream().anyMatch(RoomImage::isPrimary);
        if (!remaining.isEmpty() && !anyPrimary) {
            setPrimaryImage(roomId, remaining.get(0).getId());
        }
    }

    public void setPrimaryImage(int roomId, int imageId) throws SQLException {
        cnx.setAutoCommit(false);
        try {
            try (PreparedStatement clear = cnx.prepareStatement(
                    "UPDATE room_images SET is_primary = 0 WHERE room_id = ?")) {
                clear.setInt(1, roomId);
                clear.executeUpdate();
            }
            try (PreparedStatement set = cnx.prepareStatement(
                    "UPDATE room_images SET is_primary = 1 WHERE room_id = ? AND id = ?")) {
                set.setInt(1, roomId);
                set.setInt(2, imageId);
                set.executeUpdate();
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public void moveImage(int roomId, int imageId, boolean moveUp) throws SQLException {
        List<RoomImage> ordered = getByRoomId(roomId);
        int currentIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId() == imageId) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) return;

        int targetIndex = moveUp ? currentIndex - 1 : currentIndex + 1;
        if (targetIndex < 0 || targetIndex >= ordered.size()) return;

        RoomImage current = ordered.get(currentIndex);
        RoomImage target = ordered.get(targetIndex);

        cnx.setAutoCommit(false);
        try {
            try (PreparedStatement ps1 = cnx.prepareStatement(
                    "UPDATE room_images SET display_order = ? WHERE id = ?")) {
                ps1.setInt(1, target.getDisplayOrder());
                ps1.setInt(2, current.getId());
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = cnx.prepareStatement(
                    "UPDATE room_images SET display_order = ? WHERE id = ?")) {
                ps2.setInt(1, current.getDisplayOrder());
                ps2.setInt(2, target.getId());
                ps2.executeUpdate();
            }
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public void reorderImages(int roomId, List<Integer> orderedImageIds) throws SQLException {
        if (orderedImageIds == null || orderedImageIds.isEmpty()) return;

        cnx.setAutoCommit(false);
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE room_images SET display_order = ? WHERE room_id = ? AND id = ?")) {
            int order = 1;
            for (Integer imageId : orderedImageIds) {
                if (imageId == null) continue;
                ps.setInt(1, order++);
                ps.setInt(2, roomId);
                ps.setInt(3, imageId);
                ps.addBatch();
            }
            ps.executeBatch();
            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    public RoomImage getById(int id) {
        String sql = "SELECT * FROM room_images WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private RoomImage mapRow(ResultSet rs) throws SQLException {
        RoomImage image = new RoomImage();
        image.setId(rs.getInt("id"));
        image.setRoomId(rs.getInt("room_id"));
        image.setFileName(rs.getString("file_name"));
        image.setFilePath(rs.getString("file_path"));
        image.setMimeType(rs.getString("mime_type"));
        long size = rs.getLong("file_size_bytes");
        image.setFileSizeBytes(rs.wasNull() ? null : size);
        int width = rs.getInt("width");
        image.setWidth(rs.wasNull() ? null : width);
        int height = rs.getInt("height");
        image.setHeight(rs.wasNull() ? null : height);
        image.setPrimary(rs.getBoolean("is_primary"));
        image.setDisplayOrder(rs.getInt("display_order"));
        image.setCreatedAt(rs.getTimestamp("created_at"));
        image.setUpdatedAt(rs.getTimestamp("updated_at"));
        return image;
    }

    private boolean hasAnyImage(int roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM room_images WHERE room_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int getNextDisplayOrder(int roomId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(display_order), 0) + 1 FROM room_images WHERE room_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 1;
    }

    private String detectMimeType(Path path) {
        try {
            String mime = Files.probeContentType(path);
            return mime == null ? "application/octet-stream" : mime;
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String getExtension(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0) return ".jpg";
        return name.substring(index);
    }

    private void deletePhysicalFile(String dbPath) {
        if (dbPath == null || dbPath.isBlank()) return;
        try {
            String normalized = dbPath.replace("\\", "/");
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            Path physical = Path.of(PROJECT_ROOT, normalized);
            Files.deleteIfExists(physical);
        } catch (Exception ignored) {
        }
    }
}
