package com.example.evenue.controller.user;

import com.example.evenue.models.users.UserModel;
import com.example.evenue.models.users.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

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

        UserModel newUser = new UserModel();
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
        UserModel user = userDao.findUserByEmail(email);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/users/login";
        }

        String email = authentication.getName(); // This will be the email address
        UserModel user = userDao.findUserByEmail(email);

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/users/login";
        }

        String email = auth.getName();
        UserModel user = userDao.findUserByEmail(email);

        if (user == null) {
            return "redirect:/users/register";
        }

        model.addAttribute("email", email);
        return "set-role";
    }

    // Handle role selection
    @PostMapping("/set-role")
    public String setUserRole(@RequestParam String role, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/users/login";
        }

        String email = authentication.getName();
        UserModel user = userDao.findUserByEmail(email);

        if (user == null) {
            return "redirect:/users/register";
        }

        // Validate role
        if (!role.equals("ORGANIZER") && !role.equals("ATTENDEE")) {
            return "redirect:/users/set-role?error=invalid_role";
        }

        // Set the user's role and update in the database
        user.setRole(role);
        try {
            userDao.updateUserRole(user);

            // Update the security context with the new role
            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
            Authentication newAuth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            // Determine redirect based on new role
            if (role.equals("ORGANIZER")) {
                return "redirect:/organizer/dashboard";
            } else {
                return "redirect:/users/dashboard";
            }
        } catch (SQLException e) {
            return "redirect:/users/set-role?error=database_error";
        }
    }
}
