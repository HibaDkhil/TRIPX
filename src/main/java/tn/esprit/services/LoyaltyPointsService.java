package tn.esprit.services;

import tn.esprit.entities.LoyaltyPoints;
import tn.esprit.utils.MyDB;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoyaltyPointsService implements ICRUD<LoyaltyPoints> {

    private static final int POINTS_PER_TRIP  = 50;
    private static final int POINTS_PER_LEVEL = 200;

    private Connection conx;

    public LoyaltyPointsService() {
        conx = MyDB.getInstance().getConx();
    }

    @Override
    public void add(LoyaltyPoints lp) throws SQLException {
        String req = "INSERT INTO `loyalty_points`(`user_id`, `total_points`, `level`) VALUES (?, ?, ?)";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setInt(1, lp.getUserId());
        pstm.setInt(2, lp.getTotalPoints());
        pstm.setString(3, lp.computeLevel().name());
        pstm.executeUpdate();
        System.out.println("LoyaltyPoints ajoutés pour user id=" + lp.getUserId());
    }

    @Override
    public void modifier(LoyaltyPoints lp) throws SQLException {
        // Also update the level column based on new points total
        String req = "UPDATE `loyalty_points` SET `total_points` = ?, `level` = ? WHERE `id` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setInt(1, lp.getTotalPoints());
        pstm.setString(2, lp.computeLevel().name());
        pstm.setInt(3, lp.getId());
        pstm.executeUpdate();
        System.out.println("LoyaltyPoints modifiés → points=" + lp.getTotalPoints()
                + ", level=" + lp.computeLevel());
    }

    @Override
    public void delete(LoyaltyPoints lp) throws SQLException {
        PreparedStatement pstm = conx.prepareStatement("DELETE FROM `loyalty_points` WHERE `id` = ?");
        pstm.setInt(1, lp.getId());
        pstm.executeUpdate();
        System.out.println("LoyaltyPoints supprimés: id=" + lp.getId());
    }

    @Override
    public List<LoyaltyPoints> afficherList() throws SQLException {
        ResultSet res = conx.createStatement().executeQuery("SELECT * FROM `loyalty_points`");
        List<LoyaltyPoints> list = new ArrayList<>();
        while (res.next()) list.add(mapRow(res));
        return list;
    }

    // ---- Get by user ID ----
    public LoyaltyPoints getByUserId(int userId) throws SQLException {
        PreparedStatement pstm = conx.prepareStatement(
                "SELECT * FROM `loyalty_points` WHERE `user_id` = ?");
        pstm.setInt(1, userId);
        ResultSet res = pstm.executeQuery();
        if (res.next()) return mapRow(res);
        return null;
    }

    // ---- Add 50 points after a trip ----
    public void addTripPoints(int userId) throws SQLException {
        LoyaltyPoints lp = getByUserId(userId);
        if (lp == null) {
            LoyaltyPoints newLp = new LoyaltyPoints(userId);
            newLp.setTotalPoints(POINTS_PER_TRIP);
            add(newLp);
            System.out.println("Nouveau record créé avec " + POINTS_PER_TRIP + " pts pour user id=" + userId);
        } else {
            lp.setTotalPoints(lp.getTotalPoints() + POINTS_PER_TRIP);
            modifier(lp);
            System.out.println("+" + POINTS_PER_TRIP + " pts → total: " + lp.getTotalPoints()
                    + " | Level: " + lp.computeLevel()
                    + " | Discount: " + lp.getLoyaltyDiscountPercent() + "%");
        }
    }

    // ---- Calculate final price: offer discount + loyalty discount stacked ----
    public double calculateFinalPrice(double basePrice, int userId, double offerDiscountPercent) throws SQLException {
        LoyaltyPoints lp = getByUserId(userId);
        double loyaltyDiscount = (lp != null) ? lp.getLoyaltyDiscountPercent() : 0;
        double totalDiscount = offerDiscountPercent + loyaltyDiscount;
        return Math.max(basePrice - (basePrice * totalDiscount / 100), 0);
    }

    private LoyaltyPoints mapRow(ResultSet res) throws SQLException {
        return new LoyaltyPoints(
                res.getInt("id"),
                res.getInt("user_id"),
                res.getInt("total_points"),
                LoyaltyPoints.Level.valueOf(res.getString("level")),
                res.getTimestamp("created_at") != null
                        ? res.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now(),
                res.getTimestamp("updated_at") != null
                        ? res.getTimestamp("updated_at").toLocalDateTime() : LocalDateTime.now()
        );
    }
}
