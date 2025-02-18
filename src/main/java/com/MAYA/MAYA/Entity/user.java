package com.MAYA.MAYA.Entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString

public class user {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long UserId;

   private String name;

   private String password;

}
