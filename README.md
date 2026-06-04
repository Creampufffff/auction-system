# Auction System

Link GitHub repository: https://github.com/Creampufffff/auction-system

## 1. Mô tả bài toán và phạm vi hệ thống

Auction System là ứng dụng đấu giá trực tuyến theo mô hình client/server. Hệ thống cho phép người dùng đăng ký tài khoản, đăng nhập, quản lý sản phẩm đấu giá, tham gia đấu giá theo thời gian thực và theo dõi lịch sử giao dịch đấu giá.

Phạm vi hệ thống:

- Bidder có thể xem danh sách phiên đấu giá, xem chi tiết sản phẩm, đặt giá, đặt auto-bid, xem lịch sử bid và quản lý số dư.
- Seller có thể tạo, sửa, xóa, quản lý sản phẩm/phiên đấu giá và upload hình ảnh sản phẩm.
- Admin có console cơ bản gồm dashboard, danh sách user, danh sách auction và bid history.
- Server xử lý logic nghiệp vụ đấu giá, concurrent bidding, cập nhật realtime, auto-close auction và anti-sniping.
- Database lưu user, item, auction, bid transaction, số dư và các thông tin liên quan.

## 2. Công nghệ sử dụng

- Ngôn ngữ: Java
- UI client: JavaFX, FXML, CSS
- Server: Java Socket TCP, multithreading, scheduled task
- Database: MySQL 8, JDBC, HikariCP
- Build tool: Maven
- Test: JUnit 5, Mockito
- DevOps: Docker, Docker Compose, GitHub Actions, GitHub Container Registry, Watchtower

## 3. Môi trường chạy và yêu cầu cài đặt

Chạy local:

- JDK 17 trở lên
- Maven 3.8 trở lên
- MySQL 8
- Windows, Linux hoặc macOS

Chạy server bằng Docker:

- Docker
- Docker Compose v2, dùng lệnh `docker compose`
- VPS Linux, khuyến nghị Ubuntu 22.04/24.04

Kiểm tra môi trường local:

```bash
java -version
mvn -version
mysql --version
```

Kiểm tra Docker trên VPS:

```bash
docker --version
docker compose version
```

## 4. Cấu trúc thư mục

```text
auction-system/
|-- pom.xml                         # Maven parent project
|-- docker-compose.yml              # Chạy server, MySQL và watchtower trên VPS
|-- docker/
|   `-- mysql/
|       `-- Dockerfile              # Custom MySQL image có sẵn schema.sql
|-- auction-common/                 # Module dùng chung
|   |-- pom.xml
|   `-- src/main/java/com/app/common/
|       |-- dto/                    # Request/response DTO
|       |-- entity/                 # User, Auction, Item, Bid...
|       |-- enums/                  # Enum trạng thái
|       |-- exception/              # Exception dùng chung
|       `-- mapper/                 # Mapper entity <-> DTO
|-- auction-server/                 # Module server
|   |-- Dockerfile                  # Docker image cho Java server
|   |-- pom.xml
|   |-- schema.sql                  # Script tạo database
|   `-- src/main/java/com/auction/app/
|       |-- AuctionApplication.java # Entry point server
|       |-- config/                 # Cấu hình database
|       |-- controller/             # Controller xử lý request
|       |-- factory/                # Factory tạo item
|       |-- repository/             # DAO
|       |-- service/                # Business logic
|       `-- socket/                 # Socket server/client handler
`-- auction-client/                 # Module client JavaFX
    |-- pom.xml
    `-- src/main/
        |-- java/com/auction/
        |   |-- MainApp.java        # Entry point client
        |   |-- application/service/
        |   |-- domain/model/
        |   |-- shared/session/
        |   `-- ui/
        `-- resources/
            |-- fxml/
            |-- css/
            `-- images/
