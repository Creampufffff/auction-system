# Auction System (Hệ thống đấu giá online)

## 1. Giới thiệu bài toán và phạm vi của hệ thống

Auction System là ứng dụng đấu giá trực tuyến theo mô hình Client/Server. Hệ thống cho phép người dùng đăng ký, đăng nhập, quản lý tài khoản, tạo sản phẩm đấu giá, bắt đầu phiên đấu giá, đặt giá theo thời gian thực và theo dõi lịch sử đấu giá và bao gồm cả một số chức năng nâng cao khác.

**Phạm vi hiện tại của hệ thống gồm:**

- Đăng nhập, đăng ký tài khoản người dùng.
- Quản lí thông tin tài khoản người dùng.
- Tạo sản phẩm đấu giá, quản lí các phiên đấu giá.
- Người dùng đặt giá trực tiếp theo thời gian thực.
- Cập nhật giá đấu theo thời gian thực, tự động khi có một lượt đặt giá mới.
- Tự động đóng và xử lí đối với người chiến thắng khi phiên đấu giá kết thúc.
- Quản lí kho hàng các sản phẩm đấu giá của cá nhân đối với seller, và lịch sử đấu giá đối với bidder.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt

### Công nghệ sử dụng

- **Backend:** Java, Socket TCP, đa luồng với Thread Pool, Concurrency và ScheduledExecutorService.
- **Frontend:** JavaFX, FXML, CSS tuỳ chỉnh giao diện.
- **Database:** MySQL, JDBC, HikariCP.
- **DevOps/Tools:** Maven, Git/GitHub.
- **Testing:** JUnit 5, Mockito.

### Môi trường chạy và yêu cầu cài đặt

- **JDK:** JDK 15 trở lên, khuyến nghị JDK 17 hoặc JDK 21.
- **Maven:** Maven 3.8 trở lên.
- **Database:** MySQL Server 8.x hoặc MySQL-compatible database.
- **Hệ điều hành:** Windows, Linux hoặc macOS.

### Kiểm tra môi trường

```bash
java -version
mvn -version
mysql --version
```

## 3. Cấu trúc thư mục

```text
auction-system/
|-- pom.xml                         # Maven parent project
|-- .env                            # Cấu hình local, không commit thông tin thật
|-- auction-common/                 # Module dùng chung
|   |-- pom.xml
|   `-- src/
|       |-- main/java/com/app/common/
|       |   |-- dto/                # Request/response DTO
|       |   |-- entity/             # User, Auction, Item, Bid...
|       |   |-- mapper/             # Mapper entity <-> DTO
|       |   |-- enums/              # Trạng thái đấu giá
|       |   `-- exception/          # Exception dùng chung
|       `-- test/java/              # Unit test module common
|-- auction-server/                 # Module server
|   |-- pom.xml
|   |-- schema.sql                  # Script tạo database
|   `-- src/
|       |-- main/java/com/auction/app/
|       |   |-- AuctionApplication.java
|       |   |-- config/             # DatabaseConfig
|       |   |-- controller/         # Controller xử lý request
|       |   |-- factory/            # Factory tạo item theo loại
|       |   |-- repository/         # DAO và DAO implementation
|       |   |-- service/            # Business logic
|       |   `-- socket/             # Socket server, handler, observer
|       `-- test/java/              # Unit test server
`-- auction-client/                 # Module client JavaFX
    |-- pom.xml
    `-- src/
        |-- main/java/com/auction/
        |   |-- Launcher.java
        |   |-- MainApp.java
        |   |-- application/service/ # Service gọi server/socket
        |   |-- domain/model/        # Model hiển thị trên UI
        |   |-- shared/session/      # Quản lý session đăng nhập
        |   `-- ui/                  # Controller và navigation JavaFX
        `-- main/resources/
            |-- fxml/                # Màn hình JavaFX
            |-- css/                 # Style
            `-- images/              # Ảnh/icon ứng dụng
