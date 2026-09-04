package com.marsmars.repositories;

import com.marsmars.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByUsername(String username);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"orders.items", "roles"})
    Optional<User> findWithDetailsById(Long id);
}
