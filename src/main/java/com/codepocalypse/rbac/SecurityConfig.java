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
 * Three roles control which AI tools are available:
 * - ADMIN  -> all tools (conference search, time, weather, user management, introspect)
 * - ANALYST -> analytical tools (conference search, time, weather)
 * - VIEWER  -> chat only, no tools
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
        var admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .roles("ADMIN")
                .build();
        var analyst = User.builder()
                .username("analyst")
                .password(passwordEncoder.encode("analyst"))
                .roles("ANALYST")
                .build();
        var viewer = User.builder()
                .username("viewer")
                .password(passwordEncoder.encode("viewer"))
                .roles("VIEWER")
                .build();
        return new InMemoryUserDetailsManager(admin, analyst, viewer);
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
