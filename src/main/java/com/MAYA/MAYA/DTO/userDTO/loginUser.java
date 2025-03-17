package com.MAYA.MAYA.DTO.userDTO;
import jakarta.validation.constraints.NotBlank;

public class loginUser {
    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "Password is required")
    private String password;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Constructor
    public loginUser() {}

    public loginUser(String name, String password) {
        this.name = name;
        this.password = password;
    }
}
