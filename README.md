# PaymentHub (PMH-Project)

1. **Tham số Cấu phần xử lý (Components Management)**
2. **Tham số Danh mục theo nhóm (Group Categories Management)**

---

## Kiến trúc hệ thống

```
PMH-Project/
├── PaymentHub/       # Backend (Spring Boot 3, Java 17, JPA/Hibernate, Oracle 19c)
└── pmh-frontend/     # Frontend (Angular 21, Taiga UI v5, AG Grid Community v36)
```

---

# Những việc đã làm:
- Viết các dạng API: 
    + JPA: trong các repository.
    + Native Query: ví dụ trong ComponentRepository.
    + Procedure: Có thử viết trong DB và BE gọi nó ở method searchComponent() trong ComponentServiceImpl.
    + Specification: dùng cho chức năng tìm kiếm bằng bộ lọc ở cả 2 phần Component và GroupCategory.
- Viết các API phục vụ các chức năng có trong tài liệu RSD(lấy danh sách, tìm kiếm qua bộ lọc, thêm, sửa, xóa, lưu, gửi duyệt, duyệt, từ chối duyệt), có kiểm thử bằng Postman trước khi tích hợp vào giao diện người dùng, chưa có unit test.
- Có thử viết xác thực và phân quyền với JWT để thực hiện đăng nhập.

- Xây dựng giao diện bằng Angular và Taiga UI:
    + Các bảng dùng AG Grid.
    + Taiga UI: các nút bấm, các ô input,...
