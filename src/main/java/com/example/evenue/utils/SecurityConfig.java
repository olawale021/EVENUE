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
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

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
<<<<<<< HEAD
                        .requestMatchers("/","/users/register", "/users/login", "/users/set-role", "/css/**", "/js/**", "/images/**").permitAll()
=======
                        .requestMatchers("/", "/users/register", "/users/login", "/users/set-role", "/css/**", "/js/**", "/images/**").permitAll()
>>>>>>> cb50d73 (added browse-events fragment in index.html)
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

                                switch (role) {
                                    case "ROLE_ORGANIZER":
                                        response.sendRedirect("/organizer/dashboard");
                                        break;
                                    case "ROLE_ATTENDEE":
                                        response.sendRedirect("/");
                                        break;
                                    case "ROLE_GUEST":
                                    default:
                                        response.sendRedirect("/users/set-role");
                                        break;
                                }
                            }
                        })
                        .failureHandler(new AuthenticationFailureHandler() {
                            @Override
                            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                                AuthenticationException exception) throws IOException {
                                System.out.println("Authentication failed: " + exception.getMessage());
                                response.sendRedirect("/users/login?error=true");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/users/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.sendRedirect("/users/login"))
                        .invalidateHttpSession(true)  // Invalidate the session
                        .deleteCookies("JSESSIONID")  // Delete the session cookie
                        .permitAll()
                )
                .userDetailsService(userDetailsService);

        // Enable CSRF protection for all routes by default.
        // Uncomment the following line if you want to disable CSRF protection (not recommended for production).
        // http.csrf().disable();

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
