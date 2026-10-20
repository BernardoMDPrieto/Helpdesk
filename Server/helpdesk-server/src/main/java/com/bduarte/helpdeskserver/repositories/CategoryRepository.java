package com.bduarte.helpdeskserver.repositories;

import com.bduarte.helpdeskserver.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);
}
