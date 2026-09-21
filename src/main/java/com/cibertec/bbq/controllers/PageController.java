package com.cibertec.bbq.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sirve las páginas HTML (static/pages) en las mismas URLs del sistema Laravel.
 * Cada página lee sus ids de location.pathname y pide los datos a /api.
 */
@Controller
public class PageController {

    // ---- Redirecciones ----
    @GetMapping("/")
    public String root() { return "redirect:/company/facilities"; }

    @GetMapping("/admin")
    public String adminHome() { return "redirect:/admin/companies"; }

    @GetMapping("/company")
    public String companyHome() { return "redirect:/company/facilities"; }

    // ---- Admin ----
    @GetMapping("/admin/login")
    public String adminLogin() { return "forward:/pages/admin/login.html"; }

    @GetMapping("/admin/companies")
    public String adminCompanies() { return "forward:/pages/admin/companies.html"; }

    @GetMapping("/admin/roles")
    public String adminRoles() { return "forward:/pages/admin/roles.html"; }

    // ---- Empresa ----
    @GetMapping("/company/login")
    public String companyLogin() { return "forward:/pages/company/login.html"; }

    @GetMapping("/company/profile")
    public String profile() { return "forward:/pages/company/profile.html"; }

    @GetMapping("/company/employees")
    public String employees() { return "forward:/pages/company/employees.html"; }

    @GetMapping("/company/facilities")
    public String facilities() { return "forward:/pages/company/facilities.html"; }

    @GetMapping("/company/facilities/{facilityId}/locations")
    public String locations() { return "forward:/pages/company/locations.html"; }

    @GetMapping("/company/facilities/{facilityId}/products")
    public String products() { return "forward:/pages/company/products.html"; }

    @GetMapping("/company/facilities/{facilityId}/booking/getBookingsByFacility")
    public String bookingList() { return "forward:/pages/company/booking-list.html"; }

    @GetMapping("/company/locations/{locationId}/bookings")
    public String bookings() { return "forward:/pages/company/bookings.html"; }

    @GetMapping("/company/locations/{locationId}/closeddays")
    public String closedDays() { return "forward:/pages/company/closeddays.html"; }

    // ---- Portal (público) ----
    @GetMapping("/facility/{uuid}")
    public String portalFacility() { return "forward:/pages/portal/facility.html"; }

    @GetMapping("/location/{uuid}")
    public String portalLocation() { return "forward:/pages/portal/location.html"; }

    @GetMapping("/booking-details/{uuid}")
    public String portalBookingDetails() { return "forward:/pages/portal/booking-details.html"; }
}
