package com.example.evenue.controller.chatbot;

import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.tickets.TicketTypeModel;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.EventService;
import com.example.evenue.service.TicketTypeService;
import com.example.evenue.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class DialogflowWebhookController {

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleDialogflowWebhook(@RequestBody Map<String, Object> request) {
        // Extract the intent name
        Map<String, Object> queryResult = (Map<String, Object>) request.get("queryResult");
        Map<String, Object> intent = (Map<String, Object>) queryResult.get("intent");
        String intentName = (String) intent.get("displayName");

        // Handle the intent based on its name
        switch (intentName) {
            case "CollectEmailIntent":
                return handleCollectEmailIntent(queryResult, request);
            case "CollectEventIntent":
                return handleCollectEventIntent(queryResult, request);
            case "CollectTicketTypeIntent":
                return handleCollectTicketTypeIntent(queryResult, request);
            default:
                return handleFallbackIntent();
        }
    }

    private ResponseEntity<Map<String, Object>> handleCollectEmailIntent(Map<String, Object> queryResult, Map<String, Object> request) {
        // Extract email and get user_id from the database
        Map<String, Object> parameters = (Map<String, Object>) queryResult.get("parameters");
        String email = (String) parameters.get("email");

        UserModel user = userService.findUserByEmail(email);

        // Save user_id and email in the session
        Map<String, Object> sessionParameters = new HashMap<>();
        sessionParameters.put("user_id", user.getId());
        sessionParameters.put("email", email);

        // Respond with event prompt and session context
        Map<String, Object> fulfillmentResponse = new HashMap<>();
        fulfillmentResponse.put("fulfillmentText", "Which event would you like to book tickets for?");
        fulfillmentResponse.put("outputContexts", List.of(
                Map.of(
                        "name", request.get("session") + "/contexts/awaiting_event_name",
                        "lifespanCount", 5,
                        "parameters", sessionParameters
                )
        ));

        return ResponseEntity.ok(fulfillmentResponse);
    }

    @Autowired
    private TicketTypeService ticketTypeService;

    private ResponseEntity<Map<String, Object>> handleCollectEventIntent(Map<String, Object> queryResult, Map<String, Object> request) {
        // Extract event name and get event_id from the database
        Map<String, Object> parameters = (Map<String, Object>) queryResult.get("parameters");
        String eventName = ((String) parameters.get("event")).trim();

        // Replace non-breaking spaces with regular spaces
        eventName = eventName.replace("\u00A0", " ");

        // Log the event name to debug
        System.out.println("Event name received (normalized): " + eventName);

        // Find event by name (case-insensitive search)
        EventModel event = eventService.findByEventName(eventName);

        if (event == null) {
            // Event not found, prompt the user to provide a valid event name
            Map<String, Object> fulfillmentResponse = new HashMap<>();
            fulfillmentResponse.put("fulfillmentText", "I'm sorry, I couldn't find the event: " + eventName + ". Please provide a valid event name.");
            return ResponseEntity.ok(fulfillmentResponse);
        }

        // Retrieve available ticket types for the event
        List<TicketTypeModel> ticketTypes = ticketTypeService.getTicketTypesByEventId(event.getId());

        if (ticketTypes.isEmpty()) {
            // No ticket types available for this event
            Map<String, Object> fulfillmentResponse = new HashMap<>();
            fulfillmentResponse.put("fulfillmentText", "I'm sorry, there are no tickets available for this event.");
            return ResponseEntity.ok(fulfillmentResponse);
        }

        // Build a response showing ticket type options
        StringBuilder ticketOptions = new StringBuilder("Here are the available ticket types for " + eventName + ":\n");
        for (TicketTypeModel ticketType : ticketTypes) {
            ticketOptions.append(ticketType.getTypeName()).append(" - $").append(ticketType.getPrice()).append("\n");
        }
        ticketOptions.append("Please choose a ticket type.");

        // Add event_id and event_name to session parameters
        List<Map<String, Object>> outputContexts = (List<Map<String, Object>>) queryResult.get("outputContexts");
        Map<String, Object> sessionParameters = outputContexts.stream()
                .filter(context -> ((String) context.get("name")).endsWith("/contexts/awaiting_event_name"))
                .findFirst()
                .map(context -> (Map<String, Object>) context.get("parameters"))
                .orElse(new HashMap<>());

        sessionParameters.put("event_id", event.getId());
        sessionParameters.put("event_name", eventName);

        // Respond with ticket type options and update session context
        Map<String, Object> fulfillmentResponse = new HashMap<>();
        fulfillmentResponse.put("fulfillmentText", ticketOptions.toString());
        fulfillmentResponse.put("outputContexts", List.of(
                Map.of(
                        "name", request.get("session") + "/contexts/awaiting_ticket_type",
                        "lifespanCount", 5,
                        "parameters", sessionParameters
                )
        ));

        return ResponseEntity.ok(fulfillmentResponse);
    }

    private ResponseEntity<Map<String, Object>> handleCollectTicketTypeIntent(Map<String, Object> queryResult, Map<String, Object> request) {
        // Extract ticket type and quantity from user input
        Map<String, Object> parameters = (Map<String, Object>) queryResult.get("parameters");

        // Safely extract ticket type and handle null case
        String ticketTypeName = (String) parameters.get("ticketType");
        if (ticketTypeName == null || ticketTypeName.isEmpty()) {
            Map<String, Object> fulfillmentResponse = new HashMap<>();
            fulfillmentResponse.put("fulfillmentText", "Please specify a ticket type.");
            return ResponseEntity.ok(fulfillmentResponse);
        }
        ticketTypeName = ticketTypeName.trim(); // Safe to trim after null check

        // Safely extract quantity and handle null case
        Object quantityObj = parameters.get("quantity");
        Integer quantity = null;
        if (quantityObj instanceof Double) {
            quantity = ((Double) quantityObj).intValue();  // Convert Double to Integer
        } else if (quantityObj instanceof Integer) {
            quantity = (Integer) quantityObj;
        }

        if (quantity == null || quantity <= 0) {
            Map<String, Object> fulfillmentResponse = new HashMap<>();
            fulfillmentResponse.put("fulfillmentText", "Please specify a valid number of tickets.");
            return ResponseEntity.ok(fulfillmentResponse);
        }

        // Retrieve previous session parameters (event_id)
        List<Map<String, Object>> outputContexts = (List<Map<String, Object>>) queryResult.get("outputContexts");
        Map<String, Object> sessionParameters = outputContexts.stream()
                .filter(context -> ((String) context.get("name")).endsWith("/contexts/awaiting_ticket_type"))
                .findFirst()
                .map(context -> new HashMap<>((Map<String, Object>) context.get("parameters"))) // Copy to a new map
                .orElse(new HashMap<>());

        // Retrieve event_id safely (check if it's a Double and convert to Long)
        Object eventIdObj = sessionParameters.get("event_id");
        Long eventId = null;
        if (eventIdObj instanceof Double) {
            eventId = ((Double) eventIdObj).longValue();  // Convert Double to Long
        } else if (eventIdObj instanceof Long) {
            eventId = (Long) eventIdObj;
        }

        if (eventId == null) {
            Map<String, Object> fulfillmentResponse = new HashMap<>();
            fulfillmentResponse.put("fulfillmentText", "Event ID is missing. Please try again.");
            return ResponseEntity.ok(fulfillmentResponse);
        }

        // Retrieve the ticket types again for verification
        List<TicketTypeModel> ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);

        // Verify the selected ticket type is available for the event
        String finalTicketTypeName = ticketTypeName;
        TicketTypeModel selectedTicketType = ticketTypes.stream()
                .filter(ticketType -> ticketType.getTypeName().name().equalsIgnoreCase(finalTicketTypeName))  // Use enum's name() method
                .findFirst()
                .orElse(null);

        if (selectedTicketType == null) {
            // Ticket type not found, prompt the user again
            Map<String, Object> fulfillmentResponse = new HashMap<>();
            fulfillmentResponse.put("fulfillmentText", "I'm sorry, I couldn't find the ticket type: " + ticketTypeName + ". Please choose a valid ticket type.");
            return ResponseEntity.ok(fulfillmentResponse);
        }

        // Save the selected ticket type and quantity in the session
        sessionParameters.put("ticket_type_id", selectedTicketType.getTicketTypeId());
        sessionParameters.put("ticket_type_name", selectedTicketType.getTypeName().name());  // Store the enum's name
        sessionParameters.put("ticket_price", selectedTicketType.getPrice());
        sessionParameters.put("quantity", quantity);

        // Respond with a confirmation message
        String confirmationMessage = "You selected " + quantity + " " + selectedTicketType.getTypeName().name() + " tickets priced at $" + selectedTicketType.getPrice() + " each.";
        Map<String, Object> fulfillmentResponse = new HashMap<>();
        fulfillmentResponse.put("fulfillmentText", confirmationMessage + " Would you like to confirm the booking?");
        fulfillmentResponse.put("outputContexts", List.of(
                Map.of(
                        "name", request.get("session") + "/contexts/awaiting_confirmation",
                        "lifespanCount", 5,
                        "parameters", sessionParameters // Now using the copy of sessionParameters
                )
        ));

        return ResponseEntity.ok(fulfillmentResponse);
    }




    private ResponseEntity<Map<String, Object>> handleFallbackIntent() {
        // Handle fallback case where the intent is unrecognized
        Map<String, Object> fulfillmentResponse = new HashMap<>();
        fulfillmentResponse.put("fulfillmentText", "I'm sorry, I didn't understand that. Could you please rephrase?");
        return ResponseEntity.ok(fulfillmentResponse);
    }
}


