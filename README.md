# 🌾 FarmContest (v2.0.0)

<div align="center">

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.4--1.21.x-brightgreen?style=for-the-badge&logo=minecraft)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot%20%7C%20Purpur-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Dependencies](https://img.shields.io/badge/Dependencies-Vault%20%7C%20PlaceholderAPI-yellow?style=for-the-badge)

</div>

---

<details open>
  <summary><b>🇻🇳 Tiếng Việt</b> (Click to expand / Bấm để mở)</summary>
  <br>

**Giải pháp tổ chức cuộc thi nông sản & hệ thống nông sản đột biến chuẩn phong cách *Hypixel Jacob's Contest* hàng đầu cho Server Minecraft của bạn!**

[Tính Năng](#-1-hệ-thống-cuộc-thi-nông-sản-tự-động-farm-contest) • [Cài Đặt](#-hướng-dẫn-cài-đặt) • [Lệnh & Phân Quyền](#-lệnh--phân-quyền) • [Placeholders](#-tích-hợp-placeholderapi) • [Tương Thích](#-khả-năng-tương-thích)

---

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
* **Đa nền tảng Database:** Hỗ trợ **SQLite** (mặc định, không cần cài đặt thêm).
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

* **Phiên bản Minecraft hỗ trợ:** `1.20.4` -> `1.21.x` (và các bản nâng cấp tương lai).
* **Software:** Paper, Spigot, Purpur
* **Lưu ý:** Cần có Vault và Economy plugin bất kỳ.

</details>

<details>
  <summary><b>🇺🇸 English</b> (Click to expand / Bấm để mở)</summary>
  <br>

**The ultimate *Hypixel Jacob's Contest* style farming contest & mutated crop system solution for your Minecraft Server!**

[Features](#-key-features) • [Installation](#-installation-guide) • [Commands & Permissions](#-commands--permissions) • [Placeholders](#-placeholderapi-integration) • [Compatibility](#-compatibility)

---

## 🌟 Key Features

### 🏆 1. Automated Farming Contest System (Farm Contest)
* **Auto-rotation:** Automatically starts/ends contests and randomly selects crops (Sugar Cane, Cactus, Pumpkin, Melon, Wheat...) based on a schedule.
* **Real-time Leaderboard:** Calculates scores, updates rankings, and dispenses rewards with absolute precision.
* **Intuitive BossBar UI:** Displays countdown timer and current score directly on screen.
* **Smart Anti-Cheat:** Remembers player-placed blocks to prevent endless place-and-break exploits.

---

### 🧬 2. Mutation System & Shop GUI
* **Random Mutations:** Harvesting crops has a chance to yield mutated crops with ultra-rare attributes and massive score multipliers.
* **Biome Dependent:** Certain crop mutations only spawn in specific biomes, encouraging world exploration.
* **Mutation Shop:** Clean GUI allowing players to trade, buy, sell, and upgrade mutated seeds and crops.

---

### 💾 3. Data Storage & Flexible Integration
* **Cross-platform Database:** Built-in **SQLite** support (default, zero extra setup needed).
* **Smooth Synchronization:** Automatically loads/saves player data on Join/Quit without causing server lag.
* **Vault Integration:** Directly rewards money to players' economy accounts.
* **PlaceholderAPI Integration:** Easily display scores and rankings on Scoreboard, Tablist, Hologram, or Chat format.

---

## 🛠 Installation Guide

1. Download the latest `FarmContest-2.0.0.jar` from **Releases**.
2. Drop the `.jar` file into your server's `/plugins/` folder.
3. *(Recommended)* Install optional dependencies:
   * **[Vault](https://www.spigotmc.org/resources/vault.3431/)** (For monetary rewards).
   * **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)** (For displaying stats on Scoreboard/Tab).
4. Restart your server to generate configuration files.

---

## 📜 Commands & Permissions

Main command: `/farmcontest` or alias `/fc`

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/fc help` | View command help list | `farmcontest.use` |
| `/fc status` | View active contest status & personal score | `farmcontest.use` |
| `/fc shop` | Open the Mutated Crop Shop GUI | `farmcontest.use` |
| `/fc top` | View current contest leaderboard | `farmcontest.use` |
| `/fc start <crop>` | *(Admin)* Force-start a contest | `farmcontest.admin` |
| `/fc stop` | *(Admin)* Immediately stop the current contest | `farmcontest.admin` |
| `/fc reload` | *(Admin)* Reload all plugin configurations | `farmcontest.admin` |

---

## 📊 PlaceholderAPI Integration

Use these placeholders in your Scoreboard (FeatherBoard, TAB...), Chat, or Hologram:

* `%farmcontest_status%` — Contest status (*Ongoing / Ended*).
* `%farmcontest_time_remaining%` — Time remaining in the current contest.
* `%farmcontest_crop%` — Active crop type for the current round.
* `%farmcontest_score%` — Player's current score.
* `%farmcontest_position%` — Player's current position on the leaderboard.

---

## 🖥 Compatibility

* **Supported Minecraft Versions:** `1.20.4` -> `1.21.x` (and future releases).
* **Software:** Paper, Spigot, Purpur
* **Note:** Requires Vault and any compatible Economy plugin.

</details>
