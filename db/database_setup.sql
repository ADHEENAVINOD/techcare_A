-- Database creation
CREATE DATABASE IF NOT EXISTS techcare;
USE techcare;

-- 1. LOGIN Table
CREATE TABLE IF NOT EXISTS login (
    lid INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. USER Table
CREATE TABLE IF NOT EXISTS user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    user_lid INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    photo VARCHAR(255) DEFAULT NULL,
    house VARCHAR(150) NOT NULL,
    place VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pin VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    FOREIGN KEY (user_lid) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. COMPANY Table
CREATE TABLE IF NOT EXISTS company (
    co_id INT AUTO_INCREMENT PRIMARY KEY,
    co_lid INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    house VARCHAR(150) NOT NULL,
    district VARCHAR(100) NOT NULL,
    pin_code VARCHAR(20) NOT NULL,
    license VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'Pending',
    FOREIGN KEY (co_lid) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. SERVICE AGENT (Technician) Table
CREATE TABLE IF NOT EXISTS serviceagent (
    sa_id INT AUTO_INCREMENT PRIMARY KEY,
    sagent_lid INT NOT NULL,
    company_id INT NOT NULL, -- references company.co_lid (login ID of the company)
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    place VARCHAR(150) NOT NULL,
    starting_rate INT DEFAULT 300,
    specialties VARCHAR(255) DEFAULT 'General Electronics',
    FOREIGN KEY (sagent_lid) REFERENCES login(lid) ON DELETE CASCADE,
    FOREIGN KEY (company_id) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. PRODUCT Table
CREATE TABLE IF NOT EXISTS product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    com_lid INT NOT NULL, -- references company.co_lid (login ID of the company)
    amount VARCHAR(50) NOT NULL,
    photo VARCHAR(255) DEFAULT NULL,
    description VARCHAR(255) DEFAULT NULL,
    FOREIGN KEY (com_lid) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. COMPLAINT Table
CREATE TABLE IF NOT EXISTS complaint (
    cid INT AUTO_INCREMENT PRIMARY KEY,
    complaint VARCHAR(1000) NOT NULL,
    pid INT NOT NULL, -- references product.product_id
    date VARCHAR(50) NOT NULL,
    cphoto VARCHAR(255) DEFAULT NULL,
    status VARCHAR(50) DEFAULT 'Pending',
    estimate_amount VARCHAR(50) DEFAULT 'Pending',
    user_id INT NOT NULL, -- references login.lid of the user
    FOREIGN KEY (pid) REFERENCES product(product_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. ASSIGN Table
CREATE TABLE IF NOT EXISTS assign (
    aid INT AUTO_INCREMENT PRIMARY KEY,
    cid INT NOT NULL, -- references complaint.cid
    tid INT NOT NULL, -- references serviceagent.sagent_lid (login ID of the technician)
    remark VARCHAR(255) DEFAULT NULL,
    status VARCHAR(50) DEFAULT 'Assigned',
    chat_status VARCHAR(50) DEFAULT 'Active',
    FOREIGN KEY (cid) REFERENCES complaint(cid) ON DELETE CASCADE,
    FOREIGN KEY (tid) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. RATING Table
CREATE TABLE IF NOT EXISTS rating (
    rating_id INT AUTO_INCREMENT PRIMARY KEY,
    user_lid INT NOT NULL, -- references login.lid of the user
    service_lid INT NOT NULL, -- references login.lid of the company or technician (usually company)
    rating VARCHAR(50) NOT NULL,
    date VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_lid) REFERENCES login(lid) ON DELETE CASCADE,
    FOREIGN KEY (service_lid) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. FEEDBACK Table
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id INT AUTO_INCREMENT PRIMARY KEY,
    sagent_id INT NOT NULL, -- references login.lid of the service agent (technician)
    user_lid INT NOT NULL, -- references login.lid of the user
    feedback VARCHAR(1000) NOT NULL,
    date VARCHAR(50) NOT NULL,
    FOREIGN KEY (sagent_id) REFERENCES login(lid) ON DELETE CASCADE,
    FOREIGN KEY (user_lid) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. LIVE CHAT Table
CREATE TABLE IF NOT EXISTS live_chat (
    chat_id INT AUTO_INCREMENT PRIMARY KEY,
    from_id INT NOT NULL, -- references login.lid
    to_id INT NOT NULL, -- references login.lid
    date VARCHAR(50) NOT NULL,
    time VARCHAR(50) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    FOREIGN KEY (from_id) REFERENCES login(lid) ON DELETE CASCADE,
    FOREIGN KEY (to_id) REFERENCES login(lid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. PARTS REQUEST Table (Authentic Sourcing)
CREATE TABLE IF NOT EXISTS parts_request (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    cid INT NOT NULL, -- references complaint.cid
    product_id INT NOT NULL, -- references product.product_id (requested part)
    status VARCHAR(50) DEFAULT 'Pending',
    FOREIGN KEY (cid) REFERENCES complaint(cid) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default admin account
INSERT INTO login (lid, username, password, type) 
VALUES (1, 'admin', 'admin', 'admin')
ON DUPLICATE KEY UPDATE username='admin';
