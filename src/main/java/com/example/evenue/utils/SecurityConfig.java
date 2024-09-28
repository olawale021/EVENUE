package com.example.evenue.utils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/","/users/register", "/users/login", "/users/set-role", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers("/users/dashboard").hasRole("ATTENDEE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/users/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                                Authentication authentication) throws IOException, ServletException {
                                String role = authentication.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .findFirst()
                                        .orElse("");
                                if (role.equals("ROLE_ORGANIZER")) {
                                    response.sendRedirect("/organizer/dashboard");
                                } else if (role.equals("ROLE_ATTENDEE")) {
                                    response.sendRedirect("/users/dashboard");
                                } else {
                                    // If role is null or empty, redirect to set-role page
                                    response.sendRedirect("/users/set-role");
                                }
                            }
                        })
                        .failureHandler(new AuthenticationFailureHandler() {
                            @Override
                            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                                AuthenticationException exception) throws IOException {
                                System.out.println("Authentication failed: " + exception.getMessage());
                                System.out.println("Email parameter: " + request.getParameter("email"));
                                exception.printStackTrace();
                                response.sendRedirect("/users/login?error=true");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/users/logout")
                        .logoutSuccessUrl("/users/login")
                        .permitAll()
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}