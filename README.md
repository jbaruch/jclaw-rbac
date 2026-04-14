# JClaw RBAC

Spring Security 7 + Spring AI 2.0 -- mapping AI agent capabilities to user roles with RBAC.

The SAME `/chat` endpoint serves different users with different AI agent capabilities
based on their roles. Security enforcement happens at the Java method level with
`@PreAuthorize`, not in the prompt. The LLM can try to call tools it shouldn't --
Spring Security blocks execution before the tool runs.

## Architecture

```
curl -u admin:admin --> SecurityFilterChain --> AgentController
                           |                        |
                     HTTP Basic Auth          Reads Authentication
                     Role Hierarchy           Selects tools by role
                                              Builds per-request ChatClient
                                                    |
                                             +------+------+
                                             |             |
                                        AnalystTools  AdminTools
                                        @PreAuthorize @PreAuthorize
                                        hasRole       hasRole
                                        ANALYST       ADMIN
```

## Roles

| Role     | Chat | Conference Search | Time/Weather | List Users | Introspect |
|----------|------|-------------------|--------------|------------|------------|
| ADMIN    | Yes  | Yes               | Yes          | Yes        | Yes        |
| ANALYST  | Yes  | Yes               | Yes          | No         | No         |
| VIEWER   | Yes  | No                | No           | No         | No         |

Role hierarchy: ADMIN > ANALYST > VIEWER

## Running

```bash
export ANTHROPIC_API_KEY=your-key-here
./mvnw spring-boot:run
```

## Testing with curl

```bash
# ADMIN -- gets everything
curl -u admin:admin -X POST http://localhost:8080/chat -d "list all users"

# ANALYST -- can search conferences but not list users
curl -u analyst:analyst -X POST http://localhost:8080/chat -d "find java conferences"

# VIEWER -- chat only, tools blocked
curl -u viewer:viewer -X POST http://localhost:8080/chat -d "find java conferences"

# No auth -- 401
curl -X POST http://localhost:8080/chat -d "hello"
```

## Stack

- Spring Boot 4.0.5
- Spring Security 7.0.4
- Spring AI 2.0.0-M4
- Anthropic Claude (claude-sonnet-4-20250514)
- Java 21
