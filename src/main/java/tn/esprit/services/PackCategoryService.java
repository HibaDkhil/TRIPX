package tn.esprit.services;

import tn.esprit.entities.PackCategory;
import tn.esprit.utils.MyDB;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackCategoryService implements ICRUD<PackCategory> {

    private Connection conx;

    public PackCategoryService() {
        conx = MyDB.getInstance().getConx();
    }

    @Override
    public void add(PackCategory category) throws SQLException {
        String req = "INSERT INTO `pack_categories`(`name`) VALUES (?)";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setString(1, category.getName());
        pstm.executeUpdate();
        System.out.println("PackCategory ajoutée (méthode 2): " + category.getName());
    }


    @Override
    public void modifier(PackCategory category) throws SQLException {
        String req = "UPDATE `pack_categories` SET `name` = ? WHERE `id_category` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setString(1, category.getName());
        pstm.setInt(2, category.getIdCategory());
        pstm.executeUpdate();
        System.out.println("PackCategory modifiée: " + category.getName());
    }

    @Override
    public void delete(PackCategory category) throws SQLException {
        String req = "DELETE FROM `pack_categories` WHERE `id_category` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setInt(1, category.getIdCategory());
        pstm.executeUpdate();
        System.out.println("PackCategory supprimée: id=" + category.getIdCategory());
    }

    @Override
    public List<PackCategory> afficherList() throws SQLException {
        String req = "SELECT * FROM `pack_categories`";
        Statement stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);

        List<PackCategory> categories = new ArrayList<>();
        while (res.next()) {
            categories.add(new PackCategory(
                    res.getInt("id_category"),
                    res.getString("name")
            ));
        }
        return categories;
    }

    public PackCategory getById(int id) throws SQLException {
        String req = "SELECT * FROM `pack_categories` WHERE `id_category` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setInt(1, id);
        ResultSet res = pstm.executeQuery();
        if (res.next()) {
            return new PackCategory(res.getInt("id_category"), res.getString("name"));
        }
        return null;
    }
}
