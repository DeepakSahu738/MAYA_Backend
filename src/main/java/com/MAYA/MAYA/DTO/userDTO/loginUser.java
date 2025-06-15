package com.MAYA.MAYA.DTO.userDTO;
import jakarta.validation.constraints.NotBlank;

public class loginUser {
    @NotBlank(message = "email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Getters and Setters


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Constructor
    public loginUser() {}

    public loginUser(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
