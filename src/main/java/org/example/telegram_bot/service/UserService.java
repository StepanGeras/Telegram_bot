package org.example.telegram_bot.service;

import java.util.Optional;
import org.example.telegram_bot.entity.User;
import org.example.telegram_bot.repo.UserRepoPostgres;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepoPostgres userRepoPostgres;

  @Autowired
  public UserService(UserRepoPostgres userRepoPostgres) {
    this.userRepoPostgres = userRepoPostgres;
  }

  public void save(User user) {
    userRepoPostgres.save(user);
  }

  public User findById(Long id) {
    Optional<User> user = userRepoPostgres.findById(id);

    if (user.isPresent()) {
      return user.get();
    }

    User newUser = new User();
    newUser.setId(id);
    save(newUser);

    return newUser;

  }

}
