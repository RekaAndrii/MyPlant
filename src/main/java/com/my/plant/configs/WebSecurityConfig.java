package com.my.plant.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import com.my.plant.service.impl.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/js/**", "/style/**",
                                "/login", "/register", "/quick/**", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        InMemoryUserDetailsManager inMemory = new InMemoryUserDetailsManager(
                User.withUsername("andrii").password("{noop}").roles("USER").build(),
                User.withUsername("taras").password("{noop}password").roles("USER").build()
        );
        
        // Set the in-memory manager in CustomUserDetailsService to avoid circular dependency
        CustomUserDetailsService.setInMemoryUserDetailsManager(inMemory);
        
        return inMemory;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Return the CustomUserDetailsService which uses hybrid auth
        return new CustomUserDetailsService();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

