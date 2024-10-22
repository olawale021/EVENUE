package com.example.evenue.models.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventDao extends JpaRepository<EventModel, Long> {

    // Custom query for filtering by category and search text with pagination
    @Query("SELECT e FROM EventModel e WHERE (:categoryId IS NULL OR e.eventCategory.id = :categoryId) AND " +
            "(LOWER(e.eventName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(e.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EventModel> findByCategoryAndSearch(@Param("categoryId") Long categoryId,
                                             @Param("search") String search, Pageable pageable);

    // Custom query for filtering by search text with pagination
    @Query("SELECT e FROM EventModel e " +
            "WHERE (LOWER(e.eventName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EventModel> findBySearch(@Param("search") String search, Pageable pageable);

    // Default method to find events with pagination
    Page<EventModel> findAll(Pageable pageable);

    // Method to find events by organizer ID
    List<EventModel> findByOrganizerId(Long organizerId);

//    Optional<EventModel> findByEventName(String eventName);

    Optional<EventModel> findByEventName(String eventName);

    // Fetch all events without pagination
    @Query("SELECT e FROM EventModel e")
    List<EventModel> findAllEvents();

    @Query("SELECT DISTINCT e FROM EventModel e " +
            "LEFT JOIN e.ticketTypes t " +
            "WHERE (COALESCE(:categories, NULL) IS NULL OR e.eventCategory.id IN :categories) " +
            "AND (:startDate IS NULL OR e.eventDate BETWEEN :startDate AND :endDate) " +
            "AND (:minPrice IS NULL OR t.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR t.price <= :maxPrice)")
    Page<EventModel> findByFilters(
            @Param("categories") List<Long> categories,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);


}
