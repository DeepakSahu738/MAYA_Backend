package com.MAYA.MAYA.Entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class user {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   private String name;

   private String password;

   public long getUserId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public String getPassword() {
      return password;
   }

   public void setUserId(long userId) {
      id = userId;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setPassword(String password) {
      this.password = password;
   }
}
