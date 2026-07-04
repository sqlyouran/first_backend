package com.mooc.app.repository;

import com.mooc.app.entity.CityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CityRepository extends JpaRepository<CityEntity, UUID> {

    Page<CityEntity> findByDeletedFalse(Pageable pageable);

    Optional<CityEntity> findByIdAndDeletedFalse(UUID id);

    Optional<CityEntity> findBySlugAndDeletedFalse(String slug);

    @Query("SELECT c FROM CityEntity c WHERE c.deleted = false AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.nameZh) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<CityEntity> searchByKeyword(@Param("q") String query);
}