```

## 5. Cấu hình môi trường

Tạo file `.env` tại thư mục gốc khi chạy local:

```env
AUCTION_DB_URL=jdbc:mysql://localhost:3306/auction_system
AUCTION_DB_USERNAME=root
AUCTION_DB_PASSWORD=123456
AUCTION_SERVER_HOST=127.0.0.1
AUCTION_SERVER_PORT=5000
```

Nếu client kết nối tới server trên VPS, đổi host:

```env
AUCTION_SERVER_HOST=<IP_VPS>
AUCTION_SERVER_PORT=5000
```

Không commit file `.env` có mật khẩu thật.

## 6. Khởi tạo database khi chạy local

Tạo database và bảng bằng file `auction-server/schema.sql`.

Linux/macOS:

```bash
mysql -u root -p < auction-server/schema.sql
```

Windows PowerShell:

```powershell
Get-Content auction-server\schema.sql | mysql -u root -p
```

## 7. Build và test

Chạy từ thư mục gốc project.

Linux/macOS/Windows PowerShell:

```bash
mvn clean install
mvn test
```

Build riêng server:

```bash
mvn -pl auction-server -am clean package
```

Build riêng client:

```bash
mvn -pl auction-client -am clean package
```

## 8. Hướng dẫn chạy local Server/Client

Thứ tự chạy:

1. Khởi động MySQL.
2. Import `auction-server/schema.sql`.
3. Tạo/cập nhật file `.env`.
4. Chạy server.
5. Mở terminal mới và chạy client.

Chạy server:

```bash
mvn -pl auction-server -am exec:java -Dexec.mainClass=com.auction.app.AuctionApplication
```

Nếu dùng Windows PowerShell và gặp lỗi tham số `-D`, dùng:

```powershell
mvn -pl auction-server -am exec:java "-Dexec.mainClass=com.auction.app.AuctionApplication"
```

Chạy client:

```bash
mvn -pl auction-client -am exec:java -Dexec.mainClass=com.auction.MainApp
```

Windows PowerShell:

```powershell
mvn -pl auction-client -am exec:java "-Dexec.mainClass=com.auction.MainApp"
```

Server mặc định lắng nghe port `5000`. Client đọc `AUCTION_SERVER_HOST` và `AUCTION_SERVER_PORT` từ `.env`.

## 9. Chạy server và database bằng Docker Compose

Cách này dùng cho VPS/deploy. Client JavaFX vẫn chạy trên máy người dùng và kết nối đến IP VPS port `5000`.

File `docker-compose.yml` gồm:

- `mysql`: MySQL 8, dùng image có sẵn schema.
- `server`: Java auction server.
- `watchtower`: tự động cập nhật server khi có image mới trên GHCR.

Tạo file `.env` trên VPS:

```env
MYSQL_ROOT_PASSWORD=change_me_root_password
MYSQL_DATABASE=auction_system
MYSQL_USER=auction_user
MYSQL_PASSWORD=change_me_user_password
AUCTION_SERVER_PORT=5000
```

Chạy lần đầu trên VPS:

```bash
docker compose up -d
```

Xem log server:

```bash
docker compose logs -f server
```

Kiểm tra timezone container:

```bash
docker exec -it auction-server date
```

Mở firewall port server:

```bash
sudo ufw allow 5000/tcp
```

Lưu ý:

- MySQL không expose port `3306` ra ngoài VPS.
- Server kết nối MySQL qua hostname nội bộ Docker: `mysql`.
- Không dùng `docker compose down -v` nếu không muốn xóa volume database.

## 10. Tự động cập nhật server khi push code

Hệ thống dùng GitHub Actions để build và push image lên GitHub Container Registry:

- `ghcr.io/creampufffff/auction-server:latest`
- `ghcr.io/creampufffff/auction-mysql:latest`

Sau khi push code lên `main` hoặc `master`, workflow `Docker Publish` sẽ build image mới. Watchtower trên VPS kiểm tra image mới mỗi 60 giây và tự restart `auction-server`.

Kiểm tra watchtower:

```bash
docker compose logs -f watchtower
```

Log update thành công có dạng:

```text
Found new ghcr.io/creampufffff/auction-server:latest image
Stopping /auction-server
Creating /auction-server
Session done Failed=0 Scanned=1 Updated=1
```

## 11. Các chức năng đã hoàn thành

- Đăng ký tài khoản bidder/seller.
- Đăng nhập và quản lý session client.
- Lưu mật khẩu dạng `password_hash`.
- Quản lý số dư: xem số dư, nạp tiền, rút tiền.
- Tạo và quản lý sản phẩm đấu giá theo loại: art, electronics, vehicle.
- Upload và hiển thị hình ảnh sản phẩm.
- Tạo, sửa, xóa và xem chi tiết auction.
- Tự động chuyển trạng thái auction theo thời gian: `OPEN`, `RUNNING`, `FINISHED`.
- Đặt giá trực tiếp qua socket.
- Xử lý concurrent bidding tại DAO bằng transaction, row lock và cập nhật số dư giữ cho bidder.
- Realtime update khi có bid mới.
- Lịch sử bid của auction và của bidder.
- Auto-bid theo mức giá tối đa.
- Anti-sniping: gia hạn auction khi có bid ở gần thời điểm kết thúc.
- Kết thúc auction và xử lý người thắng.
- Admin console: dashboard, user list, auction list, bid history.
- Docker Compose deploy server + database.
- GitHub Actions build image và Watchtower auto update server trên VPS.
- Unit test cho entity, mapper, factory, controller và service chính.

## 12. Một số tình huống kỹ thuật quan trọng

- Concurrent bidding: nhiều client đặt giá cùng lúc, server dùng transaction và lock để tránh bid sai giá/số dư.
- Realtime update: server broadcast event bid mới cho các client đang theo dõi.
- Auction lifecycle: background scheduler tự động start/end auction theo thời gian.
- Anti-sniping: nếu bid xuất hiện gần lúc kết thúc, server gia hạn thời gian auction.
- Docker deployment: server và MySQL chạy cùng Docker network để giảm latency so với database online bên ngoài.
- Auto update: VPS tự pull image mới và restart server khi GitHub Actions publish image mới.

## 13. Link báo cáo PDF và video demo

- Báo cáo PDF: TODO - cập nhật link Google Drive/GitHub release tại đây.
- Video demo: TODO - cập nhật link video demo tại đây.

## 14. Phát hành client Windows

Client Windows được đóng gói dưới dạng portable ZIP có kèm Java runtime. Người
dùng cuối không cần cài Java:

1. Tải `AuctionClient-windows-x64-<version>.zip` từ GitHub Releases.
2. Giải nén file ZIP.
3. Đổi tên `.env.example` thành `.env`, rồi cập nhật IP/port server.
4. Mở `AuctionClient.exe`.

Để build local:

```powershell
.\windows\package-client.ps1 -Version "1.0.0"
```

File kết quả nằm trong `dist/`. Để GitHub Actions tự tạo Release, push một tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```
