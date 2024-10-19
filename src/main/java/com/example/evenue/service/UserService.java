package com.example.evenue.service;

import com.example.evenue.models.users.Role;
import com.example.evenue.models.users.UserModel;
import com.example.evenue.models.users.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    // Save a new user
    public void saveUser(UserModel user) {
        userDao.save(user);
    }

    // Find user by email
    public UserModel findUserByEmail(String email) {
        return userDao.findByEmail(email);
    }

    // Find user by ID
    public UserModel findUserById(Integer id) {
        Optional<UserModel> user = userDao.findById(id);
        return user.orElse(null); // Return user if found, otherwise return null
    }

    // Update user role by ID
    public void updateUserRoleById(Role role, Integer userId) {
        // Fetch the user by ID
        Optional<UserModel> userOpt = userDao.findById(userId);

        if (userOpt.isPresent()) {
            UserModel user = userOpt.get();
            user.setRole(role); // Set the new role
            userDao.save(user); // Save the updated user record
        }
    }

    // Update user role by Email
    public void updateUserRoleByEmail(Role role, String email) {
        UserModel user = findUserByEmail(email);
        if (user != null) {
            user.setRole(role); // Set the new role
            userDao.save(user); // Save the updated user record
        }
    }

    public Optional<UserModel> findUserByUsername(String username) {
        return userDao.findByUserName(username);
    }

    // Optionally, for partial matches
    public List<UserModel> searchUsersByUsername(String username) {
        return userDao.findByUserNameContainingIgnoreCase(username);
    }
    // Method to search for users by a partial username
    public List<UserModel> findByUserNameContaining(String username) {
        return userDao.findByUserNameContainingIgnoreCase(username);
    }
}
