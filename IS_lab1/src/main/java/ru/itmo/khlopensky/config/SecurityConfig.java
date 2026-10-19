package ru.itmo.khlopensky.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    public InMemoryUserDetailsManager users(
            @Value("${app.users.first.username:user1}") String first,
            @Value("${app.users.first.password:user1}") String firstPassword,
            @Value("${app.users.second.username:user2}") String second,
            @Value("${app.users.second.password:user2}") String secondPassword) {
        var encoder = new BCryptPasswordEncoder();
        return new InMemoryUserDetailsManager(
                User.withUsername(first).password("{bcrypt}" + encoder.encode(firstPassword)).roles("USER").build(),
                User.withUsername(second).password("{bcrypt}" + encoder.encode(secondPassword)).roles("USER").build());
    }

    @Bean
    public SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(form -> form.defaultSuccessUrl("/", true))
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        (request, response, exception) -> response.sendError(401),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .build();
    }
}
