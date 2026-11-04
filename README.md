# PaymentHub (PMH-Project)

Hệ thống quản lý tham số cấu hình thanh toán tập trung (Payment Hub), bao gồm hai phân hệ chính:
1. **Tham số Cấu phần xử lý (Components Management)**
2. **Tham số Danh mục theo nhóm (Group Categories Management)**

---

## 🛠 Kiến trúc hệ thống

```
PMH-Project/
├── PaymentHub/       # Backend (Spring Boot 3, Java 17, JPA/Hibernate, Oracle 19c)
└── pmh-frontend/     # Frontend (Angular 21, Taiga UI v5, AG Grid Community v36)
```

---

## ⚙️ Yêu cầu môi trường
- **Java**: JDK 17+
- **Database**: Oracle Database 19c
- **Node.js**: v18+ / v20+
- **Angular CLI**: v21+

---

## 🚀 Hướng dẫn khởi chạy

### 1. Khởi chạy Backend (PaymentHub)
```bash
cd PaymentHub
# Cấu hình chuỗi kết nối Oracle trong src/main/resources/application.properties
./mvnw spring-boot:run
```
Backend API sẽ chạy tại: `http://localhost:8080`

### 2. Khởi chạy Frontend (pmh-frontend)
```bash
cd pmh-frontend
npm install
npm start
# hoặc: npx ng serve --port 4200
```
Frontend Web App sẽ chạy tại: `http://localhost:4200`

---

## 🔐 Tài khoản thử nghiệm
- **Tài khoản**: `abcc@gmail.com`
- **Mật khẩu**: `111111`
