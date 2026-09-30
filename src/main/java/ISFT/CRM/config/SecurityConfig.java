package ISFT.CRM.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ISFT.CRM.service.CustomUserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import ISFT.CRM.security.JwtAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .userDetailsService(userDetailsService)

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/auth/login").permitAll()

                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "MANAGER")



                        .requestMatchers("/api/manager/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers("/api/agent/**")
                        .hasAnyRole("ADMIN", "MANAGER", "AGENT")

                        .requestMatchers(HttpMethod.DELETE, "/api/leads/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/leads/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/leads/**")
                        .hasAnyRole("ADMIN", "MANAGER", "AGENT")

                        .requestMatchers("/api/lead-assignments/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers("/api/call-logs/**")
                        .hasAnyRole("ADMIN", "MANAGER", "AGENT")

                        .requestMatchers("/api/follow-ups/**")
                        .hasAnyRole("ADMIN", "MANAGER", "AGENT")

                        .requestMatchers("/api/notes/**")
                        .hasAnyRole("ADMIN", "MANAGER", "AGENT")

                        .requestMatchers("/api/courses/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers("/api/teams/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers("/api/campaigns/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}