package lk.sliit.ridelink.drivvehicle.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lk.sliit.ridelink.drivvehicle.enums.VehicleStatus;
import lk.sliit.ridelink.drivvehicle.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    private String vehicleId;

    @Field("driverId")
    private String driverId;

    @Field("registrationNumber")
    private String registrationNumber;

    @Field("make")
    private String make;

    @Field("model")
    private String model;

    @Field("year")
    private int year;

    @Field("color")
    private String color;

    @Field("vehicleType")
    private VehicleType vehicleType;

    @Field("capacity")
    private int capacity;

    @Field("status")
    private VehicleStatus status;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("updatedAt")
    private LocalDateTime updatedAt;

    // Generate IDs and timestamps on creation
    public void generateIdAndTimestamps() {
        if (this.vehicleId == null) {
            this.vehicleId = "V-" + UUID.randomUUID().toString().substring(0, 4);
        }
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Update timestamp on modifications
    public void refreshUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
