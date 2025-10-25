package co.edu.unbosque.tripservice.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "bicycle_id", nullable = false)
    private Bicycle bicycle;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "reserved_at")
    private java.time.LocalDateTime reservedAt = java.time.LocalDateTime.now();

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;

    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, CANCELLED, USED

    public Reservation() {
    }

    public Reservation(LocalDateTime expiresAt, Bicycle bicycle, LocalDateTime reservedAt, Station station, String status, Long userId) {
        this.expiresAt = expiresAt;
        this.bicycle = bicycle;
        this.reservedAt = reservedAt;
        this.station = station;
        this.status = status;
        this.userId = userId;
    }

    public Bicycle getBicycle() {
        return bicycle;
    }

    public void setBicycle(Bicycle bicycle) {
        this.bicycle = bicycle;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
