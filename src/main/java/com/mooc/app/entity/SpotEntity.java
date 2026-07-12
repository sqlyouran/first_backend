package com.mooc.app.entity;

import com.mooc.app.converter.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "spots")
public class SpotEntity extends BaseEntity {

    @NotBlank(message = "Name must not be blank")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 200, message = "NameZh must not exceed 200 characters")
    @Column(name = "name_zh")
    private String nameZh;

    @NotBlank(message = "Slug must not be blank")
    @Size(max = 220, message = "Slug must not exceed 220 characters")
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_zh", columnDefinition = "TEXT")
    private String descriptionZh;

    @Size(max = 2048, message = "Cover image URL must not exceed 2048 characters")
    @Column(name = "cover_image")
    private String coverImage;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> gallery = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags = new ArrayList<>();

    @NotNull(message = "City ID must not be null")
    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Size(max = 100, message = "City name must not exceed 100 characters")
    @Column(name = "city_name")
    private String cityName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotStatus status = SpotStatus.PUBLISHED;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "view_count")
    private int viewCount = 0;

    @Column(name = "bookmark_count")
    private int bookmarkCount = 0;

    @Size(max = 200, message = "Ticket price must not exceed 200 characters")
    @Column(name = "ticket_price", length = 200)
    private String ticketPrice;

    @Size(max = 500, message = "Opening hours must not exceed 500 characters")
    @Column(name = "opening_hours", length = 500)
    private String openingHours;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(length = 500)
    private String address;

    @Column(name = "data_refreshed_at")
    private Instant dataRefreshedAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDescriptionZh() { return descriptionZh; }
    public void setDescriptionZh(String descriptionZh) { this.descriptionZh = descriptionZh; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public List<String> getGallery() { return gallery; }
    public void setGallery(List<String> gallery) { this.gallery = gallery != null ? gallery : new ArrayList<>(); }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public SpotStatus getStatus() { return status; }
    public void setStatus(SpotStatus status) { this.status = status; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getBookmarkCount() { return bookmarkCount; }
    public void setBookmarkCount(int bookmarkCount) { this.bookmarkCount = bookmarkCount; }

    public String getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(String ticketPrice) { this.ticketPrice = ticketPrice; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Instant getDataRefreshedAt() { return dataRefreshedAt; }
    public void setDataRefreshedAt(Instant dataRefreshedAt) { this.dataRefreshedAt = dataRefreshedAt; }
}
