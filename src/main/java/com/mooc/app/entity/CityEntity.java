package com.mooc.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "cities")
public class CityEntity extends BaseEntity {

    @NotBlank(message = "Name must not be blank")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 100, message = "NameZh must not exceed 100 characters")
    @Column(name = "name_zh")
    private String nameZh;

    @NotBlank(message = "Slug must not be blank")
    @Size(max = 120, message = "Slug must not exceed 120 characters")
    @Column(nullable = false, unique = true)
    private String slug;

    @Size(max = 2048, message = "Cover image URL must not exceed 2048 characters")
    @Column(name = "cover_image")
    private String coverImage;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 50, message = "Best season must not exceed 50 characters")
    @Column(name = "best_season")
    private String bestSeason;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBestSeason() { return bestSeason; }
    public void setBestSeason(String bestSeason) { this.bestSeason = bestSeason; }
}
