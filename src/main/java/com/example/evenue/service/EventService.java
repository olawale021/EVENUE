package com.example.evenue.service;

import com.example.evenue.models.events.EventDao;
import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.tickets.TicketDao;
import com.example.evenue.models.users.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    private EventDao eventDao;

    @Autowired
    private TicketDao ticketDao;

    // Method to add an event
    public EventModel addEvent(EventModel event) {
        return eventDao.save(event);
    }

    public Page<EventModel> getAllEvents(Pageable pageable) {
        return eventDao.findAll(pageable);
    }

    public Optional<EventModel> getEventById(Long eventId) {
        return eventDao.findById(eventId); // Use Optional here
    }

    // Implement filter logic
    public Page<EventModel> filterEvents(Long categoryId, String search, Pageable pageable) {
        if (categoryId != null && categoryId > 0) {
            return eventDao.findByCategoryAndSearch(categoryId, search, pageable);
        }
        return eventDao.findBySearch(search, pageable);
    }

    // Method to find an event by name
    public Optional<EventModel> findByEventName(String eventName) {
        return eventDao.findByEventName(eventName); // Use Optional to avoid returning null
    }

    // Fetch events by tickets the user has
    public List<EventModel> getEventsByUser(UserModel user) {
        return ticketDao.findEventsByUser(user);
    }
}
