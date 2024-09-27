package com.example.evenue.models.events;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCategoryDao extends JpaRepository<EventCategory, Long> {
}