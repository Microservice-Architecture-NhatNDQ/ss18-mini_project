# 🧪 Kịch Bản Kiểm Thử & Minh Chứng Hệ Thống (Test Scenarios & Verification)
## Dự án: Ngân Hàng Số RikkeiBank API

---

## 1. Danh Sách Kịch Bản Kiểm Thử Bắt Buộc

| STT | Kịch Bản | Mục Đích | Kỳ Vọng Kết Quả |
|---|---|---|---|
| **1** | Đăng nhập Admin / Teller / Customer | Xác thực JWT & Phân quyền RBAC | Trả về `accessToken`, `refreshToken`, `role` tương ứng |
| **2** | Đăng xuất ép buộc (Force-Logout) | Kiểm tra Redis Token Blacklist | Sau khi Admin thu hồi, request tiếp theo trả về `401 Unauthorized` |
| **3** | Tra cứu thông tin có Cache | Kiểm tra Redis Cache-Aside | Lần 1 log `Cache MISS! Querying DB`, Lần 2 phản hồi tức thì từ Redis |
| **4** | Chuyển khoản thành công (Happy Path) | Kiểm tra Saga 2 bước | Trừ tiền TK A, cộng tiền TK B, phát sự kiện Kafka, thông báo SSE |
| **5** | Chuyển khoản Rollback (Compensating) | Kiểm tra phục hồi dữ liệu khi lỗi | Trừ tiền TK A thành công, cộng TK B lỗi -> Hoàn tiền lại cho TK A |
| **6** | Ngắt mạch Circuit Breaker | Kiểm tra khả năng chịu lỗi (Resilience4j) | Khi Account Service ngừng, Gateway trả về Fallback `503 Service Unavailable` |

---

## 2. Hướng Dẫn Thực Hiện Chi Tiết Trên Postman

### Kịch Bản 1: Đăng nhập và Lấy Token
1. Mở Postman, chọn request `01. Identity & Auth Service -> 1.1 Login as Admin`.
2. Bấm **Send**. Kiểm tra tab *Environment*: biến `adminToken` tự động được cập nhật.
3. Chạy `1.3 Login as Customer`. Biến `customerToken` và `refreshToken` tự động được lưu.

### Kịch Bản 2: Kiểm tra Ép Buộc Đăng Xuất (Admin Force Logout)
1. Dùng `customerToken` gửi request `02. Customer & Catalog Service -> 2.5 Get Customer Profile By ID` -> Nhận kết quả `200 OK`.
2. Admin gọi `01. Identity & Auth Service -> 1.6 Admin Force Logout (User ID 3)`.
3. Gửi lại request `2.5 Get Customer Profile By ID` -> Nhận ngay mã lỗi `401 Unauthorized: Session has been revoked by Administrator`.

### Kịch Bản 3: Chuyển Khoản Thành Công (Saga Happy Path)
1. Mở request `04. Transaction & Saga Transfer Service -> 4.1 SAGA Transfer - Happy Path`.
2. Body request:
   ```json
   {
     "fromAccountNumber": "1000123456",
     "toAccountNumber": "1000654321",
     "amount": 5000000,
     "description": "Chuyen tien thanh toan don hang"
   }
   ```
3. Bấm **Send** -> Nhận phản hồi `201 Created` với trạng thái `SUCCESS`.
4. Tra cứu lại số dư của 2 tài khoản: TK 1000123456 còn `95.000.000 VND`, TK 1000654321 lên `55.000.000 VND`.

### Kịch Bản 4: Chuyển Khoản Rollback Có Sự Kiện Bù Trừ
1. Mở request `4.2 SAGA Transfer - Rollback (Simulate Failure & Compensating Refund)`.
2. Body request có cờ `"simulateCreditFailure": true`:
   ```json
   {
     "fromAccountNumber": "1000123456",
     "toAccountNumber": "1000654321",
     "amount": 2000000,
     "description": "Test Saga Compensating Rollback Flow",
     "simulateCreditFailure": true
   }
   ```
3. Bấm **Send** -> Nhận phản hồi `422 Unprocessable Entity` với thông báo lỗi rõ ràng và đã kích hoạt Saga Compensating Rollback.
4. Tra cứu lại số dư TK 1000123456: Số dư **VẪN NGUYÊN VẸN** (không bị thất thoát số dư).
5. Mở request `05. Notification Service -> 5.1 List All Notifications`: Xuất hiện thông báo hoàn tiền:
   > *"Cảnh báo GD - Đã hoàn tiền (Compensated): Giao dịch TXN-... chuyển 2.000.000 VND đến 1000654321 không thành công. Hệ thống đã hoàn tiền về tài khoản 1000123456."*
