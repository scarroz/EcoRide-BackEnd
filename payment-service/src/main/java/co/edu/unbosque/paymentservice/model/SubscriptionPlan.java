package co.edu.unbosque.paymentservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;
    private Integer maxTrips;
    private Boolean active = true;

    public SubscriptionPlan() {}
    public SubscriptionPlan(Long id, String name, Double price, Integer maxTrips, Boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.maxTrips = maxTrips;
        this.active = active;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMaxTrips() {
        return maxTrips;
    }

    public void setMaxTrips(Integer maxTrips) {
        this.maxTrips = maxTrips;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
