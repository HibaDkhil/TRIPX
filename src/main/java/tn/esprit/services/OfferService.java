package tn.esprit.services;

import tn.esprit.entities.Offer;
import tn.esprit.utils.MyDB;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OfferService implements ICRUD<Offer> {

    private Connection conx;

    public OfferService() {
        conx = MyDB.getInstance().getConx();
    }

    @Override
    public void add(Offer offer) throws SQLException {
        String req = "INSERT INTO `offers`(`title`, `description`, `discount_type`, `discount_value`, "
                + "`pack_id`, `destination_id`, `accommodation_id`, `start_date`, `end_date`, `is_active`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setString(1, offer.getTitle());
        pstm.setString(2, offer.getDescription());
        pstm.setString(3, offer.getDiscountType().name());
        pstm.setBigDecimal(4, offer.getDiscountValue());

        // Handle nullable FKs
        if (offer.getPackId() != null) pstm.setInt(5, offer.getPackId());
        else pstm.setNull(5, Types.INTEGER);

        if (offer.getDestinationId() != null) pstm.setLong(6, offer.getDestinationId());  // Fixed: Long
        else pstm.setNull(6, Types.BIGINT);  // Fixed: BIGINT

        if (offer.getAccommodationId() != null) pstm.setInt(7, offer.getAccommodationId());
        else pstm.setNull(7, Types.INTEGER);

        pstm.setDate(8, Date.valueOf(offer.getStartDate()));
        pstm.setDate(9, Date.valueOf(offer.getEndDate()));
        pstm.setBoolean(10, offer.isActive());
        pstm.executeUpdate();
        System.out.println("Offer ajoutée (méthode 2): " + offer.getTitle());
    }


    @Override
    public void modifier(Offer offer) throws SQLException {
        String req = "UPDATE `offers` SET `title` = ?, `description` = ?, `discount_type` = ?, "
                + "`discount_value` = ?, `pack_id` = ?, `destination_id` = ?, `accommodation_id` = ?, "
                + "`start_date` = ?, `end_date` = ?, `is_active` = ? WHERE `id_offer` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setString(1, offer.getTitle());
        pstm.setString(2, offer.getDescription());
        pstm.setString(3, offer.getDiscountType().name());
        pstm.setBigDecimal(4, offer.getDiscountValue());

        if (offer.getPackId() != null) pstm.setInt(5, offer.getPackId());
        else pstm.setNull(5, Types.INTEGER);

        if (offer.getDestinationId() != null) pstm.setLong(6, offer.getDestinationId());  // Fixed: Long
        else pstm.setNull(6, Types.BIGINT);  // Fixed: BIGINT

        if (offer.getAccommodationId() != null) pstm.setInt(7, offer.getAccommodationId());
        else pstm.setNull(7, Types.INTEGER);

        pstm.setDate(8, Date.valueOf(offer.getStartDate()));
        pstm.setDate(9, Date.valueOf(offer.getEndDate()));
        pstm.setBoolean(10, offer.isActive());
        pstm.setInt(11, offer.getIdOffer());
        pstm.executeUpdate();
        System.out.println("Offer modifiée: " + offer.getTitle());
    }

    @Override
    public void delete(Offer offer) throws SQLException {
        PreparedStatement pstm = conx.prepareStatement("DELETE FROM `offers` WHERE `id_offer` = ?");
        pstm.setInt(1, offer.getIdOffer());
        pstm.executeUpdate();
        System.out.println("Offer supprimée: id=" + offer.getIdOffer());
    }

    @Override
    public List<Offer> afficherList() throws SQLException {
        ResultSet res = conx.createStatement().executeQuery("SELECT * FROM `offers`");
        List<Offer> offers = new ArrayList<>();
        while (res.next()) offers.add(mapRow(res));
        return offers;
    }

    // ---- Active offers valid today ----
    public List<Offer> getActiveOffers() throws SQLException {
        String req = "SELECT * FROM `offers` WHERE `is_active` = TRUE AND CURDATE() BETWEEN `start_date` AND `end_date`";
        ResultSet res = conx.createStatement().executeQuery(req);
        List<Offer> offers = new ArrayList<>();
        while (res.next()) offers.add(mapRow(res));
        return offers;
    }

    // ---- Active offer for a specific pack ----
    public Offer getActiveOfferByPackId(int packId) throws SQLException {
        String req = "SELECT * FROM `offers` WHERE `pack_id` = ? AND `is_active` = TRUE "
                + "AND CURDATE() BETWEEN `start_date` AND `end_date` LIMIT 1";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setInt(1, packId);
        ResultSet res = pstm.executeQuery();
        if (res.next()) return mapRow(res);
        return null;
    }

    // ---- Active offer for a specific destination ----
    public Offer getActiveOfferByDestinationId(long destinationId) throws SQLException {  // Fixed: long parameter
        String req = "SELECT * FROM `offers` WHERE `destination_id` = ? AND `is_active` = TRUE "
                + "AND CURDATE() BETWEEN `start_date` AND `end_date` LIMIT 1";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setLong(1, destinationId);  // Fixed: setLong
        ResultSet res = pstm.executeQuery();
        if (res.next()) return mapRow(res);
        return null;
    }

    private Offer mapRow(ResultSet res) throws SQLException {
        // Handle nullable FK columns
        Integer packId          = res.getObject("pack_id") != null ? res.getInt("pack_id") : null;
        Long destinationId      = res.getObject("destination_id") != null ? res.getLong("destination_id") : null;  // Fixed: Long
        Integer accommodationId = res.getObject("accommodation_id") != null ? res.getInt("accommodation_id") : null;

        return new Offer(
                res.getInt("id_offer"),
                res.getString("title"),
                res.getString("description"),
                Offer.DiscountType.valueOf(res.getString("discount_type")),
                res.getBigDecimal("discount_value"),
                packId,
                destinationId,
                accommodationId,
                res.getDate("start_date").toLocalDate(),
                res.getDate("end_date").toLocalDate(),
                res.getBoolean("is_active")
        );
    }
}
