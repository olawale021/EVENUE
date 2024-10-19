package com.example.evenue.controller.friends;

import com.example.evenue.models.events.EventModel;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.EventService;
import com.example.evenue.service.FriendsService;
import com.example.evenue.models.friends.FriendRequestModel;
import com.example.evenue.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/friends")
public class FriendsController {

    @Autowired
    private FriendsService friendsService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    // View the list of friends for the logged-in user
    @GetMapping
    public String viewFriends(Model model, Authentication authentication) {
        // Get the currently logged-in user
        String userEmail = authentication.getName();
        UserModel currentUser = userService.findUserByEmail(userEmail);

        // Fetch the user's friends
        List<UserModel> friends = friendsService.getFriends(currentUser);

        // Fetch pending friend requests
        List<FriendRequestModel> pendingFriendRequests = friendsService.getPendingFriendRequests(currentUser);

        // Add friends and pending friend requests to the model
        model.addAttribute("friends", friends);
        model.addAttribute("pendingFriendRequests", pendingFriendRequests);

        return "friends"; // Returns a Thymeleaf template named "friends.html"
    }

    // Search for users by username
    @GetMapping("/search")
    public String searchUsers(@RequestParam("username") String username, Authentication authentication, Model model) {
        // Get the currently logged-in user
        String currentUserEmail = authentication.getName();
        UserModel currentUser = userService.findUserByEmail(currentUserEmail);

        // Perform the search and filter out already-friends and the current user
        List<UserModel> searchResults = friendsService.searchUsersToAddAsFriends(currentUser, username);

        // Add search results to the model
        model.addAttribute("searchResults", searchResults);
        return "search-friends";  // This will render a view called search-friends.html
    }

    // Send a friend request
    @PostMapping("/add/{friendId}")
    public String addFriend(@PathVariable Long friendId, Authentication authentication) {
        // Get the currently logged-in user
        String currentUserEmail = authentication.getName();
        UserModel currentUser = userService.findUserByEmail(currentUserEmail);

        // Find the user to be added as a friend
        UserModel friendToAdd = userService.findUserById(Math.toIntExact(friendId));

        // Send friend request
        friendsService.sendFriendRequest(currentUser, friendToAdd);

        // Redirect back to the friends list or another page
        return "redirect:/friends";
    }

    // Accept a friend request
    @PostMapping("/accept/{requestId}")
    public String acceptFriendRequest(@PathVariable Long requestId) {
        friendsService.acceptFriendRequest(requestId);
        return "redirect:/friends";
    }

    // Reject a friend request
    @PostMapping("/reject/{requestId}")
    public String rejectFriendRequest(@PathVariable Long requestId) {
        friendsService.rejectFriendRequest(requestId);
        return "redirect:/friends";
    }

    // View friend details
    @GetMapping("/details/{friendId}")
    public String viewFriendDetails(@PathVariable Long friendId, Model model) {
        // Find the friend's details
        UserModel friend = userService.findUserById(Math.toIntExact(friendId));

        // Fetch the events the friend is attending based on tickets
        List<EventModel> attendingEvents = eventService.getEventsByUser(friend);

        // Add data to the model to render in the view
        model.addAttribute("friend", friend);
        model.addAttribute("attendingEvents", attendingEvents);

        return "friend-details";  // Return Thymeleaf template for friend details
    }

}
