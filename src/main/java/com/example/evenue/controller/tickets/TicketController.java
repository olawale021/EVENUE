package com.example.evenue.controller.tickets;


import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.tickets.TicketModel;
import com.example.evenue.models.tickets.TicketTypeModel;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.EventService;
import com.example.evenue.service.TicketService;
import com.example.evenue.service.TicketTypeService;
import com.example.evenue.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketTypeService ticketTypeService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    // Endpoint to show the form for adding a new ticket type to an event
    @GetMapping("/types/add/{eventId}")
    public String showAddTicketTypeForm(@PathVariable Long eventId, Model model) {
        EventModel event = eventService.getEventById(eventId);
        if (event == null) {
            model.addAttribute("error", "Event not found");
            return "error";
        }

        TicketTypeModel ticketType = new TicketTypeModel();
        ticketType.setEvent(event); // Set the event object to the ticket type

        model.addAttribute("event", event);
        model.addAttribute("ticketType", new TicketTypeModel());
        return "add-ticket-type"; // The form template to add ticket type
    }

    // Endpoint to handle the creation of a new ticket type
    @PostMapping("/types/add")
    public String addTicketType(
            @ModelAttribute("ticketType") TicketTypeModel ticketType,
            @RequestParam("eventId") Long eventId,
            Model model) {

        EventModel event = eventService.getEventById(eventId);
        if (event == null) {
            model.addAttribute("error", "Event not found");
            return "error";
        }

        ticketType.setEvent(event);

        if (ticketType.getPrice() == null || ticketType.getQuantity() == null || ticketType.getTypeName() == null) {
            model.addAttribute("error", "All fields are required.");
            model.addAttribute("event", event);
            return "add-ticket-type";
        }

        ticketType.setRemainingQuantity(ticketType.getQuantity());

        try {
            ticketTypeService.saveTicketType(ticketType);
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while saving the ticket type. Please try again.");
            model.addAttribute("event", event);
            return "add-ticket-type";
        }

        return "redirect:/tickets/types/" + eventId;
    }



    // Endpoint to view all ticket types for an event
    @GetMapping("/types/{eventId}")
    public String viewTicketTypes(@PathVariable Long eventId, Model model) {
        EventModel event = eventService.getEventById(eventId);
        if (event == null) {
            model.addAttribute("error", "Event not found");
            return "error";
        }

        List<TicketTypeModel> ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);
        model.addAttribute("event", event);
        model.addAttribute("ticketTypes", ticketTypes);
        return "view-ticket-types";
    }

    // Endpoint to show form for creating a ticket for a specific ticket type
    @GetMapping("/buy/{ticketTypeId}")
    public String showCreateTicketForm(@PathVariable Long ticketTypeId,
                                       @RequestParam("quantity") Integer quantity,
                                       Model model) {
        TicketTypeModel ticketType = ticketTypeService.getTicketTypeById(ticketTypeId);
        if (ticketType == null) {
            model.addAttribute("error", "Invalid ticket type.");
            return "error";
        }

        if (ticketType.getRemainingQuantity() <= 0) {
            model.addAttribute("error", "This ticket type is sold out.");
            return "error";
        }

        if (quantity <= 0 || quantity > ticketType.getRemainingQuantity()) {
            model.addAttribute("error", "Please select a valid quantity.");
            return "redirect:/events/details/" + ticketType.getEvent().getId();
        }

        model.addAttribute("ticketType", ticketType);
        model.addAttribute("quantity", quantity);
        return "confirm-ticket-purchase";
    }


    // Endpoint to handle the creation of a ticket
    @PostMapping("/buy")
    public String createTicket(
            @RequestParam("ticketTypeId") Long ticketTypeId,
            @RequestParam("eventId") Long eventId,
            @RequestParam("quantity") Integer quantity,
            Model model) {

        // Retrieve the currently authenticated user from Spring Security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        UserModel user = userService.findUserByEmail(userEmail);

        if (user == null) {
            model.addAttribute("error", "User not authenticated.");
            return "confirm-ticket-purchase";
        }

        // Retrieve event and ticket type
        EventModel event = eventService.getEventById(eventId);
        TicketTypeModel ticketType = ticketTypeService.getTicketTypeById(ticketTypeId);

        if (event == null || ticketType == null) {
            model.addAttribute("error", "Invalid event or ticket type.");
            return "confirm-ticket-purchase";
        }

        if (ticketType.getRemainingQuantity() < quantity) {
            model.addAttribute("error", "Not enough tickets available for this type.");
            return "confirm-ticket-purchase";
        }

        // Create and populate the ticket
        TicketModel ticket = new TicketModel();
        ticket.setUser(user);
        ticket.setEvent(event);
        ticket.setTicketType(ticketType);
        ticket.setQuantity(quantity);
        ticket.setPrice(ticketType.getPrice() * quantity);
        ticket.setTicketCode(generateTicketCode());
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        // Save the ticket
        ticketService.saveTicket(ticket);

        // Update remaining quantity in ticket type
        ticketType.setRemainingQuantity(ticketType.getRemainingQuantity() - quantity);
        ticketTypeService.saveTicketType(ticketType);

        // Redirect to success page and pass the ticket code
        return "redirect:/tickets/success?ticketCode=" + ticket.getTicketCode();
    }


    @GetMapping("/success")
    public String showPurchaseSuccess(@RequestParam("ticketCode") String ticketCode, Model model) {
        model.addAttribute("ticketCode", ticketCode);
        return "purchase-success";
    }



    // Helper method to generate a unique ticket code
    private String generateTicketCode() {
        return UUID.randomUUID().toString();
    }

    // Add other ticket and ticket type management endpoints as needed
}
