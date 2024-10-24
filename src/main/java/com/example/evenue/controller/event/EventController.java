package com.example.evenue.controller.event;

import com.example.evenue.models.events.EventCategory;
import com.example.evenue.models.events.EventCategoryDao;
import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.tickets.TicketTypeModel;
import com.example.evenue.models.users.UserDao;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserBehaviourService userBehaviourService;

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

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

    // Browse events - GET method with pagination support
    @GetMapping("/browse")
    public String browseEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String price,
            Model model, Authentication authentication) {

        // Fetch the logged-in user
        String userEmail = authentication.getName();
        UserModel loggedInUser = userService.findUserByEmail(userEmail);
        model.addAttribute("loggedInUser", loggedInUser);

        // Initialize categories to an empty list if null
        if (categories == null) {
            categories = new ArrayList<>();
        }

        // Log the filter values
        logger.info("Categories: {}", categories);
        logger.info("Date filter: {}", date);
        logger.info("Price filter: {}", price);

        // Create pageable object
        Pageable pageable = PageRequest.of(page, size);

        // Fetch filtered events
        Page<EventModel> eventPage;
        if (categories.isEmpty() && (date == null || date.isEmpty()) && (price == null || price.isEmpty())) {
            eventPage = eventService.getAllEvents(pageable);
        } else {
            eventPage = eventService.getFilteredEvents(categories, date, price, pageable);
        }

        // Log the number of events fetched
        logger.info("Number of events fetched: {}", eventPage.getTotalElements());

        // Set 'isLiked' flag for each event
        for (EventModel event : eventPage.getContent()) {
            boolean isLiked = wishlistService.isEventLikedByUser(loggedInUser, event);
            event.setLiked(isLiked);
        }

        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);

        // Fetch categories for filtering
        List<EventCategory> eventCategories = eventCategoryDao.findAll();
        model.addAttribute("categories", eventCategories);

        // Ensure date and price filters are passed back
        model.addAttribute("selectedDate", date != null ? date : "");
        model.addAttribute("selectedPrice", price != null ? price : "");
        // Add selectedCategories to the model
        model.addAttribute("selectedCategories", categories);

        return "browse-events";
    }

    // Filter events with pagination
    @PostMapping("/browse")
    public String filterEvents(
            @RequestParam("category") Long categoryId,
            @RequestParam("search") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<EventModel> filteredEvents = eventService.filterEvents(categoryId, search, pageable);

        model.addAttribute("events", filteredEvents.getContent());
        model.addAttribute("totalPages", filteredEvents.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);

        // Add categories again for the filter dropdown
        List<EventCategory> categories = eventCategoryDao.findAll();
        model.addAttribute("categories", categories);

        return "browse-events"; // Return to the browse events page with filtered results
    }

    @GetMapping("/details/{eventId}")
    public String eventDetails(@PathVariable("eventId") Long eventId, Model model) {
        // Fetch the event details using the event ID
        Optional<EventModel> eventOptional = eventService.getEventById(eventId);

        // Check if the event exists
        if (eventOptional.isEmpty()) {
            model.addAttribute("errorMessage", "Event not found.");
            return "error"; // Render an error page or message if event not found
        }

        EventModel event = eventOptional.get(); // Unwrapping Optional to get EventModel

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
        model.addAttribute("event", event); // Pass the unwrapped EventModel to the view
        model.addAttribute("ticketTypes", ticketTypes);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        return "event-details"; // Returns the name of the Thymeleaf template for event details
    }

    @PostMapping(value = "/log-behaviour", consumes = "text/plain")
    @ResponseBody
    public ResponseEntity<String> logUserBehaviour(@RequestBody String data, Authentication authentication) {
        // Log to see if the method is triggered
        System.out.println("Log behaviour request received with text/plain content type");

        // Parse the plain text data to extract JSON
        Map<String, Object> jsonData = null;
        try {
            jsonData = new ObjectMapper().readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Extract timeSpent and filterType from the JSON data
        Double timeSpent = ((Number) jsonData.get("timeSpent")).doubleValue();
        Map<String, Object> filterTypeMap = (Map<String, Object>) jsonData.get("filterType");

        // Extract eventId if it's present
        Object eventIdObj = jsonData.get("eventId");
        Long eventId = null;
        if (eventIdObj != null) {
            eventId = eventIdObj instanceof Integer ? Long.valueOf((Integer) eventIdObj) : Long.valueOf((String) eventIdObj);
        }

        // Log received data for verification
        System.out.println("Time Spent: " + timeSpent + " seconds");
        System.out.println("Filter Type: " + filterTypeMap);

        // Fetch the logged-in user
        String userEmail = authentication.getName();
        UserModel loggedInUser = userService.findUserByEmail(userEmail);

        // Extract user's preferred categories
        Set<EventCategory> preferredCategories = loggedInUser.getPreferredCategories();
        Long preferredCategory1 = null;
        Long preferredCategory2 = null;
        Long preferredCategory3 = null;

        Iterator<EventCategory> iterator = preferredCategories.iterator();
        if (iterator.hasNext()) {
            preferredCategory1 = iterator.next().getId();
        }
        if (iterator.hasNext()) {
            preferredCategory2 = iterator.next().getId();
        }
        if (iterator.hasNext()) {
            preferredCategory3 = iterator.next().getId();
        }

        // Handle the case where we're just logging the session length and filters on the browse page
        if (filterTypeMap != null && !filterTypeMap.isEmpty()) {
            String dateFilter = (String) filterTypeMap.get("date");
            String priceFilter = (String) filterTypeMap.get("price");
            String locationFilter = (String) filterTypeMap.get("location");

            userBehaviourService.logUserBehaviour(
                    loggedInUser.getId(),
                    null,  // No eventId for browsing
                    "browse",  // Set interaction type as browse
                    null,  // No event location
                    dateFilter,  // Date filter
                    priceFilter,  // Price filter
                    locationFilter,  // Location filter
                    timeSpent,  // Set session length (time spent browsing)
                    null,  // No friend ID
                    null,  // No wishlist action
                    null // No event category ID

            );
            System.out.println("Browse interaction logged for user: " + loggedInUser.getId());
        }

        // Handle the case where an eventId is present (e.g., for event details)
        if (eventId != null) {
            Optional<EventModel> eventOptional = eventService.getEventById(eventId);
            if (eventOptional.isPresent()) {
                EventModel event = eventOptional.get();
                Long eventCategoryId = event.getEventCategory() != null ? event.getEventCategory().getId() : null;

                userBehaviourService.logUserBehaviour(
                        loggedInUser.getId(),
                        eventId,
                        "view",  // Set interaction type as view
                        event.getLocation(),
                        null,  // No filter type for event view
                        null,  // No date filter for event view
                        null,  // No price filter for event view
                        timeSpent,  // Set session length
                        null,  // No friend ID
                        null,  // No wishlist action
                        eventCategoryId // Pass the event category ID directly

                );
                System.out.println("Interaction logged for user: " + loggedInUser.getId());
            }
        }

        return ResponseEntity.ok("Behaviour logged successfully");
    }


}
