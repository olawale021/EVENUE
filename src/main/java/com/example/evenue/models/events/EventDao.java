package com.example.evenue.models.events;

import com.example.evenue.models.events.EventModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDao extends JpaRepository<EventModel, Long> {
    // The save method is inherited from JpaRepository, no need to define it again
}
