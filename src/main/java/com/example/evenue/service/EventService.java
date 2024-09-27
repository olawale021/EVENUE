package com.example.evenue.service;

import com.example.evenue.models.events.EventDao;
import com.example.evenue.models.events.EventModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    @Autowired
    private EventDao eventDao;

    // Method to add an event
    public EventModel addEvent(EventModel event) {
        return eventDao.save(event);
    }
}
