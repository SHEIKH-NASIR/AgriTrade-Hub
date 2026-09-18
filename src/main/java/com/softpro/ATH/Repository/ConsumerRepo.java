package com.softpro.ATH.Repository;

import com.softpro.ATH.Model.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsumerRepo extends JpaRepository<Consumer, Long> {

    Optional<Consumer> findByEmail(String email);

    boolean existsByEmail(String email);
}