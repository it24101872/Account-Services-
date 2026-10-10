package lk.sliit.ridelink.drivvehicle.repository;

import lk.sliit.ridelink.drivvehicle.entity.Vehicle;
import lk.sliit.ridelink.drivvehicle.enums.VehicleStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    List<Vehicle> findByDriverId(String driverId);
    Optional<Vehicle> findByDriverIdAndStatus(String driverId, VehicleStatus status);
    boolean existsByRegistrationNumber(String registrationNumber);
}
