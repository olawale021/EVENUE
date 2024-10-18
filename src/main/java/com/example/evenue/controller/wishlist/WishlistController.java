package com.example.evenue.controller.wishlist;

import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.WishlistService;
import com.example.evenue.service.UserService;
import com.example.evenue.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    // Get all wishlist items for a specific user
    @GetMapping("/user")
    public String getWishlistByUser(Model model) {
        // Retrieve the currently authenticated user from Spring Security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Fetch user by email (assuming you have a method to find by email)
        Optional<UserModel> user = Optional.ofNullable(userService.findUserByEmail(userEmail));

        if (user.isPresent()) {
            List<EventModel> wishlistItems = wishlistService.getWishlistByUser(user.get());
            model.addAttribute("wishlistItems", wishlistItems);
            return "wishlist"; // Return the wishlist view
        } else {
            model.addAttribute("errorMessage", "User not found.");
            return "error"; // Redirect to error page
        }
    }

    // Add an event or product to the wishlist
    @PostMapping("/add")
    public String addToWishlist(@RequestParam Long userId, @RequestParam Long eventId, Model model) {
        Optional<UserModel> user = Optional.ofNullable(userService.findUserById(Math.toIntExact(userId)));
        Optional<EventModel> event = eventService.getEventById(eventId);

        if (user.isPresent() && event.isPresent()) {
            wishlistService.addEventToWishlist(user.get(), event.get());
            return "redirect:/wishlist/user/" + userId; // Redirect to the user's wishlist
        } else {
            model.addAttribute("errorMessage", "User or Event not found.");
            return "error"; // Redirect to error page
        }
    }

    // Remove an event or product from the wishlist
    @DeleteMapping("/remove/{wishlistId}")
    public String removeFromWishlist(@PathVariable Long wishlistId, Model model) {
        boolean removed = wishlistService.removeFromWishlist(wishlistId);
        if (removed) {
            return "redirect:/wishlist/user"; // Redirect back to the wishlist after removing the item
        } else {
            model.addAttribute("errorMessage", "Wishlist item not found.");
            return "error"; // Redirect to error page
        }
    }

    // Check if an event is liked by a specific user
    @GetMapping("/isLikedAndInWishlist")
    public @ResponseBody boolean isLikedAndInWishlist(@RequestParam Integer userId, @RequestParam Long eventId) {
        Optional<UserModel> user = Optional.ofNullable(userService.findUserById(userId));
        Optional<EventModel> event = eventService.getEventById(eventId);

        if (user.isPresent() && event.isPresent()) {
            return wishlistService.isEventLikedByUser(user.get(), event.get());
        } else {
            throw new RuntimeException("User or Event not found."); // Throw exception if user or event not found
        }
    }

    // Toggle the like (add or remove) and redirect
    @PostMapping("/toggleLike")
    public String toggleLike(@RequestParam Long userId, @RequestParam Long eventId, Model model) {
        Optional<UserModel> user = Optional.ofNullable(userService.findUserById(Math.toIntExact(userId)));
        Optional<EventModel> event = eventService.getEventById(eventId);

        if (user.isPresent() && event.isPresent()) {
            wishlistService.toggleLike(user.get(), event.get());
            return "redirect:/events/browse"; // Redirect to the event browsing page
        } else {
            model.addAttribute("errorMessage", "User or Event not found.");
            return "error"; // Redirect to error page
        }
    }
}
