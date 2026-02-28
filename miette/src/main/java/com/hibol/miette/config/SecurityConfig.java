package com.hibol.miette.config;

import com.hibol.miette.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/recettes", "/recettes**", "/recette/**", "/css/**", "/js/**", "/login", "/login**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .successHandler((request, response, authentication) -> {
                String redirectTo = request.getParameter("redirectTo");
                if (redirectTo != null && redirectTo.startsWith("/")) {
                    response.sendRedirect(redirectTo);
                } else {
                    response.sendRedirect("/recettes");
                }
            })
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler((request, response, authentication) -> {
                String redirectTo = request.getParameter("redirectTo");
                if (redirectTo != null && redirectTo.startsWith("/")) {
                    response.sendRedirect(redirectTo);
                } else {
                    response.sendRedirect("/recettes");
                }
            })
            .permitAll()
        )
        .rememberMe(rm -> rm.key("miette-recettes").tokenValiditySeconds(86400)); // 24h
        
        return http.build();
    }
}

