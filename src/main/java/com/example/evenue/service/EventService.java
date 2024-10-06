package com.example.evenue.service;

import com.example.evenue.models.events.EventDao;
import com.example.evenue.models.events.EventModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    private EventDao eventDao;

    // Method to add an event
    public EventModel addEvent(EventModel event) {
        return eventDao.save(event);
    }

    public List<EventModel> getAllEvents() {
        return eventDao.findAll();
    }

    public EventModel getEventById(Long eventId) {
        return eventDao.findById(eventId).orElse(null);
    }

    // Implement filter logic
    public List<EventModel> filterEvents(Long categoryId, String search, String sortBy) {
        // Default sorting by event date in descending order
        Sort sort = Sort.by(Sort.Direction.DESC, "eventDate");

        // If a valid sortBy field is provided, set ascending sort for that field
        if (sortBy != null && !sortBy.isEmpty()) {
            sort = Sort.by(Sort.Direction.ASC, sortBy);
        }

        // Filter by category and search text with sorting
        if (categoryId != null && categoryId > 0) {
            return eventDao.findByCategoryAndSearch(categoryId, search, sort);
        }
        // Filter by search text with sorting
        else {
            return eventDao.findBySearch(search, sort);
        }
    }

    // Method to find an event by name
    public EventModel findByEventName(String eventName) {
        return eventDao.findByEventName(eventName).orElse(null); // Use Optional to avoid returning null
    }
}
