package co.edu.unbosque.userservice.dto;

public record UserProfileRequestDTO(Long id, String phone,
                                    String address,
                                    String city,
                                    String country) {
}