```

## 4. Cấu hình môi trường

Tạo file `.env` tại thư mục gốc project. Không đưa mật khẩu thật vào README hoặc commit lên Git.

```dotenv
AUCTION_DB_URL=jdbc:mysql://localhost:3306/auction_system
AUCTION_DB_USERNAME=root
AUCTION_DB_PASSWORD=your_password
AUCTION_SERVER_HOST=127.0.0.1
AUCTION_SERVER_PORT=5000
```

Ghi chú:

- Server đọc `AUCTION_SERVER_PORT`, mặc định là `5000`.
- Client đọc `AUCTION_SERVER_HOST` và `AUCTION_SERVER_PORT`, mặc định là `127.0.0.1:5000`.
- Database có thể cấu hình trực tiếp bằng `AUCTION_DB_URL` hoặc tách thành `AUCTION_DB_HOST`, `AUCTION_DB_PORT`, `AUCTION_DB_NAME`.

## 5. Khởi tạo database

Tạo database và bảng bằng file `auction-server/schema.sql`.

### Windows PowerShell

```powershell
mysql -u root -p < auction-server\schema.sql
```

Nếu PowerShell không nhận redirect với lệnh trên, chạy:

```powershell
Get-Content auction-server\schema.sql | mysql -u root -p
```

### Linux/macOS

```bash
mysql -u root -p < auction-server/schema.sql
```

## 6. Build và test

Chạy từ thư mục gốc project.

### Windows PowerShell

```powershell
mvn clean install
mvn test
```

### Linux/macOS

```bash
mvn clean install
mvn test
```

Build riêng từng module:

```bash
mvn -pl auction-common clean install
mvn -pl auction-server -am clean package
mvn -pl auction-client -am clean package
```

## 7. Hướng dẫn chạy Server/Client

Thứ tự chạy bắt buộc:

1. Khởi động MySQL và tạo schema bằng `auction-server/schema.sql`.
2. Cấu hình file `.env`.
3. Build project bằng `mvn clean install`.
4. Chạy server.
5. Chạy client.

### 8.1. Chạy server

Chạy từ thư mục gốc project:

```bash
mvn -pl auction-server -am exec:java -Dexec.mainClass="com.auction.app.AuctionApplication"
```

Nếu dùng PowerShell và gặp lỗi tham số `-D`, có thể đặt trong dấu nháy:

```powershell
mvn -pl auction-server -am exec:java "-Dexec.mainClass=com.auction.app.AuctionApplication"
```

Server sẽ lắng nghe tại port trong `.env`, ví dụ:

```text
AUCTION_SERVER_PORT=5000
```

### 8.2. Chạy client JavaFX

Mở terminal mới, giữ server đang chạy, sau đó chạy:

```bash
mvn -pl auction-client -am exec:java -Dexec.mainClass="com.auction.MainApp"
```

PowerShell:

```powershell
mvn -pl auction-client -am exec:java "-Dexec.mainClass=com.auction.MainApp"
```

Client sẽ kết nối tới server theo:

```dotenv
AUCTION_SERVER_HOST=127.0.0.1
AUCTION_SERVER_PORT=5000
```

### 8.3. Test socket bằng terminal

Có thể kiểm tra server bằng telnet/netcat nếu đã cài công cụ.

Linux/macOS:

```bash
nc 127.0.0.1 5000
```

Windows có thể dùng `telnet` nếu đã bật Telnet Client:

```powershell
telnet 127.0.0.1 5000
```

Ví dụ lệnh:

```text
PING
LOGIN username password
LIST_AUCTIONS
GET_BALANCE
```

## 8. Các chức năng đã hoàn thành

- Đăng ký tài khoản bidder/seller.
- Đăng nhập và lưu session người dùng trên client.
- Nạp/rút tiền và xem số dư tài khoản.
- Tạo sản phẩm đấu giá theo nhiều loại: art, electronics, vehicle.
- Upload/hiển thị hình ảnh sản phẩm.
- Tạo và quản lý phiên đấu giá.
- Hiển thị danh sách phiên đấu giá.
- Xem chi tiết sản phẩm/phiên đấu giá.
- Bắt đầu và kết thúc phiên đấu giá.
- Đặt giá trực tiếp qua socket.
- Cập nhật giá realtime khi có bid mới.
- Xem lịch sử bid.
- Tự động gia hạn/kết thúc phiên đấu giá theo logic server.
- Auto bidding.
- Tách module common/server/client để tái sử dụng DTO, entity và mapper.
- Unit test cho mapper, entity, service, controller và factory chính.

## 9. Giao thức giao tiếp

Server hỗ trợ command dạng text và một số request dạng JSON. Format phản hồi text:

```text
OK|COMMAND|field1|field2|...
ERR|ERROR_CODE|message
EVENT|BID_UPDATED|auctionId|currentPrice|leadingBidderId
EVENT|AUCTION_ENDED|auctionId
EVENT|AUCTION_EXTENDED|auctionId|newEndTime
```

Một số command tiêu biểu:

```text
PING
LOGIN <username> <password>
REGISTER_BIDDER <username> <password> <email>
REGISTER_SELLER <username> <password> <email>
GET_BALANCE
DEPOSIT <amount>
WITHDRAW <amount>
LIST_AUCTIONS
GET_AUCTION <auctionId>
PLACE_BID <auctionId> <bidderId> <amount>
START_AUCTION <auctionId>
END_AUCTION <auctionId>
```

## 10. Link báo cáo và video demo

- Báo cáo PDF: [Cập nhật link báo cáo tại đây](https://example.com/report.pdf)
- Video demo: [Cập nhật link video demo tại đây](https://example.com/demo-video)

## 11. Ghi chú khi nộp bài

- Thay các link placeholder trong mục 11 bằng link thật.
- Kiểm tra lại `.env` trước khi nộp, không để lộ thông tin database production.
- Nếu project cần chạy trên máy khác, nên dùng database local và cập nhật `AUCTION_DB_URL`, `AUCTION_DB_USERNAME`, `AUCTION_DB_PASSWORD`.
- Chạy lại `mvn clean install` và `mvn test` trước khi nộp để đảm bảo build thành công.
