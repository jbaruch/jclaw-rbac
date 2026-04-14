package com.codepocalypse.rbac;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Single /chat endpoint that serves a DIFFERENT AI agent persona per user.
 *
 * Same endpoint, completely different experience. The controller:
 * 1. Reads the authenticated user's username from SecurityContextHolder
 * 2. Looks up the persona system prompt for that specific user
 * 3. Selects which tool beans to register based on the user's role
 * 4. Builds a per-request ChatClient with the persona prompt and role-appropriate tools
 *
 * Elvis gets The King. Spock gets pure logic. Yoda gets no tools.
 * The role still controls access. The persona controls the vibe.
 *
 * Defense in depth: the controller filters tools by role (so the LLM doesn't
 * even see tools it can't use), AND @PreAuthorize blocks execution if something
 * slips through. Belt AND suspenders.
 */
@RestController
public class AgentController {

    private final ChatClient.Builder chatClientBuilder;
    private final AnalystTools analystTools;
    private final AdminTools adminTools;

    public AgentController(ChatClient.Builder chatClientBuilder,
                           AnalystTools analystTools,
                           AdminTools adminTools) {
        this.chatClientBuilder = chatClientBuilder;
        this.analystTools = analystTools;
        this.adminTools = adminTools;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        // Get the persona for this specific user
        AgentConfig.Persona persona = AgentConfig.getPersona(username);

        // Select tools based on role -- defense in depth layer 1
        List<Object> tools = new ArrayList<>();
        if (hasRole(auth, "ROLE_ANALYST")) {
            tools.add(analystTools);
        }
        if (hasRole(auth, "ROLE_ADMIN")) {
            tools.add(adminTools);
        }

        // Build a per-request ChatClient with persona-specific system prompt
        var builder = chatClientBuilder.clone();
        builder.defaultSystem(persona.systemPrompt());
        if (!tools.isEmpty()) {
            builder.defaultTools(tools.toArray());
        }
        ChatClient client = builder.build();

        return client.prompt()
                .system(s -> s
                        .param("username", username)
                        .param("roles", roles))
                .user(message)
                .call()
                .content();
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
