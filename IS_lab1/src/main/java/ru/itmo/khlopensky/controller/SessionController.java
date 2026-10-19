package ru.itmo.khlopensky.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.Map;

@RestController
public class SessionController {
    @GetMapping("/api/session")
    public Map<String, String> session(Principal principal, CsrfToken token) {
        return Map.of("username", principal.getName(), "headerName", token.getHeaderName(), "token", token.getToken());
    }
}
