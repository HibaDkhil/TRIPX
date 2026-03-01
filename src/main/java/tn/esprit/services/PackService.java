package tn.esprit.services;

import tn.esprit.entities.Pack;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PackService implements ICRUD<Pack> {

    private Connection conx;

    public PackService() {
        conx = MyDB.getInstance().getConx();
    }

    @Override
    public void add(Pack pack) throws SQLException {
        String req = "INSERT INTO `packs`(`title`, `description`, `destination_id`, `accommodation_id`, "
                + "`activity_id`, `transport_id`, `category_id`, `duration_days`, `base_price`, `status`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setString(1, pack.getTitle());
        pstm.setString(2, pack.getDescription());
        pstm.setInt(3, Math.toIntExact(pack.getDestinationId()));
        pstm.setInt(4, pack.getAccommodationId());
        pstm.setInt(5, Math.toIntExact(pack.getActivityId()));
        pstm.setInt(6, pack.getTransportId());
        pstm.setInt(7, pack.getCategoryId());
        pstm.setInt(8, pack.getDurationDays());
        pstm.setBigDecimal(9, pack.getBasePrice());
        pstm.setString(10, pack.getStatus().name());
        pstm.executeUpdate();
        System.out.println("Pack ajouté (méthode 2): " + pack.getTitle());
    }


    @Override
    public void modifier(Pack pack) throws SQLException {
        String req = "UPDATE `packs` SET `title` = ?, `description` = ?, `destination_id` = ?, "
                + "`accommodation_id` = ?, `activity_id` = ?, `transport_id` = ?, `category_id` = ?, "
                + "`duration_days` = ?, `base_price` = ?, `status` = ? WHERE `id_pack` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setString(1, pack.getTitle());
        pstm.setString(2, pack.getDescription());
        pstm.setInt(3, Math.toIntExact(pack.getDestinationId()));
        pstm.setInt(4, pack.getAccommodationId());
        pstm.setInt(5, Math.toIntExact(pack.getActivityId()));
        pstm.setInt(6, pack.getTransportId());
        pstm.setInt(7, pack.getCategoryId());
        pstm.setInt(8, pack.getDurationDays());
        pstm.setBigDecimal(9, pack.getBasePrice());
        pstm.setString(10, pack.getStatus().name());
        pstm.setInt(11, pack.getIdPack());
        pstm.executeUpdate();
        System.out.println("Pack modifié: " + pack.getTitle());
    }

    @Override
    public void delete(Pack pack) throws SQLException {
        String req = "DELETE FROM `packs` WHERE `id_pack` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setInt(1, pack.getIdPack());
        pstm.executeUpdate();
        System.out.println("Pack supprimé: id=" + pack.getIdPack());
    }

    @Override
    public List<Pack> afficherList() throws SQLException {
        String req = "SELECT * FROM `packs`";
        ResultSet res = conx.createStatement().executeQuery(req);
        List<Pack> packs = new ArrayList<>();
        while (res.next()) {
            packs.add(mapRow(res));
        }
        return packs;
    }

    public List<Pack> getActivePacks() throws SQLException {
        String req = "SELECT * FROM `packs` WHERE `status` = 'ACTIVE'";
        ResultSet res = conx.createStatement().executeQuery(req);
        List<Pack> packs = new ArrayList<>();
        while (res.next()) {
            packs.add(mapRow(res));
        }
        return packs;
    }

    public Pack getById(int id) throws SQLException {
        PreparedStatement pstm = conx.prepareStatement("SELECT * FROM `packs` WHERE `id_pack` = ?");
        pstm.setInt(1, id);
        ResultSet res = pstm.executeQuery();
        if (res.next()) return mapRow(res);
        return null;
    }

    public List<Pack> getByCategory(int categoryId) throws SQLException {
        PreparedStatement pstm = conx.prepareStatement("SELECT * FROM `packs` WHERE `category_id` = ?");
        pstm.setInt(1, categoryId);
        ResultSet res = pstm.executeQuery();
        List<Pack> packs = new ArrayList<>();
        while (res.next()) packs.add(mapRow(res));
        return packs;
    }

    private Pack mapRow(ResultSet res) throws SQLException {
        return new Pack(
                res.getInt("id_pack"),
                res.getString("title"),
                res.getString("description"),
                res.getInt("destination_id"),
                res.getInt("accommodation_id"),
                res.getInt("activity_id"),
                res.getInt("transport_id"),
                res.getInt("category_id"),
                res.getInt("duration_days"),
                res.getBigDecimal("base_price"),
                Pack.Status.valueOf(res.getString("status")),
                res.getTimestamp("created_at") != null
                        ? res.getTimestamp("created_at").toLocalDateTime()
                        : LocalDateTime.now()
        );
    }
}
