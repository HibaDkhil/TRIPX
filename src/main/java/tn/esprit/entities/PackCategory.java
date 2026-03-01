package tn.esprit.entities;

public class PackCategory {

    private int idCategory;
    private String name;

    // ---- Constructor with id (reading from DB) ----
    public PackCategory(int idCategory, String name) {
        this.idCategory = idCategory;
        this.name       = name;
    }

    // ---- Constructor without id (inserting) ----
    public PackCategory(String name) {
        this.name = name;
    }

    // ---- Getters & Setters ----
    public int getIdCategory()              { return idCategory; }
    public void setIdCategory(int id)       { this.idCategory = id; }

    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }

    @Override
    public String toString() {
        return "PackCategory{" +
                "idCategory=" + idCategory +
                ", name='" + name + '\'' +
                '}';
    }
}
