package com.example.evenue.controller.user.organizer;

import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.models.users.UserDao;
import com.example.evenue.models.events.EventDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/organizer")
public class OrganizerController {

    @Autowired
    private EventDao eventDao;

    @Autowired
    private UserDao userDao;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get the currently authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserModel organizer = userDao.findUserByEmail(auth.getName());

        // Check if the user is an organizer
        if (organizer == null || !"ORGANIZER".equals(organizer.getRole())) {
            return "redirect:/users/login";
        }

        // Fetch events for this organizer
//        List<EventModel> upcomingEvents = eventDao.getUpcomingEventsByOrganizer(organizer.getId());
//        List<EventModel> pastEvents = eventDao.getPastEventsByOrganizer(organizer.getId());

        // Add attributes to the model
        model.addAttribute("organizer", organizer);
//        model.addAttribute("upcomingEvents", upcomingEvents);
//        model.addAttribute("pastEvents", pastEvents);
//        model.addAttribute("totalEvents", upcomingEvents.size() + pastEvents.size());

        // Return the view name
        return "organizer-dashboard";
    }
}