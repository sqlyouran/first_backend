package com.mooc.app.repository;

import com.mooc.app.entity.CityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CityRepository extends JpaRepository<CityEntity, UUID> {

    Page<CityEntity> findByDeletedFalse(Pageable pageable);

    Optional<CityEntity> findByIdAndDeletedFalse(UUID id);

    Optional<CityEntity> findBySlugAndDeletedFalse(String slug);
}
