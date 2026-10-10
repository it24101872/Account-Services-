package lk.sliit.ridelink.drivvehicle.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lk.sliit.ridelink.drivvehicle.enums.AvailabilityStatus;
import lk.sliit.ridelink.drivvehicle.enums.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    private String driverId;

    @Field("accountId")
    private String accountId;

    @Field("licenseNumber")
    private String licenseNumber;

    @Field("fullName")
    private String fullName;

    @Field("phone")
    private String phone;

    @Field("driverStatus")
    private DriverStatus driverStatus;

    @Field("availabilityStatus")
    private AvailabilityStatus availabilityStatus;

    @Field("serviceArea")
    private String serviceArea;

    @Field("latitude")
    private BigDecimal latitude;

    @Field("longitude")
    private BigDecimal longitude;

    @Field("locationUpdatedAt")
    private LocalDateTime locationUpdatedAt;

    @Field("createdAt")
    private LocalDateTime createdAt;

    @Field("updatedAt")
    private LocalDateTime updatedAt;

    // Generate IDs and timestamps on creation
    public void generateIdAndTimestamps() {
        if (this.driverId == null) {
            this.driverId = "D-" + UUID.randomUUID().toString().substring(0, 4);
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

