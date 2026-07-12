package com.mooc.app.repository;

import com.mooc.app.entity.SpotEntity;
import com.mooc.app.entity.SpotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotRepository extends JpaRepository<SpotEntity, UUID> {

    Page<SpotEntity> findByDeletedFalse(Pageable pageable);

    Page<SpotEntity> findByCityIdAndDeletedFalse(UUID cityId, Pageable pageable);

    @Query("SELECT s FROM SpotEntity s JOIN CityEntity c ON s.cityId = c.id WHERE c.slug = :citySlug AND s.deleted = false AND s.status = 'PUBLISHED'")
    Page<SpotEntity> findByCitySlugAndDeletedFalse(@Param("citySlug") String citySlug, Pageable pageable);

    Optional<SpotEntity> findByIdAndDeletedFalse(UUID id);

    Optional<SpotEntity> findBySlugAndDeletedFalse(String slug);

    Page<SpotEntity> findByStatusAndDeletedFalse(SpotStatus status, Pageable pageable);

    List<SpotEntity> findAllByIdInAndStatusAndDeletedFalse(List<UUID> ids, SpotStatus status);

    @Query("SELECT s FROM SpotEntity s WHERE s.deleted = false AND s.status = 'PUBLISHED' AND " +
            "(LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.nameZh) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.descriptionZh) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(s.cityName) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<SpotEntity> searchByKeyword(@Param("q") String query);

    @Query("SELECT s FROM SpotEntity s WHERE s.deleted = false AND s.status = 'PUBLISHED' " +
            "AND (s.dataRefreshedAt IS NULL OR s.dataRefreshedAt < :cutoff) " +
            "ORDER BY s.dataRefreshedAt ASC NULLS FIRST")
    List<SpotEntity> findStaleSpots(@Param("cutoff") Instant cutoff);
}
