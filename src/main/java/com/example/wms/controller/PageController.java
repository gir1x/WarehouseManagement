package com.example.wms.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Serves the HTML pages — separate from the REST API controllers. */
@Controller
public class PageController {

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    public PageController(ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
        this.clientRegistrations = clientRegistrations;
    }

    @GetMapping("/")
    public String index() {
        // No page is mapped to "/" itself — without this, an authenticated user hitting
        // http://localhost:8080/ got a blank 404. Redirecting to /dashboard gives every
        // visit a real landing page either way (SecurityConfig sends unauthenticated
        // visitors to /login automatically).
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login(Model model) {
        // Same check SecurityConfig uses to decide whether to enable oauth2Login() at all —
        // only show the "Continue with Google" button if a provider is actually configured.
        model.addAttribute("oauth2Enabled", clientRegistrations.getIfAvailable() != null);
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    /** Token comes from the emailed link as a query param — read client-side via JS. */
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    /** List + create page. Data is loaded client-side from GET/POST /api/warehouses. */
    @GetMapping("/warehouses")
    public String warehouses() {
        return "warehouses";
    }

    /** Grid + receive + pick + report page for one warehouse. */
    @GetMapping("/warehouses/{id}")
    public String warehouseDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("warehouseId", id.toString());
        return "warehouse-detail";
    }

    /** Product catalog page. Create form is ADMIN-only (hidden client-side); list is ADMIN + VISITOR. */
    @GetMapping("/products")
    public String products() {
        return "products";
    }

    /** ADMIN-only page for approving/promoting accounts. */
    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin-users";
    }
}
