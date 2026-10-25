package cl.duoc.museo.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Producto o curso del catálogo, con precio ficticio. */
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank private String name;
    @NotBlank @Pattern(regexp = "CURSO|LIBRO|MATERIAL|ARTICULO") private String category;
    @NotBlank @Column(length = 2000) private String description;
    @NotNull @Positive private Double price;
    private String imageUrl = "";
    @NotNull @Min(0) private Integer stock;
    private boolean active = true;
    private String qrCode;
    private Double rating = 0.0;

    public Product() {}

    public Product(String name, String category, String description, double price, int stock, double rating) {
        this.name = name; this.category = category; this.description = description;
        this.price = price; this.stock = stock; this.rating = rating;
    }

    @JsonIgnore
    public boolean isCourse() { return "CURSO".equals(category); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}
