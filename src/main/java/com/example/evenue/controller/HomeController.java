package com.example.evenue.controller;

import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.events.EventDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private EventDao eventDao;

    @GetMapping("/")
    public String home(Model model) {
        // Fetch all events from the database
        List<EventModel> events = eventDao.findAll();

        // Add the events list to the model to be used in the view
        model.addAttribute("events", events);

        return "index"; // Returns the "index.html" view from the templates folder
    }
}
