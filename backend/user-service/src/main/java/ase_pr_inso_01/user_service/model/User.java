package ase_pr_inso_01.user_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.Date;

@Document(collection = "users")
@Getter
@Setter
public class User {
  @Id
  private String id;
  private String email;
  private String firstName;
  private String lastName;
  private String password;
  private String password2;
  @Field(write = Field.Write.ALWAYS)
  private LocalDate deleted_at;
  private byte[] profileImageBlob;
}
