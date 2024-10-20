package com.example.evenue.models.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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


}
