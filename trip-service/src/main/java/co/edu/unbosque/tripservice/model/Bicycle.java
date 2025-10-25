package co.edu.unbosque.tripservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bicycle")
public class Bicycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 20)
    private String type; // MECHANICAL, ELECTRIC

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(length = 20)
    private String status = "AVAILABLE"; // AVAILABLE, IN_USE, MAINTENANCE, RESERVED

    @ManyToOne
    @JoinColumn(name = "last_station_id")
    private Station lastStation;

    public Bicycle() {
    }

    public Bicycle(String code, Integer batteryLevel, Station lastStation, String status, String type) {
        this.code = code;
        this.batteryLevel = batteryLevel;
        this.lastStation = lastStation;
        this.status = status;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Station getLastStation() {
        return lastStation;
    }

    public void setLastStation(Station lastStation) {
        this.lastStation = lastStation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}