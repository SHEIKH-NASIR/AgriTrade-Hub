package com.softpro.ATH.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.softpro.ATH.Model.Merchant;

public interface MerchantRepo extends JpaRepository<Merchant, Long>{

	boolean existsByEmail(String email);

	List<Merchant> findAllByStatus(String string);

	Merchant findByEmail(String email);

}
