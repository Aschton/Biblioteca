package cl.duoc.museo.api.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** Pedido o solicitud. El backend centraliza el pedido y su estado. */
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Embeddable
    public static class Item {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double unitPrice;

        public Item() {}
        public Item(Long productId, String productName, Integer quantity, Double unitPrice) {
            this.productId = productId; this.productName = productName; this.quantity = quantity; this.unitPrice = unitPrice;
        }
        public Long getProductId() { return productId; }
        public String getProductName() { return productName; }
        public Integer getQuantity() { return quantity; }
        public Double getUnitPrice() { return unitPrice; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String userEmail;
    private String userName;
    private Long createdAt = System.currentTimeMillis();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_item", joinColumns = @JoinColumn(name = "order_id"))
    private List<Item> items = new ArrayList<>();

    private Double subtotal = 0.0;
    private Double shipping = 0.0;
    private Double total = 0.0;
    private String status = "PENDIENTE";
    private String country;
    private String shippingType;
    private String kind = "COMPRA";

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Long getCreatedAt() { return createdAt; }
    public List<Item> getItems() { return items; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public Double getShipping() { return shipping; }
    public void setShipping(Double shipping) { this.shipping = shipping; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getShippingType() { return shippingType; }
    public void setShippingType(String shippingType) { this.shippingType = shippingType; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
}
