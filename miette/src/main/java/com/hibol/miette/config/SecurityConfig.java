package com.hibol.miette.config;

import com.hibol.miette.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.remember-me-key}")
    private String rememberMeKey;

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
    public SecurityFilterChain filterChain(HttpSecurity http, LoginRateLimiter rateLimiter) throws Exception {
        http.addFilterBefore(new LoginRateLimitFilter(rateLimiter), UsernamePasswordAuthenticationFilter.class);
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/recettes", "/recettes**", "/recette/**", "/a-propos", "/ca-veut-dire-quoi",
                "/css/**", "/js/**", "/api/**", "/images/**",
                "/styles.css", "/*.svg", "/*.webp",
                "/favicon.ico", "/robots.txt", "/sitemap.xml"
            ).permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/recettes")
            .loginProcessingUrl("/login")
            .successHandler((request, response, authentication) -> {
                rateLimiter.reset(request.getRemoteAddr());
                response.setStatus(200);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":true}");
            })
            .failureHandler((request, response, exception) -> {
                rateLimiter.recordFailure(request.getRemoteAddr());
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false}");
            })
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler((request, response, authentication) -> {
                String redirectTo = request.getParameter("redirectTo");
                if (redirectTo != null && redirectTo.startsWith("/") && !redirectTo.startsWith("/admin")) {
                    response.sendRedirect(redirectTo);
                } else {
                    response.sendRedirect("/recettes");
                }
            })
            .permitAll()
        )
        .rememberMe(rm -> rm.key(rememberMeKey).tokenValiditySeconds(30 * 24 * 60 * 60));
        
        return http.build();
    }
}

