package com.MAYA.MAYA.Entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class user {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   @Column(name = "id")
   private long id;

   @Column(name = "email", nullable = false)
   private String email;

   @Column(name = "name", nullable = false)
   private String name;

   @Column(name = "password", nullable = false)
   private String password;

   @Column(name = "created_at", updatable = false)
   @CreationTimestamp
   private LocalDateTime createdDate;

   @Enumerated(EnumType.STRING)
   @Column(name = "role", nullable = false)
   private Role role;

   public Role getRole() {
      return role;
   }

   public void setRole(Role role) {
      this.role = role;
   }

   public long getUserId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public String getPassword() {
      return password;
   }

   public String getEmail(){return email;}

   public void setUserId(long userId) {
      id = userId;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public void setEmail(String email){this.email= email;}


}
