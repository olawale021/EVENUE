package com.example.evenue.controller.event;

import com.example.evenue.models.events.EventCategory;
import com.example.evenue.models.events.EventCategoryDao;
import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.tickets.TicketTypeModel;
import com.example.evenue.models.users.UserDao;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.EventService;
import com.example.evenue.service.TicketTypeService;
import com.example.evenue.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventCategoryDao eventCategoryDao;

    @Autowired
    private UserService userService;

    @Autowired
    private TicketTypeService ticketTypeService;

    // Endpoint to display the create event form
    @GetMapping("/create")
    public String showCreateEventForm(Model model) {
        // Adds a new EventModel object to the model, which will be used in the form
        model.addAttribute("event", new EventModel());

        // Fetch all categories from the database
        List<EventCategory> categories = eventCategoryDao.findAll();

        // Add the categories to the model to be displayed in the dropdown
        model.addAttribute("categories", categories);
        return "create-event"; // Returns the name of the Thymeleaf template for the form
    }

    // Endpoint to handle form submission and add a new event with file upload handling
    @PostMapping("/create")
    public String createEvent(
            @ModelAttribute("event") EventModel event,
            @RequestParam(value = "eventImageFile", required = false) MultipartFile file,
            Model model) {

        // Get the currently logged-in user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInUserEmail = authentication.getName(); //  the username is the email
        UserModel loggedInUser = userService.findUserByEmail(loggedInUserEmail); // Using UserDao to get user details

        // Set the organizer ID from the logged-in user
        if (loggedInUser != null) {
            event.setOrganizerId(loggedInUser.getId());
        } else {
            model.addAttribute("errorMessage", "Could not find logged-in user details.");
            return "create-event";
        }

        if (file != null && !file.isEmpty()) {
            try {
                if (file.getSize() > 5 * 1024 * 1024) {
                    model.addAttribute("errorMessage", "File size must be less than 5MB.");
                    return "create-event";
                }

                byte[] photoBytes = file.getBytes();
                String photoBase64 = Base64.getEncoder().encodeToString(photoBytes);
                String photoBase64WithPrefix = "data:" + file.getContentType() + ";base64," + photoBase64;

                event.setEventImage(photoBase64WithPrefix);

            } catch (IOException e) {
                model.addAttribute("errorMessage", "Failed to upload image. Please try again.");
                return "create-event";
            }
        }

        // Set any other necessary fields that aren't coming from the form
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        // Save the event to the database
        eventService.addEvent(event);
        model.addAttribute("message", "Event created successfully!");
        return "redirect:/events/create";
    }

    // Browse events - GET method to display events
    @GetMapping("/browse")
    public String browseEvents(Model model) {
        // Fetch all events
        List<EventModel> events = eventService.getAllEvents();
        model.addAttribute("events", events);

        // Fetch all categories for filtering purposes
        List<EventCategory> categories = eventCategoryDao.findAll();
        model.addAttribute("categories", categories);

        return "browse-events";
    }

    // Browse events - POST method to handle filtering of events
    @PostMapping("/browse")
    public String filterEvents(
            @RequestParam("category") Long categoryId,
            @RequestParam("search") String search,
            @RequestParam("sortBy") String sortBy,
            Model model) {

        // Filter events based on category, search text, and sort order
        List<EventModel> filteredEvents = eventService.filterEvents(categoryId, search, sortBy);
        model.addAttribute("events", filteredEvents);

        // Also add the categories again for the filter dropdown
        List<EventCategory> categories = eventCategoryDao.findAll();
        model.addAttribute("categories", categories);

        return "browse-events"; // Return to the browse events page with filtered results
    }

    // Endpoint to display event details
    @GetMapping("/details/{eventId}")
    public String eventDetails(@PathVariable("eventId") Long eventId, Model model) {
        // Fetch the event details using the event ID
        EventModel event = eventService.getEventById(eventId);

        // Fetch all events from the database
        List<EventModel> events = eventService.getAllEvents();

        // Check if the event exists
        if (event == null) {
            model.addAttribute("errorMessage", "Event not found.");
            return "error"; // Render an error page or message if event not found
        }

        // Fetch ticket types for the event
        List<TicketTypeModel> ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);

        // Calculate price range
        double minPrice = ticketTypes.stream()
                .mapToDouble(TicketTypeModel::getPrice)
                .min()
                .orElse(0);
        double maxPrice = ticketTypes.stream()
                .mapToDouble(TicketTypeModel::getPrice)
                .max()
                .orElse(0);

        // Add attributes to the model
        model.addAttribute("events", events);
        model.addAttribute("event", event);
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        return "event-details"; // Returns the name of the Thymeleaf template for event details
    }
}
