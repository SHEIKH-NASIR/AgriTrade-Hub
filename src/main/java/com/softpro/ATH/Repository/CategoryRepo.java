package com.softpro.ATH.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.softpro.ATH.Model.Category;

public interface CategoryRepo extends JpaRepository<Category, Long>{

	boolean existsByCategory(String cate);

}
