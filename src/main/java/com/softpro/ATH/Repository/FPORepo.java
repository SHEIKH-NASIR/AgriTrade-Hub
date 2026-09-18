
package com.softpro.ATH.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.softpro.ATH.Model.FPO;
import com.softpro.ATH.Model.Formers;

@Repository
public interface FPORepo extends JpaRepository<FPO, Long> {

    Optional<FPO> findByEmail(String email);

    List<FPO> findAllByStatus(String status);

    List<FPO> findByMembersContaining(Formers farmer);

}

