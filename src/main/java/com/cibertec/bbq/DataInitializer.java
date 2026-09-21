package com.cibertec.bbq;

import com.cibertec.bbq.models.*;
import com.cibertec.bbq.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos iniciales: roles y una empresa demo completa (solo si las tablas están vacías). */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeFacilityRepository employeeFacilityRepository;
    private final FacilityRepository facilityRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            roleRepository.save(role("Administrador", AppConstants.ROLE_TYPE_COMPANY_ADMIN));
            roleRepository.save(role("Empleado", AppConstants.ROLE_TYPE_EMPLOYEE));
            System.out.println(">>> Roles creados: Administrador (tipo 1), Empleado (tipo 2)");
        }
        if (companyRepository.count() == 0) {
            seedDemoCompany();
        }
    }

    private void seedDemoCompany() {
        Role adminRole = roleRepository.findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(AppConstants.ROLE_TYPE_COMPANY_ADMIN).orElseThrow();
        Role employeeRole = roleRepository.findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(AppConstants.ROLE_TYPE_EMPLOYEE).orElseThrow();

        Company company = new Company();
        company.setName("BBQ Demo");
        company.setEmail("contacto@bbqdemo.com");
        company.setPhone("01-555-0000");
        company.setAddress("Av. Principal 123, Lima");
        company.setUrl("https://bbqdemo.com");
        company.setStatus(AppConstants.COMPANY_ACTIVE);
        company.setUuid(UUID.randomUUID().toString());
        companyRepository.save(company);

        employeeRepository.save(employee(company, "Representante Demo", "demo@bbq.com", adminRole));
        Employee staff = employeeRepository.save(employee(company, "Empleado Demo", "empleado@bbq.com", employeeRole));

        Facility facility = new Facility();
        facility.setCompany(company);
        facility.setName("Parque BBQ Central");
        facility.setEmail("central@bbqdemo.com");
        facility.setPhone("01-555-0001");
        facility.setAddress("Jr. Los Pinos 456, Lima");
        facility.setDescription("Zonas de parrilla al aire libre con estacionamiento.");
        facility.setUuid(UUID.randomUUID().toString());
        facilityRepository.save(facility);
        employeeFacilityRepository.save(new EmployeeFacility(staff, facility));

        locationRepository.save(location(facility, "Zona Parrilla A", "Área común con 5 parrillas.", 20, "150.00"));
        locationRepository.save(location(facility, "Quincho Familiar", "Quincho privado: una reserva por día.", null, "300.00"));

        productRepository.save(product(facility, "Carbón 5 kg", "25.00"));
        productRepository.save(product(facility, "Kit de utensilios", "15.00"));
        productRepository.save(product(facility, "Pack de bebidas x6", "30.00"));

        System.out.println(">>> Empresa demo creada. Login empresa: demo@bbq.com / demo1234 (tipo 1), empleado@bbq.com / demo1234 (tipo 2)");
    }

    private Role role(String name, int type) {
        Role r = new Role();
        r.setName(name);
        r.setType(type);
        return r;
    }

    private Employee employee(Company company, String name, String email, Role role) {
        Employee e = new Employee();
        e.setCompany(company);
        e.setName(name);
        e.setEmail(email);
        e.setPassword(passwordEncoder.encode("demo1234"));
        e.setPhone("999-000-000");
        e.setRole(role);
        return e;
    }

    private Location location(Facility facility, String name, String description, Integer maxPerson, String price) {
        Location l = new Location();
        l.setFacility(facility);
        l.setName(name);
        l.setDescription(description);
        l.setMaxPerson(maxPerson);
        l.setPrice(new BigDecimal(price));
        l.setUuid(UUID.randomUUID().toString());
        return l;
    }

    private Product product(Facility facility, String name, String price) {
        Product p = new Product();
        p.setFacility(facility);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        return p;
    }
}
