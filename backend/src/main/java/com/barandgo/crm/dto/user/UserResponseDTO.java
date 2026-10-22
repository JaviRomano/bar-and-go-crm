package com.barandgo.crm.dto.user;

import com.barandgo.crm.entity.User;
import com.barandgo.crm.enums.StaffTag;
import com.barandgo.crm.enums.UserRole;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

public class UserResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private Set<StaffTag> tags = EnumSet.noneOf(StaffTag.class);
    private LocalDateTime dateOfCreation;

    public UserResponseDTO() {
    }

    // No incluye el password por seguridad
    public static UserResponseDTO from(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setTags(user.getTags().isEmpty() ? EnumSet.noneOf(StaffTag.class) : EnumSet.copyOf(user.getTags()));
        dto.setDateOfCreation(user.getDateOfCreation());
        return dto;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public Set<StaffTag> getTags() {
        return tags;
    }
    
    public void setTags(Set<StaffTag> tags) {
        this.tags = tags;
    }
    
    public LocalDateTime getDateOfCreation() {
        return dateOfCreation;
    }
    
    public void setDateOfCreation(LocalDateTime dateOfCreation) {
        this.dateOfCreation = dateOfCreation;
    }
}