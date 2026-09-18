package com.softpro.ATH.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.softpro.ATH.Model.Formers;

public interface FormersRepo extends JpaRepository<Formers, Long>{

	boolean existsByEmail(String email);

	List<Formers> findAllByStatus(String string);

	Formers findFormerByEmail(String email);
	
	Optional<Formers> findByEmail(String email);
}
