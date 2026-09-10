package com.example.wms.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize on service methods — see §7.5 "Defense in Depth"
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            CustomOidcUserService oidcUserService,
                                            ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // public: no account needed to reach these
                .requestMatchers("/login", "/register", "/forgot-password", "/reset-password", "/css/**", "/js/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()

                // ADMIN-only pages and endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/warehouses").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/warehouses/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                // ADMIN + VISITOR — day-to-day operations (PENDING accounts are blocked here
                // until an admin promotes them, which is the whole point of that role)
                .requestMatchers(HttpMethod.GET, "/api/warehouses").hasAnyRole("ADMIN", "VISITOR")
                .requestMatchers("/api/warehouses/**").hasAnyRole("ADMIN", "VISITOR")
                .requestMatchers(HttpMethod.GET, "/api/products").hasAnyRole("ADMIN", "VISITOR")
                .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "VISITOR")

                // everything else just needs to be signed in (e.g. /api/me, /dashboard,
                // and the page shells for /warehouses, /products — their data calls are
                // still gated by the rules above)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        // OAuth2 login is only wired up if a provider is actually configured in
        // application.yml. This keeps the app runnable with form-login alone
        // out of the box, and lets you turn Google sign-in on later without
        // touching this class — see application.yml for how.
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                    .defaultSuccessUrl("/dashboard", true)
            );
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
