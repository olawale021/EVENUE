package com.example.evenue.service;

import com.example.evenue.models.users.UserModel;
import com.example.evenue.models.users.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDao userDao;

    @Autowired
    public CustomUserDetailsService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("loadUserByUsername called with email: '" + email + "'");
        System.out.println("Email class: " + (email != null ? email.getClass().getName() : "null"));
        System.out.println("Email length: " + (email != null ? email.length() : "N/A"));

        if (email == null || email.trim().isEmpty()) {
            System.out.println("Email is null or empty");
            throw new UsernameNotFoundException("Email cannot be empty");
        }

        UserModel user = userDao.findUserByEmail(email.trim());
        if (user == null) {
            System.out.println("No user found for email: " + email);
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        System.out.println("User found: " + user.getEmail());
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                getAuthorities(user.getRole())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(String role) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}