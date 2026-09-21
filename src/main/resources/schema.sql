-- Esquema de BBQ Reservas (MySQL 8.4).
-- Diagrama entregado + decisiones D1-D4 de PLAN_IMPLEMENTACION.md.
-- Se ejecuta en cada arranque: solo crea lo que no existe.

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type INT NOT NULL,                              -- 1 administrador de empresa, 2 empleado
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS companies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  phone VARCHAR(20) NOT NULL,
  address VARCHAR(255) NULL,
  status SMALLINT NOT NULL DEFAULT 1,             -- 1 activa, 0 inactiva
  uuid CHAR(36) NOT NULL UNIQUE,
  url VARCHAR(255) NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS employees (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20) NULL,
  role_id BIGINT NOT NULL,
  remember_token VARCHAR(100) NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_employees_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_employees_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS facilities (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  address VARCHAR(255) NULL,
  description TEXT NULL,
  uuid CHAR(36) NOT NULL UNIQUE,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_facilities_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE TABLE IF NOT EXISTS employee_facility (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  facility_id BIGINT NOT NULL,
  created_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_ef_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
  CONSTRAINT fk_ef_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);

CREATE TABLE IF NOT EXISTS locations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  facility_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT NULL,
  uuid CHAR(36) NOT NULL UNIQUE,
  max_person INT NULL,                            -- NULL = exclusiva (1 reserva por día)
  price DECIMAL(10,2) NOT NULL DEFAULT 0,         -- D2: precio por reserva
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_locations_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);

CREATE TABLE IF NOT EXISTS closed_days (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  location_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  reason TEXT NULL,
  facility_id BIGINT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_cd_location FOREIGN KEY (location_id) REFERENCES locations(id),
  CONSTRAINT fk_cd_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);

CREATE TABLE IF NOT EXISTS products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  facility_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_products_facility FOREIGN KEY (facility_id) REFERENCES facilities(id)
);

CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,                    -- D1
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL,                    -- D3: sin UNIQUE
  phone VARCHAR(20) NOT NULL,
  date DATE NOT NULL,
  employee_id BIGINT NULL,                        -- quién la registró (NULL = portal)
  uuid CHAR(36) NOT NULL UNIQUE,
  number_persons INT NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  INDEX idx_bookings_email (email),
  INDEX idx_bookings_location_date (location_id, date),
  CONSTRAINT fk_bookings_company FOREIGN KEY (company_id) REFERENCES companies(id),
  CONSTRAINT fk_bookings_location FOREIGN KEY (location_id) REFERENCES locations(id),
  CONSTRAINT fk_bookings_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS booking_details (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL,
  product_id BIGINT NULL,
  price DECIMAL(10,2) NOT NULL,                   -- precio unitario al momento de reservar
  total DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_bd_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
  CONSTRAINT fk_bd_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS sales (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT NOT NULL UNIQUE,              -- 1:1 con bookings
  location_price DECIMAL(10,2) NOT NULL,
  product_total DECIMAL(10,2) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  status SMALLINT NOT NULL,                       -- 1 pendiente, 2 pagado, 3 cancelado, 4 expirado
  paid_at TIMESTAMP NULL,
  payment_deadline_date DATE NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  deleted_at TIMESTAMP NULL,
  CONSTRAINT fk_sales_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
