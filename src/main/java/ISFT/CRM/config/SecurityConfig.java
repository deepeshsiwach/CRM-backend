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

                // ==================================================
                // CSRF
                // ==================================================

                .csrf(csrf -> csrf.disable())


                // ==================================================
                // CORS
                // ==================================================

                .cors(cors -> {})


                // ==================================================
                // SESSION
                // ==================================================

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                // ==================================================
                // USER DETAILS SERVICE
                // ==================================================

                .userDetailsService(userDetailsService)


                // ==================================================
                // AUTHORIZATION
                // ==================================================

                .authorizeHttpRequests(auth -> auth


                        // ==================================================
                        // LOGIN
                        // ==================================================

                        .requestMatchers("/api/auth/login")
                        .permitAll()


                        // ==================================================
                        // USERS
                        // ==================================================

                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")


                        // ==================================================
                        // ADMIN
                        // ==================================================

                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")


                        // ==================================================
                        // DASHBOARD
                        // ==================================================

                        .requestMatchers("/api/dashboard/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // ==================================================
                        // MANAGER
                        // ==================================================

                        .requestMatchers("/api/manager/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // ==================================================
                        // AGENT
                        // ==================================================

                        .requestMatchers("/api/agent/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "AGENT"
                        )


                        // ==================================================
                        // LEADS - DELETE
                        // ==================================================

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/leads/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // ==================================================
                        // LEADS - PUT
                        // ==================================================

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/leads/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // ==================================================
                        // LEADS
                        // ==================================================

                        .requestMatchers("/api/leads/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "AGENT"
                        )


                        // ==================================================
                        // LEAD ASSIGNMENTS
                        // ==================================================

                        .requestMatchers("/api/lead-assignments/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // ==================================================
                        // CALL LOGS
                        // ==================================================

                        .requestMatchers("/api/call-logs/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "AGENT"
                        )


                        // ==================================================
                        // FOLLOW-UPS
                        // ==================================================

                        .requestMatchers("/api/follow-ups/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "AGENT"
                        )


                        // ==================================================
                        // NOTES
                        // ==================================================

                        .requestMatchers("/api/notes/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "AGENT"
                        )


                        // ==================================================
                        // COURSES
                        // ==================================================

                        .requestMatchers("/api/courses/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // ==================================================
                        // TEAMS
                        //
                        // AGENT CAN NOW READ TEAMS FOR
                        // THE TRANSFER LEAD POPUP
                        // ==================================================

                        .requestMatchers("/api/teams/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "AGENT"
                        )


                        // ==================================================
                        // CAMPAIGNS
                        // ==================================================

                        .requestMatchers("/api/campaigns/**")
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )


                        // ==================================================
                        // EVERYTHING ELSE
                        // ==================================================

                        .anyRequest()
                        .authenticated()

                )


                // ==================================================
                // JWT FILTER
                // ==================================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();

    }

}