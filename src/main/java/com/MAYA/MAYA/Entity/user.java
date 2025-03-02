package com.MAYA.MAYA.Entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter

public class user {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long UserId;

   private String name;

   private String password;

   public long getUserId() {
      return UserId;
   }

   public String getName() {
      return name;
   }

   public String getPassword() {
      return password;
   }

   public void setUserId(long userId) {
      UserId = userId;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setPassword(String password) {
      this.password = password;
   }
}
