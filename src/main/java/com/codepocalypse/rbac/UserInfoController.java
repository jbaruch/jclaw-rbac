package com.codepocalypse.rbac;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Returns the current user's identity, roles, and available tools.
 *
 * The tool list mirrors the same role-based logic in AgentController:
 * - VIEWER: no tools (chat only)
 * - ANALYST: getCurrentTime, getWeather, searchCfps
 * - ADMIN: all of the above + listUsers, introspect
 */
@RestController
public class UserInfoController {

    @GetMapping("/api/me")
    public Map<String, Object> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        List<String> tools = new ArrayList<>();
        if (hasRole(auth, "ROLE_ANALYST")) {
            tools.add("getCurrentTime");
            tools.add("getWeather");
            tools.add("searchCfps");
        }
        if (hasRole(auth, "ROLE_ADMIN")) {
            tools.add("listUsers");
            tools.add("introspect");
        }

        return Map.of(
                "username", username,
                "roles", roles,
                "tools", tools
        );
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
