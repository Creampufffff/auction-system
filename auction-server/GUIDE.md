# Auction Server - Hướng Dẫn Kiến Trúc & Cách Dùng

## 📋 Mục Lục
1. [Tổng Quát](#tổng-quát)
2. [Cấu Trúc Thư Mục](#cấu-trúc-thư-mục)
3. [Các Thành Phần Chính](#các-thành-phần-chính)
4. [Flow Xử Lý](#flow-xử-lý)
5. [Cách Khởi Động](#cách-khởi-động)
6. [Socket Protocol](#socket-protocol)
7. [Database Schema](#database-schema)
8. [Lưu Ý Quan Trọng](#lưu-ý-quan-trọng)

---

## 🎯 Tổng Quát

**Auction Server** là backend của hệ thống đấu giá trực tuyến, chịu trách nhiệm:
- Quản lý tài khoản (Bidder & Seller)
- Quản lý phiên đấu giá (tạo, khởi động, kết thúc)
- Xử lý đặt giá (bid) một cách an toàn (transaction-safe)
- Phát sự kiện realtime cho client (EVENT|BID_UPDATED, EVENT|AUCTION_ENDED)
- Tự động kết thúc auction khi hết giờ (auto-close)

**Stack công nghệ:**
- Java 15+
- Maven (build)
- MySQL (database)
- Socket Protocol (client-server)
- Jackson (JSON serialization)
- Lombok (code generation)

---

## 📁 Cấu Trúc Thư Mục

```
auction-server/
├── src/
│   ├── main/java/com/auction/app/
│   │   ├── AuctionApplication.java          # Entry point, đọc config từ .env
│   │   ├── repository/                      # Data Access Layer (DAO)
│   │   │   ├── AuctionDAO.java             # Interface
│   │   │   ├── BidDAO.java
│   │   │   ├── UserDAO.java
│   │   │   ├── ItemDAO.java
│   │   │   └── impl/
│   │   │       ├── AuctionDAOImpl.java      # Query DB, transaction-safe bid
│   │   │       ├── BidDAOImpl.java          # placeBidSafely() logic
│   │   │       ├── UserDAOImpl.java
│   │   │       └── ItemDAOImpl.java
│   │   ├── service/                         # Business Logic Layer
│   │   │   ├── AuctionService.java         # Interface
│   │   │   ├── BidService.java
│   │   │   ├── UserService.java
│   │   │   ├── ItemService.java
│   │   │   └── impl/
│   │   │       ├── AuctionServiceImpl.java
│   │   │       ├── BidServiceImpl.java
│   │   │       ├── UserServiceImpl.java
│   │   │       ├── ItemServiceImpl.java
│   │   │       └── AuctionManager.java      # Auto-close logic
│   │   ├── controller/                      # API Handlers
│   │   │   ├── AuctionController.java
│   │   │   ├── BidController.java
│   │   │   ├── UserController.java
│   │   │   └── ItemController.java
│   │   ├── socket/                          # Socket & Real-time
│   │   │   ├── AuctionSocketServer.java    # ServerSocket, accept clients
│   │   │   ├── ClientHandler.java          # Per-client command processor
│   │   │   └── observer/
│   │   │       ├── Subject.java            # Publisher interface
│   │   │       ├── Observer.java           # Subscriber interface
│   │   │       └── SocketClientObserver.java # Event listener
│   │   └── config/
│   │       └── DatabaseConfig.java         # Connection pool (HikariCP)
│   ├── test/java/com/auction/app/service/impl/
│   │   ├── AuctionServiceImplTest.java
│   │   ├── BidServiceImplTest.java
│   │   └── UserServiceImplTest.java
│   └── resources/
│       └── schema.sql                       # DB init script
├── pom.xml                                   # Maven dependencies
├── GUIDE.md                                  # This file
└── README.md                                 # Quick start

```

---

## 🔧 Các Thành Phần Chính

### 1. **AuctionApplication** (Entry Point)
**Vị trí:** `src/main/java/com/auction/app/AuctionApplication.java`

**Chức năng:**
- Khởi tạo tất cả DAO, Service, Controller
- Đọc port từ `.env` file (AUCTION_SERVER_PORT)
- Khởi động AuctionSocketServer
- Khởi động AuctionManager (auto-close task)
- Đăng ký shutdown hook

**Cách dùng:**
```bash
# Chạy với port mặc định (5000)
mvn exec:java -Dexec.mainClass="com.auction.app.AuctionApplication"

# Cấu hình trong .env
# AUCTION_SERVER_PORT=5000
```

---

### 2. **AuctionSocketServer** (Socket Acceptor)
**Vị trí:** `src/main/java/com/auction/app/socket/AuctionSocketServer.java`

**Chức năng:**
- Mở `ServerSocket` trên cổng được cấu hình
- Chấp nhận client connection trong vòng lặp vô hạn
- Spawn một `ClientHandler` thread cho mỗi client
- Quản lý danh sách Observer để broadcast event

**Cách dùng:**
```java
AuctionSocketServer server = new AuctionSocketServer(
    5000,                    // port
    userService,
    itemService,
    auctionService,
    bidService
);
server.start();              // Block cho đến khi shutdown
server.broadcast("message"); // Gửi đến tất cả client
```

---

### 3. **ClientHandler** (Command Processor)
**Vị trị:** `src/main/java/com/auction/app/socket/ClientHandler.java`

**Chức năng:**
- Đọc command từ client (text hoặc JSON)
- Xử lý từng lệnh (LOGIN, PLACE_BID, CREATE_AUCTION, ...)
- Ghi kết quả về client
- Tự động đăng ký `SocketClientObserver` để nhận broadcast

**Command hỗ trợ:**
- `LOGIN username password` → Xác thực, giữ session
- `REGISTER_BIDDER / REGISTER_SELLER` → Tạo tài khoản
- `DEPOSIT amount` → Nạp tiền (require login)
- `GET_BALANCE` → Lấy số dư ví
- `LIST_AUCTIONS` → Danh sách auction (OPEN + RUNNING)
- `GET_AUCTION auctionId` → Chi tiết auction
- `CREATE_ART_AUCTION [fields]` → Tạo sàn (require seller)
- `START_AUCTION auctionId` → Khởi động (require seller)
- `PLACE_BID auctionId bidderId amount` → Đặt giá (require bidder + RUNNING)
- `END_AUCTION auctionId` → Kết thúc (require seller)
- `HELP` → Danh sách lệnh

**Response format:**
```
OK|COMMAND|[field1]|[field2]...
ERR|ERROR_CODE|[message]
```

---

### 4. **BidDAOImpl.placeBidSafely()** (Transaction-Safe Bidding)
**Vị trí:** `src/main/java/com/auction/app/repository/impl/BidDAOImpl.java`

**Chức năng:**
- Khóa (lock) record auction trong transaction
- Kiểm tra trạng thái auction = RUNNING
- Kiểm tra số dư bidder
- Kiểm tra bid >= hiện giá + min increment
- Insert bid mới
- Update highest_current_price + last_bidder_id
- Commit hoặc rollback toàn bộ

**Lưu ý:**
- Sử dụng `FOR UPDATE` để tránh race condition
- Tất cả bước trong 1 transaction

---

### 5. **AuctionManager** (Auto-Close Task)
**Vị trí:** `src/main/java/com/auction/app/service/impl/AuctionManager.java`

**Chức năng:**
- Chạy periodic task kiểm tra auction hết giờ
- Tự động gọi `endAuction()` nếu time > end_time
- Phát event `EVENT|AUCTION_ENDED|auctionId`
- Là Singleton (chỉ có 1 instance)

**Cách dùng:**
```java
AuctionManager manager = AuctionManager.getInstance();
manager.startAutoClose(auctionService, server);
// ... sau đó
manager.stopAutoClose();  // Khi shutdown
```

---

### 6. **Observer Pattern** (Real-time Events)
**Vị trí:** `src/main/java/com/auction/app/socket/observer/`

**Thành phần:**
- `Subject<T>` - Interface publish
- `Observer<T>` - Interface subscribe
- `SocketClientObserver` - Concrete observer (ghi vào PrintWriter của client)

**Flow:**
```
ClientHandler subscribe (registerObserver)
    ↓
BidDAOImpl.placeBidSafely() success
    ↓
AuctionSocketServer.broadcast("EVENT|BID_UPDATED|...")
    ↓
Tất cả SocketClientObserver.onEvent() được gọi
    ↓
Client nhận EVENT qua socket
```

---

### 7. **DatabaseConfig** (Connection Pool)
**Vị trị:** `src/main/java/com/auction/app/config/DatabaseConfig.java`

**Chức năng:**
- Cấu hình HikariCP connection pool
- Đọc JDBC URL, username, password từ `.env`
- Cung cấp `getConnection()` để DAO dùng

**Biến môi trường:**
```dotenv
AUCTION_DB_URL=jdbc:mysql://localhost:3306/auction_system
AUCTION_DB_USERNAME=root
AUCTION_DB_PASSWORD=password
```

---

## 🔄 Flow Xử Lý

### Flow Đặt Giá (PLACE_BID)

```
Client (auction-client)
    │
    └─ "PLACE_BID auction_id bidder_id amount"
         │
         ↓ (Socket)
    
ClientHandler
    └─ Phân tích lệnh
    └─ Kiểm tra currentUser (phải đã LOGIN)
    └─ Gọi BidController.placeBid()
         │
         ↓
    
BidController
    └─ Validate request
    └─ Gọi BidServiceImpl.placeBid()
         │
         ↓
    
BidServiceImpl
    └─ Kiểm tra bid != null, auctionId, bidderId, amount > 0
    └─ Load Bidder từ DB → kiểm tra balance >= amount
    └─ Gọi BidDAOImpl.placeBidSafely()
         │
         ↓
    
BidDAOImpl.placeBidSafely() [TRANSACTION]
    ├─ Lock auction: SELECT ... FOR UPDATE
    ├─ Kiểm tra status = RUNNING
    ├─ Kiểm tra min_increment
    ├─ INSERT bid
    ├─ UPDATE auction.highest_current_price
    └─ COMMIT
         │
         ↓
    
AuctionSocketServer
    └─ Broadcast "EVENT|BID_UPDATED|auction_id|amount|bidder_id"
         │
         ↓
    
Tất cả SocketClientObserver
    └─ Ghi event vào PrintWriter của từng client
         │
         ↓
    
Tất cả Client
    └─ Nhận EVENT → update UI
```

### Flow Kết Thúc Auction (AUTO-CLOSE)

```
AuctionManager (periodic task)
    │
    └─ Kiểm tra: current_time > end_time?
         │
         ├─ YES → AuctionServiceImpl.endAuction()
         │         │
         │         ├─ settleWinningBid()
         │         │   ├─ Load highest bid
         │         │   ├─ Debit winner
         │         │   └─ Credit seller
         │         │
         │         └─ Update status = FINISHED
         │
         └─ NO → (bỏ qua)
              │
              ↓
    AuctionSocketServer.broadcast("EVENT|AUCTION_ENDED|auction_id")
         │
         ↓
    Tất cả client nhận sự kiện
```

---

## 🚀 Cách Khởi Động

### 1. Cấu hình `.env`
```dotenv
AUCTION_DB_URL=jdbc:mysql://localhost:3306/auction_system
AUCTION_DB_USERNAME=root
AUCTION_DB_PASSWORD=password
AUCTION_SERVER_PORT=5000
```

### 2. Khởi tạo DATABASE
```bash
cd auction-server
mysql -u root -p < src/main/resources/schema.sql
```

### 3. Build & Chạy
```bash
# Từ root project
mvn clean install

# Từ auction-server
mvn exec:java -Dexec.mainClass="com.auction.app.AuctionApplication"

# Hoặc chạy JAR
mvn package
java -jar target/auction-server-1.0-SNAPSHOT.jar
```

### 4. Client kết nối
```
Socket: localhost:5000
Protocol: Plain text (legacy) + JSON envelope

Ví dụ (telnet):
> LOGIN user1 pass123
< OK|LOGIN|user1_id|user1|BIDDER
```

---

## 🔌 Socket Protocol

### Text Commands (Legacy)
```
Format: COMMAND arg1 arg2 arg3

LOGIN username password
REGISTER_BIDDER username password email
DEPOSIT amount
PLACE_BID auctionId bidderId amount
START_AUCTION auctionId
END_AUCTION auctionId
```

### Response Format
```
OK|COMMAND|field1|field2|...
ERR|ErrorCode|ErrorMessage
```

### Real-time Events
```
EVENT|BID_UPDATED|auctionId|currentPrice|leadingBidderId
EVENT|AUCTION_ENDED|auctionId
EVENT|AUCTION_STARTED|auctionId
```

---

## 🗄️ Database Schema

**Các bảng chính:**

```sql
-- Users (Bidder + Seller)
users (
  id, username, password_hash, role (BIDDER/SELLER), 
  balance, email, created_at
)

-- Items/Products
items (
  id, name, description, author, start_price, min_increment,
  highest_current_price, seller_id, created_at
)

-- Auctions (Phiên đấu giá)
auctions (
  id, item_id, status (OPEN/RUNNING/FINISHED/CANCELED),
  start_time, end_time, last_bidder_id, current_version,
  created_at, updated_at
)

-- Bids (Lịch sử đặt giá)
bid_transactions (
  id, auction_id, bidder_id, bid_amount, created_at
)
```

---

## ⚠️ Lưu Ý Quan Trọng

### 1. Transaction Safety
- `BidDAOImpl.placeBidSafely()` sử dụng **FOR UPDATE lock** để tránh race condition
- Settlement (`endAuction`) chưa 100% atomic → cần cải thiện

### 2. Fund Reservation
- Hiện tại chỉ check balance khi bid, **không hold money**
- Bidder có thể thắng nhiều auction nhưng tài khoản thiếu khi settle
- **Cần implement escrow system**

### 3. Socket Writer Sync
- `SocketClientObserver` ghi thẳng vào PrintWriter
- Nếu multiple threads ghi đồng thời → có thể trộn output
- **Nên serialize writes per socket**

### 4. Thread-per-Connection
- Server dùng `new Thread()` cho mỗi client
- Đơn giản nhưng không bền khi tải cao
- **Nên dùng ExecutorService cho production**

### 5. Auto-Close Timing
- `AuctionManager` check mỗi 1 giây (configurable)
- Khoảng delay giữa end_time và auto-close có thể lên tới 1s
- **OK cho use case này**

---

## 📊 Dependency Diagram

```
AuctionApplication (entry)
    ├─ AuctionSocketServer
    │   ├─ ClientHandler (per client)
    │   │   ├─ AuctionController
    │   │   ├─ BidController
    │   │   ├─ UserController
    │   │   └─ ItemController
    │   │
    │   ├─ SocketClientObserver (per client, listener)
    │   └─ Observer pattern (publish-subscribe)
    │
    ├─ AuctionManager (singleton)
    │   └─ AuctionService → Database
    │
    └─ Services
        ├─ AuctionServiceImpl
        ├─ BidServiceImpl
        ├─ UserServiceImpl
        └─ ItemServiceImpl
            └─ DAOImpl (repository)
                └─ DatabaseConfig → MySQL
```

---

## 🧪 Testing

Run tests:
```bash
mvn test

# Hoặc chỉ service tests:
mvn test -Dtest=*ServiceImplTest
```

Hiện có:
- `AuctionServiceImplTest.java`
- `BidServiceImplTest.java`
- `UserServiceImplTest.java`

Khuyến nghị thêm:
- Socket integration test
- End-to-end flow test
- Concurrent bid test

---

## 📚 Các Enum & Dto

### Status Enum
```java
enum Status {
    OPEN,      // Tạo mới, chưa bắt đầu
    RUNNING,   // Đang diễn ra
    FINISHED,  // Kết thúc, đã settle
    CANCELED   // Hủy
}
```

### Key DTOs
```java
LoginResponseDTO
RegisterResponseDTO
AuctionListDTO
PlaceBidRequestDTO / PlaceBidResponseDTO
BidHistoryDTO
BalanceResponseDTO
```

---

Chúc bạn phát triển thành công! 🎉

