package org.example.telegram_bot.repo;

import java.util.Optional;
import org.example.telegram_bot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepoPostgres extends JpaRepository<User, Long> {
  Optional<User> findById (Long id);
}
