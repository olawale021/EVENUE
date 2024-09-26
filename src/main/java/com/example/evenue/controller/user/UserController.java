package com.example.evenue.controller.user;

import com.example.evenue.models.users.User;
import com.example.evenue.models.users.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.sql.SQLException;

@Controller
@RequestMapping("/users")
@SessionAttributes("userId") // Using Spring's session management
public class UserController {

    private final UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserDao userDao, BCryptPasswordEncoder passwordEncoder) {
        this.userDao = userDao;
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
        return "redirect:/users/login"; // Redirect to the login page
    }


    // Register a new user with email and password, and set user ID in session
    @PostMapping("/register")
    public String registerUser(@RequestParam String email, @RequestParam String password, Model model) {
        if (userDao.findUserByEmail(email) != null) {
            model.addAttribute("error", "Email already in use.");
            return "register"; // Return to the registration form with an error message
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password)); // Encrypt the password

        try {
            userDao.insertUser(newUser); // This will set the ID in the newUser object
            model.addAttribute("userId", newUser.getId()); // Store user ID in the model
            return "redirect:/users/set-role"; // Redirect to the role selection page
        } catch (SQLException e) {
            model.addAttribute("error", "Error registering user: " + e.getMessage());
            return "register"; // Return to the registration form with an error message
        }
    }

    // Handle user login
    @PostMapping("/login")
    public String loginUser(@RequestParam String email, @RequestParam String password, Model model) {
        User user = userDao.findUserByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "Invalid email or password.");
            return "login"; // Return to the login form with an error message
        }

        // Successful login, store user ID in the session
        model.addAttribute("userId", user.getId());
        return "redirect:/users/dashboard"; // Redirect to the dashboard
    }

    // Serve the dashboard page
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        Integer userId = (Integer) model.getAttribute("userId");
        if (userId == null) {
            return "redirect:/users/login"; // Redirect to login if user is not logged in
        }

        // Add any necessary data to the model for the dashboard
        model.addAttribute("user", userDao.findUserById(userId)); // Optional: add user info
        return "dashboard"; // Return dashboard.html
    }

    // Serve the role selection page
    @GetMapping("/set-role")
    public String showRoleSelectionForm(Model model) {
        Integer userId = (Integer) model.getAttribute("userId");
        if (userId == null) {
            model.addAttribute("error", "User ID not found in session. Please register or log in.");
            return "redirect:/users/register"; // Redirect to registration if user ID is not in model
        }
        model.addAttribute("userId", userId); // Pass the user ID to the form
        return "set-role"; // Return set-role.html
    }

    // Handle role selection
    @PostMapping("/set-role")
    public String setUserRole(@RequestParam String role, Model model) {
        Integer userId = (Integer) model.getAttribute("userId");
        if (userId == null) {
            model.addAttribute("error", "User ID not found in session. Please register or log in.");
            return "redirect:/users/register"; // Redirect to registration if user ID is not in model
        }

        User user = userDao.findUserById(userId);
        if (user == null) {
            model.addAttribute("error", "User not found.");
            return "set-role"; // Return set-role.html with error message
        }

        // Validate role
        if (!role.equals("ORGANIZER") && !role.equals("ATTENDEE")) {
            model.addAttribute("error", "Invalid role selected.");
            return "set-role"; // Return set-role.html with error message
        }

        // Set the user's role and update in the database
        user.setRole(role);
        try {
            userDao.updateUserRole(user); // Update user role in database
            model.addAttribute("success", "Role updated successfully.");
            return "redirect:/users/login"; // Redirect to the login page or another page as needed
        } catch (SQLException e) {
            model.addAttribute("error", "Error updating role: " + e.getMessage());
            return "set-role"; // Return set-role.html with error message
        }
    }
}
