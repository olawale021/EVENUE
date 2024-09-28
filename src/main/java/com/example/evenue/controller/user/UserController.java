package com.example.evenue.controller.user;

import com.example.evenue.models.users.UserModel;
import com.example.evenue.service.UserService;
import com.example.evenue.models.users.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/users")
@SessionAttributes("userId") // Using Spring's session management
public class UserController {

    private final UserService userService; // Inject UserService
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // Serve the registration page
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        return "register"; // Return register.html
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        return "login"; // Return login.html
    }

    @PostMapping("/logout")
    public String logout(Model model) {
        model.addAttribute("userId", null); // Clear the user ID from the model
        SecurityContextHolder.clearContext(); // Clear the security context
        return "redirect:/users/login"; // Redirect to the login page
    }

    // Register a new user with email and password, and set user ID in session
    @PostMapping("/register")
    public String registerUser(@RequestParam String email, @RequestParam String password, Model model) {
        if (userService.findUserByEmail(email) != null) { // Use userService
            model.addAttribute("error", "Email already in use.");
            return "register"; // Return to the registration form with an error message
        }

        UserModel newUser = new UserModel();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password)); // Encrypt the password
        newUser.setRole(null); // Initially, the role is set to null

        userService.saveUser(newUser); // Save the new user using userService
        model.addAttribute("userId", newUser.getId()); // Store user ID in the model
        return "redirect:/users/set-role"; // Redirect to the role selection page
    }


    // Handle user login
    @PostMapping("/login")
    public String loginUser(@RequestParam String email, @RequestParam String password, Model model) {
        UserModel user = userService.findUserByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "Invalid email or password.");
            return "login"; // Return to the login form with an error message
        }

        // If role is null, redirect to set-role page
        if (user.getRole() == null) {
            model.addAttribute("userId", user.getId());
            return "redirect:/users/set-role";
        }

        model.addAttribute("userId", user.getId());
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        Authentication auth = new UsernamePasswordAuthenticationToken(user.getEmail(), password, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Redirect based on role
        if (user.getRole() == Role.ORGANIZER) {
            return "redirect:/organizer/dashboard";
        } else if (user.getRole() == Role.ATTENDEE) {
            return "redirect:/users/dashboard";
        } else {
            return "redirect:/users/set-role"; // Fallback in case role is not set correctly
        }
    }



    // Serve the dashboard page
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/users/login";
        }

        String email = authentication.getName(); // This will be the email address
        UserModel user = userService.findUserByEmail(email); // Use userService

        if (user == null) {
            // This shouldn't happen if the user is authenticated, but just in case
            return "redirect:/users/login";
        }

        model.addAttribute("user", user);
        return "dashboard";
    }

    // Serve the role selection page
    @GetMapping("/set-role")
    public String showRoleSelectionForm(Model model) {
        Integer userId = (Integer) model.getAttribute("userId");
        if (userId == null) {
            return "redirect:/users/login"; // If there's no user in session, redirect to login
        }

        UserModel user = userService.findUserById(userId); // Fetch user by ID
        if (user == null) {
            return "redirect:/users/register";
        }

        model.addAttribute("email", user.getEmail());
        return "set-role";
    }


    // Handle role selection
    @PostMapping("/set-role")
    public String setUserRole(@RequestParam String role, Model model, Authentication authentication) {
        Integer userId = (Integer) model.getAttribute("userId");
        if (userId == null) {
            return "redirect:/users/login"; // If there's no user in session, redirect to login
        }

        UserModel user = userService.findUserById(userId); // Fetch user by ID
        if (user == null) {
            return "redirect:/users/register";
        }

        // Convert the string role to the Role enum
        try {
            Role userRole = Role.valueOf(role.toUpperCase());

            // Update the role in the database
            userService.updateUserRoleById(userRole, userId);

            // Update the security context with the new role
            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole.name()));
            Authentication newAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            // Determine redirect based on new role
            if (userRole == Role.ORGANIZER) {
                return "redirect:/organizer/dashboard";
            } else {
                return "redirect:/users/dashboard";
            }
        } catch (IllegalArgumentException e) {
            // Invalid role provided
            return "redirect:/users/set-role?error=invalid_role";
        }
    }
}
