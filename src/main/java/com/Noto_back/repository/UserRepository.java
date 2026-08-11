package com.Noto_back.repository;

import com.Noto_back.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    //Utiliser par Spring security pour charger le UserDetails

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
}
