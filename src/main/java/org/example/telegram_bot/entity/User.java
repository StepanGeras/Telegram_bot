package org.example.telegram_bot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

  @Id
  private Long id;

  private String utm;
  private boolean consent;
  private String fullName;
  private String birthDate;
  private String gender;
  private String imageUrl;

}
