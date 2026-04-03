package ase_pr_inso_01.user_service.repository;

import ase_pr_inso_01.user_service.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findUserByEmail(String email);
  boolean existsByEmail(String email);
}
