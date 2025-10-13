package co.edu.unbosque.userservice.dto;

public record UserProfileDTO( Long id,
                              String phone,
                              String address,
                              String city,
                              String country) {
}
