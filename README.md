<div align="center">

# 🛠️ TECH CARE
### Integrated Electronic Appliance Service & Repair Management Platform

[![Python](https://img.shields.io/badge/Python-3.8+-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://python.org)
[![Flask](https://img.shields.io/badge/Flask-2.x-000000?style=for-the-badge&logo=flask&logoColor=white)](https://flask.palletsprojects.com/)
[![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Status](https://img.shields.io/badge/Status-Completed-success?style=for-the-badge)]()
[![Academic](https://img.shields.io/badge/Academic-Bachelor_Capstone_Project-blue?style=for-the-badge)]()

<p align="center">
  A multi-tier ecosystem connecting consumers, authorized electronic appliance manufacturers/service centers, and certified field technicians for streamlined maintenance, transparent estimates, live chat & video diagnostics, and authentic spare parts sourcing.
</p>

</div>

---

## 📌 Project Overview

**TECH CARE** is an end-to-end service management platform developed as a Bachelor Degree Capstone Project. It eliminates common pain points in after-sales electronic product repairs—such as counterfeit spare parts, arbitrary pricing, long service delays, and lack of real-time communication between customers and technicians.

The system incorporates a **Flask-powered web backend and administration portal** alongside a **native Android mobile application** to facilitate seamless coordination among four core stakeholders:
- **System Administrator (Web)**: Verifies company registrations, oversees dispute resolution, and monitors platform health.
- **Service Companies (Web)**: Maintain genuine appliances & spare parts catalogs, employ technicians, and fulfill parts requests.
- **Service Agents / Technicians (Mobile & Web)**: Accept repair requests, provide dynamic repair estimates, conduct video diagnostics, and order authentic OEM parts.
- **Customers / Users (Mobile)**: Register appliance complaints with photos, track repair stages in real time, communicate via live chat / video calls, review estimates, make payments, and leave feedback.

---

## 🚀 Key Features

### 1. 🛡️ Administrator Module (Web Portal)
- **Company Verification**: Review company credentials and license certificates; grant or revoke platform access.
- **System-Wide Monitoring**: Monitor complaints, assignment pipelines, and resolution metrics.
- **Ratings & Feedback Oversight**: Analyze performance reviews for quality assurance.

### 2. 🏢 Service Company Module (Web Portal)
- **Profile & Branch Management**: Configure authorized company profiles, service regions, and contact information.
- **Product & Spare Parts Catalog**: Manage product listings and authentic OEM spare parts inventory with transparent pricing.
- **Technician Workforce Management**: Onboard and monitor field technicians with baseline rates and technical specialties.
- **Parts Request Fulfillment**: Review and approve parts requisitions submitted by technicians for customer repairs.

### 3. 🔧 Service Agent / Technician Module (Mobile App / Web)
- **Real-Time Job Feeds**: Instant alerts on newly assigned repairs with complaint descriptions and defect photos.
- **Dynamic Cost Quotations**: Generate and send itemized labor and parts quotations to the customer for approval.
- **Authentic Parts Sourcing**: Order verified manufacturer spare parts directly through the linked company catalog.
- **Live Communication**: Integrated real-time chat and video consultation for remote pre-visit diagnostics.
- **Status Pipeline**: Progressively update repair stages (`Assigned` ➔ `In Progress` ➔ `Completed`).

### 4. 📱 Customer / End-User Module (Android Mobile App)
- **Account & Profile Management**: Secure sign-up and address management.
- **Lodge Complaints**: Select product category, describe malfunction, and attach photos directly from camera/gallery.
- **Real-Time Tracking**: Track repair status from assignment to final completion.
- **In-App Messaging & Video Call**: Consult directly with the assigned technician.
- **Digital Billing & Invoicing**: Review itemized charges and receive digital receipts.
- **Ratings & Reviews**: Submit star ratings and written reviews for technicians and service companies.

---

## 🛠️ Technology Stack

| Layer | Technologies Used |
| :--- | :--- |
| **Backend Framework** | [Flask (Python 3.x)](https://flask.palletsprojects.com/) |
| **Database** | [MySQL 8.0](https://www.mysql.com/) (with automatic [SQLite](https://www.sqlite.org/) fallback via `db_helper.py`) |
| **Database Drivers** | `pymysql`, `sqlite3` |
| **Web Frontend** | HTML5, CSS3, JavaScript, Jinja2 Template Engine |
| **Mobile App (Android)** | Native Android (Java), Google Material Design Components |
| **Mobile Networking** | [Volley HTTP Library](https://github.com/google/volley), Custom Multipart Request Handler |
| **Process Launcher** | Windows Batch Script (`start_project.bat`) |

---

## 📂 Project Directory Structure

```text
bachorlor/
├── android_app/                     # Native Android Studio Project
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/techcare/app/
│   │   │   │   ├── AppConfig.java          # Server IP and endpoint definitions
│   │   │   │   ├── LoginActivity.java       # User & technician authentication
│   │   │   │   ├── RegisterActivity.java    # Customer account registration
│   │   │   │   ├── UserHomeActivity.java    # Customer navigation hub
│   │   │   │   ├── TechHomeActivity.java    # Technician dashboard
│   │   │   │   ├── ComplaintActivity.java   # Lodge new appliance complaints
│   │   │   │   ├── ChatActivity.java        # In-app real-time messaging
│   │   │   │   ├── VideoCallActivity.java   # Video consultation interface
│   │   │   │   ├── EstimateActivity.java    # Quotation and estimation approval
│   │   │   │   ├── ReceiptActivity.java     # Digital billing and receipts
│   │   │   │   └── ...
│   │   │   ├── res/                         # Android UI layouts, drawables, strings
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   └── settings.gradle
├── backend/                         # Flask Web Backend & API Server
│   ├── static/                      # Static assets & user upload storage
│   │   └── uploads/                 # Uploaded product and complaint photos
│   ├── templates/                   # Web Portal Jinja2 HTML templates
│   │   ├── admin_dashboard.html     # Administrator management console
│   │   ├── company_dashboard.html   # Company management console
│   │   ├── login.html               # Web portal login screen
│   │   ├── register_company.html    # Company registration form
│   │   └── change_password.html     # Credential management
│   ├── app.py                       # Main Flask routing, controllers, & REST APIs
│   ├── db_helper.py                 # Unified database connection & query abstraction
│   └── techcare.db                  # Standalone SQLite database fallback
├── db/                              # Database Schemas & Migrations
│   └── database_setup.sql           # Complete MySQL DDL schema and default seed data
├── start_project.bat                # 1-click startup batch script
└── README.md                        # Project documentation
```

---

## ⚙️ Installation & Setup

### Prerequisites
- **Python 3.8+** installed and added to `PATH`
- **MySQL Server 8.0+** (optional; SQLite fallback works out of the box)
- **Android Studio** (for building and deploying the mobile app)
- **Pip dependencies**:
  ```bash
  pip install flask pymysql werkzeug
  ```

---

### 1. Database Setup (MySQL)
If you wish to use MySQL:
1. Open your MySQL client (e.g., MySQL Workbench or Command Line).
2. Execute the setup script:
   ```sql
   SOURCE db/database_setup.sql;
   ```
3. Update connection settings in [backend/db_helper.py](file:///d:/adheena%20personal%20thing/bachorlor/backend/db_helper.py) if your MySQL port, username, or password differ:
   ```python
   MYSQL_HOST = 'localhost'
   MYSQL_USER = 'root'
   MYSQL_PASSWORD = ''
   MYSQL_DB = 'techcare'
   ```
> **Note:** If MySQL is not running, the application will automatically fall back to the built-in SQLite database (`backend/techcare.db`).

---

### 2. Running the Backend Server

#### Option A: Quick Launch (Windows)
Double-click `start_project.bat` from the root folder, or run in terminal:
```bat
start_project.bat
```
This automatically boots MySQL, launches the Flask server on port 5000, and opens your default browser.

#### Option B: Manual Execution
Navigate to the project root and run:
```bash
python backend/app.py
```
The server will start listening at:
```text
http://127.0.0.1:5000/
```

---

### 3. Setting Up the Android App
1. Open **Android Studio**.
2. Select **Open an Existing Project** and choose the `android_app/` directory.
3. Open [AppConfig.java](file:///d:/adheena%20personal%20thing/bachorlor/android_app/app/src/main/java/com/techcare/app/AppConfig.java).
4. Update the `BASE_URL` with your local machine's IP address (e.g. `http://192.168.1.X:5000/` or `http://10.0.2.2:5000/` for standard Android Emulator).
5. Build and run the app on an Android Emulator or physical device connected to the same local Wi-Fi network.

---

## 🔑 Default Administrator Credentials

| Role | Username | Password | Access URL |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin` | `admin` | `http://127.0.0.1:5000/login` |

---

## 📄 License & Attribution

Developed for academic purposes as a Bachelor's Degree Capstone Project.
All rights reserved.
