# 🌾 FarmContest (v2.0.0)

<div align="center">

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.4--1.21.x-brightgreen?style=for-the-badge&logo=minecraft)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot%20%7C%20Purpur-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Dependencies](https://img.shields.io/badge/Dependencies-Vault%20%7C%20PlaceholderAPI-yellow?style=for-the-badge)

**Giải pháp tổ chức cuộc thi nông sản & hệ thống nông sản đột biến chuẩn phong cách *Hypixel Jacob's Contest* hàng đầu cho Server Minecraft của bạn!**

[Tính Năng](#-tính-năng-nổi-bật) • [Cài Đặt](#-hướng-dẫn-cài-đặt) • [Lệnh & Phân Quyền](#-lệnh--phân-quyền) • [Placeholders](#-tích-hợp-placeholderapi) • [Cấu Hình](#-cấu-hình)

---

</div>

## 🌟 Tính Năng Nổi Bật

### 🏆 1. Hệ Thống Cuộc Thi Nông Sản Tự Động (Farm Contest)
* **Xoay tua tự động:** Tự động bắt đầu/kết thúc cuộc thi và chọn ngẫu nhiên các loại nông sản (Mía, Xương rồng, Bí ngô, Dưa hấu, Lúa mì...) theo lịch trình.
* **Bảng xếp hạng thời gian thực:** Tính điểm, cập nhật thứ hạng và trả thưởng chính xác tuyệt đối cho từng người chơi.
* **Giao diện BossBar trực quan:** Hiển thị thời gian đếm ngược và trạng thái điểm số ngay trên màn hình.
* **Chống gian lận (Anti-Cheat) thông minh:** Tự động ghi nhớ các khối do người chơi tự đặt để ngăn chặn việc đặt xuống - phá ra lấy điểm vô hạn.

---

### 🧬 2. Nông Sản Đột Biến (Mutation System) & Shop GUI
* **Đột biến ngẫu nhiên:** Thu hoạch nông sản có tỷ lệ nhận được nông sản đột biến với các thuộc tính siêu hiếm và hệ số nhân điểm (Multiplier) cực khủng.
* **Phụ thuộc Biome:** Một số loại đột biến chỉ xuất hiện ở các vùng sinh thái (Biome) nhất định, khuyến khích người chơi khám phá thế giới.
* **Cửa hàng Đột Biến (Mutation Shop):** Giao diện GUI đẹp mắt cho phép người chơi mua bán, trao đổi và nâng cấp các loại hạt giống/nông sản đột biến.

---

### 💾 3. Lưu Trữ Dữ Liệu & Tích Hợp Linh Hoạt
* **Đa nền tảng Database:** Hỗ trợ cả **SQLite** (mặc định, không cần cài đặt thêm) và **MySQL** (cho cụm server lớn/Network).
* **Đồng bộ mượt mà:** Tự động tải/lưu dữ liệu người chơi khi Tham gia (Join) hoặc Thoát (Quit) server mà không gây lag.
* **Tích hợp Vault:** Hỗ trợ thưởng tiền trực tiếp vào tài khoản ngân hàng của người chơi.
* **Tích hợp PlaceholderAPI:** Dễ dàng đưa điểm số, thứ hạng lên Scoreboard, Tablist, Hologram hoặc Chat format.

---

## 🛠 Hướng Dẫn Cài Đặt

1. Tải file `FarmContest-2.0.0.jar` mới nhất ở mục **Releases**.
2. Đặt file `.jar` vào thư mục `/plugins/` của máy chủ.
3. *(Khuyên dùng)* Cài đặt thêm các plugin hỗ trợ:
   * **[Vault](https://www.spigotmc.org/resources/vault.3431/)** (Để thưởng tiền).
   * **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)** (Để hiển thị thông số lên Scoreboard/Tab).
4. Khởi động lại Server để plugin tự tạo file cấu hình.

---

## 📜 Lệnh & Phân Quyền

Lệnh chính: `/farmcontest` hoặc lệnh tắt `/fc`

| Lệnh | Mô tả | Phân quyền (Permission) |
| :--- | :--- | :--- |
| `/fc help` | Xem danh sách hướng dẫn lệnh | `farmcontest.use` |
| `/fc status` | Xem trạng thái cuộc thi đang diễn ra & điểm cá nhân | `farmcontest.use` |
| `/fc shop` | Mở Cửa Hàng Nông Sản Đột Biến | `farmcontest.use` |
| `/fc top` | Xem Bảng Xếp Hạng cuộc thi hiện tại | `farmcontest.use` |
| `/fc start <loại_cây>` | *(Admin)* Cưỡng chế bắt đầu cuộc thi | `farmcontest.admin` |
| `/fc stop` | *(Admin)* Dừng cuộc thi ngay lập tức | `farmcontest.admin` |
| `/fc reload` | *(Admin)* Tải lại toàn bộ cấu hình plugin | `farmcontest.admin` |

---

## 📊 Tích Hợp PlaceholderAPI

Sử dụng các placeholder sau trong Scoreboard (FeatherBoard, TAB...), Chat hoặc Hologram:

* `%farmcontest_status%` — Trạng thái cuộc thi (*Đang diễn ra / Đã kết thúc*).
* `%farmcontest_time_remaining%` — Thời gian còn lại của cuộc thi.
* `%farmcontest_crop%` — Loại nông sản đang thi đấu trong vòng này.
* `%farmcontest_score%` — Điểm số hiện tại của người chơi.
* `%farmcontest_position%` — Thứ hạng hiện tại của người chơi trong BXH.

---

## 🖥 Khả Năng Tương Thích

* **Phiên bản Minecraft hỗ trợ:** `1.20.4` $\rightarrow$ `1.21.x` (và các bản nâng cấp tương lai).
* **Software:** Paper, Spigot, Purpur, Leaf, Folia...
* **Lưu ý:** Plugin được viết hoàn toàn bằng API chuẩn, không sử dụng NMS/Reflection nên cực kỳ ổn định và không lo bị lỗi khi Server cập nhật bản Minecraft mới!
