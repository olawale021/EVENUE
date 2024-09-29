package com.example.evenue.controller.tickets;


import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.tickets.TicketModel;
import com.example.evenue.models.tickets.TicketTypeModel;
import com.example.evenue.service.EventService;
import com.example.evenue.service.TicketService;
import com.example.evenue.service.TicketTypeService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @GetMapping("/create/{ticketTypeId}")
    public String showCreateTicketForm(@PathVariable Long ticketTypeId, Model model) {
        TicketTypeModel ticketType = ticketTypeService.getTicketTypeById(ticketTypeId);
        if (ticketType == null || ticketType.getRemainingQuantity() <= 0) {
            model.addAttribute("error", "Invalid or sold out ticket type");
            return "error";
        }

        model.addAttribute("ticketType", ticketType);
        model.addAttribute("ticket", new TicketModel());
        return "create-ticket";
    }

    // Endpoint to handle the creation of a ticket
    @PostMapping("/create")
    public String createTicket(@ModelAttribute("ticket") TicketModel ticket, Model model) {
        // Validate and save the ticket
        if (ticket.getUser() == null || ticket.getEvent() == null || ticket.getTicketType() == null) {
            model.addAttribute("error", "Invalid input");
            return "create-ticket";
        }

        TicketTypeModel ticketType = ticketTypeService.getTicketTypeById(ticket.getTicketType().getTicketTypeId());
        if (ticketType.getRemainingQuantity() <= 0) {
            model.addAttribute("error", "No tickets available for this type");
            return "create-ticket";
        }

        ticket.setTicketCode(generateTicketCode());
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketService.saveTicket(ticket);

        // Update remaining quantity in ticket type
        ticketType.setRemainingQuantity(ticketType.getRemainingQuantity() - 1);
        ticketTypeService.saveTicketType(ticketType);

        model.addAttribute("message", "Ticket created successfully!");
        return "redirect:/tickets/types/" + ticket.getEvent().getId();
    }

    // Helper method to generate a unique ticket code
    private String generateTicketCode() {
        return UUID.randomUUID().toString();
    }

    // Add other ticket and ticket type management endpoints as needed
}
