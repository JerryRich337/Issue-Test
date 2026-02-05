package com.portfolio.supportsim.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

    http.authorizeHttpRequests(
        auth ->
            auth
                .requestMatchers("/api/health")
                .permitAll()
                .requestMatchers("/actuator/health")
                .permitAll()
              .requestMatchers("/h2-console/**")
              .permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/issues/**")
                .hasRole("ADMIN")
                .requestMatchers("/api/issues/*/audit")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/issues/**")
                .hasAnyRole("AGENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/issues/**")
                .hasAnyRole("AGENT", "ADMIN")
                .requestMatchers("/api/**")
                .authenticated()
                .anyRequest()
                .denyAll());

    http.httpBasic(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    UserDetails agent =
        User.withUsername("agent")
            .password(encoder.encode("agentpass"))
            .roles("AGENT")
            .build();

    UserDetails admin =
        User.withUsername("admin")
            .password(encoder.encode("adminpass"))
            .roles("ADMIN")
            .build();

    return new InMemoryUserDetailsManager(agent, admin);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
