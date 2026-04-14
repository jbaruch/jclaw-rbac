package com.codepocalypse.rbac;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 7 configuration for AI agent RBAC.
 *
 * Five users, each with a unique AI agent persona:
 * - elvis  (ADMIN)   -> The King -- all tools, dramatic confidence
 * - spock  (ADMIN)   -> Mr. Spock -- all tools, pure logic
 * - godfather (ANALYST) -> Don Corleone -- analytical tools, menacing wisdom
 * - pirate (ANALYST) -> Captain Jack Sparrow -- analytical tools, rum-soaked nonsense
 * - yoda   (VIEWER)  -> Master Yoda -- no tools, inverted wisdom only
 *
 * Role hierarchy ensures ADMIN inherits ANALYST permissions, and ANALYST inherits VIEWER.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/**", "/chat").authenticated()
                        .anyRequest().authenticated())
                .httpBasic(basic -> {});
        return http.build();
    }

    @Bean
    InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        var elvis = User.builder()
                .username("elvis")
                .password(passwordEncoder.encode("elvis"))
                .roles("ADMIN")
                .build();
        var spock = User.builder()
                .username("spock")
                .password(passwordEncoder.encode("spock"))
                .roles("ADMIN")
                .build();
        var godfather = User.builder()
                .username("godfather")
                .password(passwordEncoder.encode("godfather"))
                .roles("ANALYST")
                .build();
        var pirate = User.builder()
                .username("pirate")
                .password(passwordEncoder.encode("pirate"))
                .roles("ANALYST")
                .build();
        var yoda = User.builder()
                .username("yoda")
                .password(passwordEncoder.encode("yoda"))
                .roles("VIEWER")
                .build();
        return new InMemoryUserDetailsManager(elvis, spock, godfather, pirate, yoda);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_ANALYST
                ROLE_ANALYST > ROLE_VIEWER
                """);
    }
}
