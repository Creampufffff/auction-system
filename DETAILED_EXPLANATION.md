# 📚 GIẢI THÍCH CHI TIẾT CẤU TRÚC AUCTION-SERVER

## 🏗️ KIẾN TRÚC TỔNG QUÁT

```
             CLIENT (JavaFX)
                   ↕  (Socket)
            ┌──────────────────┐
            │  Socket Server   │ (Lắng nghe port 5000)
            └──────────────────┘
                   ↓
         ┌─────────────────────────┐
         │   ClientHandler         │ (Xử lý mỗi client riêng)
         │  (Parse request)        │
         └─────────────────────────┘
                   ↓
 ┌────────────────────────────────────────┐
 │      ====== CONTROLLER LAYER ======    │
 │ • UserController       (Quản lý người) │
 │ • ItemController       (Quản lý vật)   │
 │ • AuctionController    (Vòng đời)      │
 │ • BidController        (Đấu giá)       │
 │ • AutoBidController    (Tự động)       │
 └────────────────────────────────────────┘
                   ↓
 ┌────────────────────────────────────────┐
 │     ====== SERVICE LAYER ====== │
 │ • UserServiceImpl         (Logic user) │
 │ • ItemServiceImpl         (Logic item) │
 │ • AuctionServiceImpl      (Vòng đời)   │
 │ • BidServiceImpl          (Validate)   │
 │ • AutoBidServiceImpl      (Auto-bid)   │
 │ • AuctionManager         (Background) │
 │ • AuctionExtensionMgr    (Anti-snip)  │
 └────────────────────────────────────────┘
                   ↓
 ┌────────────────────────────────────────┐
 │    ====== REPOSITORY/DAO LAYER ===== │
 │ • UserDAO              (CRUD User)     │
 │ • ItemDAO              (CRUD Item)     │
 │ • AuctionDAO           (CRUD Auction)  │
 │ • BidDAO               (CRUD Bid)      │
 │   ├─ placeBidSafely()  (Transaction)   │
 │   └─ FOR UPDATE Lock   (Concurrency)   │
 └────────────────────────────────────────┘
                   ↓
             DATABASE (MySQL)
```

---

## 📋 LUỒNG HOẠT ĐỘNG CHÍNH

### **A. Luồng Đặt Giá Thầu (Bid Flow)**

```
1️⃣ CLIENT GỬI YÊU CẦU
   Client → Socket: "PLACE_BID auctionId|bidderId|amount"

2️⃣ SOCKET SERVER NHẬN & PARSE
   ClientHandler.run()
   → Parse chuỗi → Extract auctionId, bidderId, amount
   → Gọi BidController.placeBid()

3️⃣ CONTROLLER LAYER
   BidController.placeBid(bid)
   → Tạo BidTransaction object
   → Gọi BidService.placeBid()

4️⃣ SERVICE LAYER - VALIDATE
   BidServiceImpl.placeBid()
   → Kiểm tra bid != null
   → Kiểm tra auctionId != null
   → Kiểm tra bidderId != null
   → Kiểm tra bidAmount > 0
   → Lấy User từ DB → Kiểm tra có đủ tiền
   → Gọi BidDAO.placeBidSafely()

5️⃣ DAO LAYER - TRANSACTION + LOCKING
   BidDAOImpl.placeBidSafely()
   
   Bắt đầu transaction:
   ├─ connection.setAutoCommit(false)
   ├─ Khóa bảng (FOR UPDATE) trên auctions
   ├─ Validate giá thầu > hiện tại + min_increment
   ├─ Validate người dùng là BIDDER
   ├─ INSERT bid_transactions
   ├─ UPDATE items (highest_current_price)
   ├─ Nếu OK: connection.commit()
   └─ Nếu ERROR: connection.rollback()

6️⃣ DATABASE
   bid_transactions table nhận BID mới
   auctions table cập nhật highest_current_price
   
7️⃣ TRẢ VỀ CLIENT
   Socket → Client: "OK|BID_PLACED|bidId"
   
8️⃣ NOTIFY CLIENTS
   AuctionSocketServer.notifyObservers()
   → Gửi thông báo tới TẤT CẢ clients đang online
   "EVENT|BID_UPDATE|auctionId|newPrice"
```

---

## 🔄 KIẾN TRÚC MVC TRONG PROJECT

### **Model (Mô hình dữ liệu)**
```java
// auction-common/src/main/java/com/app/common/entity/

User (abstract)
├── Admin(User)
├── Seller(User)
└── Bidder(User)

Item (abstract)
├── Art(Item)           // Có trường: author
├── Electronics(Item)   // Có trường: warrantyMonths
└── Vehicle(Item)       // Có trường: brand

Auction
├── id
├── item (Item)
├── bidHistory (List<BidTransaction>)
├── auctionStatus (OPEN/RUNNING/FINISHED)
└── lastBidderId

BidTransaction
├── id
├── auction (Auction)
├── bidder (Bidder)
└── bidAmount

AutoBid
├── id
├── auctionId
├── bidderId
├── maxAutoAmount
└── isActive
```

### **View (Giao diện)**
```java
// auction-client/src/main/java/com/auction/client/

MainApp.java
├── JavaFX Application
├── Socket Client Connection
└── UI Controllers (FXML)
```

### **Controller (Điều khiển)**
```java
// auction-server/src/main/java/com/auction/app/controller/

UserController
├── register(user)
├── login(username, password)
├── getBalance(userId)
├── deposit(userId, amount)
└── withdraw(userId, amount)

AuctionController
├── createAuction(auction)
├── startAuction(auctionId)
├── endAuction(auctionId)
└── getActiveAuctions()

BidController
├── placeBid(bid)
├── getAuctionBidHistory(auctionId)
└── deleteBid(bidId)

AutoBidController
├── setAutoBid(autoBid)
├── processAutoBids(auctionId)
└── cancelAutoBid(autoBidId)
```

---

## ⚙️ CỤC BỘ CHI TIẾT - KEY COMPONENTS

### **1. BidDAOImpl.placeBidSafely()**
```java
// ⭐⭐⭐ CÓ RẤT QUAN TRỌNG - Xử lý race condition

Vấn đề: Khi 2 người cùng bid lúc một lúc
├─ Bid A: "giá 500 vào lúc 10:00:00.001"
├─ Bid B: "giá 600 vào lúc 10:00:00.002"
└─ Ai sẽ win?

Giải pháp: FOR UPDATE Lock
├─ Khi Bid A đang xử lý
├─ Bid B phải chờ Bid A hoàn tất
├─ Đảm bảo tính toàn vẹn dữ liệu

Tại sao FOR UPDATE:
├─ Khóa row trong database
├─ Ngăn chặn 2 transaction cùng modify row
└─ Tự động unlock khi commit/rollback
```

### **2. AuctionServiceImpl.settleWinningBid()**
```java
// Quá trình thanh toán đấu giá

Bước 1: Tìm người thắng
└─ BidTransaction maxBid = bidDAO.getMaxBidByAuctionId(auctionId)

Bước 2: Lấy info người thắng & người bán
├─ User winner   = userDAO.findById(maxBid.getBidder.getId())
└─ User seller   = userDAO.findById(auction.getItem.getSellerId())

Bước 3: Chuyển tiền
├─ winner.withdraw(bidAmount)    // Trừ tiền người thắng
└─ seller.deposit(bidAmount)     // Cộng tiền người bán

⚠️ QUAN TRỌNG: Lưu CÙNG LÚC cả 2 người
├─ Nếu chỉ lưu 1 người → hệ thống bất nhất
└─ userDAO.save(winner) + userDAO.save(seller)
```

### **3. AutoBidServiceImpl.processAutoBidsForAuction()**
```java
// Xử lý AutoBid tự động

Ví dụ:
├─ Phiên A có 3 AutoBids: 
│  ├─ Person 1: maxAutoAmount = 100 (ưu tiên 3)
│  ├─ Person 2: maxAutoAmount = 500 (ưu tiên 2)
│  └─ Person 3: maxAutoAmount = 1000 (ưu tiên 1) ⭐ CAO NHẤT
│
├─ Bid hiện tại: 800
├─ Min increment: 50
├─ Giá tiếp theo: 850
│
└─ Xử lý:
   1️⃣ Kiểm tra Person 3 (1000) >= 850? ✓ YES → bid lên 850
   2️⃣ Cập nhật currentPrice = 850
   3️⃣ Kiểm tra Person 2 (500) >= 900? ✗ NO → hủy
   4️⃣ Kiểm tra Person 1 (100) >= 900? ✗ NO → hủy
```

### **4. AuctionExtensionManager.checkAndExtend()**
```java
// Ngăn chặn sniping - Gia hạn phiên

Tình huống:
├─ Phiên kết thúc lúc 15:00:00
├─ Hiện tại 14:56:00 (4 phút trước hết)
├─ Có bid vừa đặt lúc 14:56:30
│
└─ CÓ TRONG 5 PHÚT CUỐI? ✓ YES
   ├─ Gia hạn thêm 5 phút
   ├─ Kết thúc mới: 15:05:00
   ├─ Lần gia hạn: 1/3
   └─ Người dùng khác lại có cơ hội bid
```

---

## 📊 FLOW CHO CÁC TRƯỜNG HỢP CHÍNH

### **Trường Hợp 1: User Đăng Nhập**
```
1. ClientHandler nhận: "LOGIN alice password123"
2. UserController.login("alice", "password123")
3. UserServiceImpl.login()
   ├─ validateInput()
   ├─ userDAO.findByUsername("alice")
   ├─ check password
   └─ return user object
4. Phản hồi client: "OK|LOGIN|userId|role|balance"
```

### **Trường Hợp 2: Bắt Đầu Phiên**
```
1. ClientHandler nhận: "START_AUCTION auctionId123"
2. AuctionController.startAuction("auctionId123")
3. AuctionServiceImpl.startAuction()
   ├─ getAuctionById (xem trạng thái)
   ├─ if status != OPEN → throw error
   ├─ setStatus = RUNNING
   └─ auctionDAO.save()
4. Phản hồi: "OK|AUCTION_STARTED|auctionId"
5. Notify: "EVENT|AUCTION_STATUS_CHANGE|auctionId|RUNNING"
```

### **Trường Hợp 3: Kết Thúc Phiên**
```
1. AuctionManager.closeExpiredAuctions() (chạy background)
   ├─ Duyệt tất cả RUNNING auctions
   ├─ Check nếu currentTime > endTime
   └─ Nếu yes → gọi endAuction()

2. AuctionServiceImpl.endAuction()
   ├─ setStatus = FINISHED
   ├─ settleWinningBid()
   │  ├─ winner.withdraw()
   │  ├─ seller.deposit()
   │  └─ save cả 2
   └─ auctionDAO.save()

3. Notify all clients:
   "EVENT|AUCTION_FINISHED|auctionId|winnerId|finalPrice"
```

---

## 🔐 SECURITY & CONCURRENCY FEATURES

### **1. Validation**
```java
// Tất cả input đều kiểm tra trước khi xử lý
UserServiceImpl.register()
├─ isEmpty checks
├─ username duplicate check
├─ email format check (optional)
└─ balance >= 0 check
```

### **2. Transaction Management**
```java
// Atomicity - All or Nothing
BidDAOImpl.placeBidSafely()
├─ connection.setAutoCommit(false)
├─ ... operations ...
├─ if (success) connection.commit()
└─ if (error) connection.rollback()
```

### **3. Locking**
```java
// FOR UPDATE locks
SELECT * FROM auctions WHERE id = ?
FOR UPDATE  // ← Khóa row này
```

### **4. Exception Handling**
```java
// Custom exceptions với thông điệp rõ ràng
throw new InsufficientBalanceException("Tài khoản không đủ tiền")
throw new InvalidBidException("Bid amount phải > 0")
throw new AuctionClosedException("Phiên đã kết thúc")
```

---

## 📝 FILE STRUCTURE

```
auction-server/
├── controller/              (Điều khiển)
│   ├── UserController.java
│   ├── ItemController.java
│   ├── AuctionController.java
│   ├── BidController.java
│   └── AutoBidController.java
│
├── service/                 (Interface)
│   ├── UserService.java
│   ├── ItemService.java
│   ├── AuctionService.java
│   ├── BidService.java
│   └── AutoBidService.java
│
├── service/impl/            (Implementation)
│   ├── UserServiceImpl.java
│   ├── ItemServiceImpl.java
│   ├── AuctionServiceImpl.java
│   ├── BidServiceImpl.java
│   ├── AutoBidServiceImpl.java
│   ├── AuctionManager.java
│   ├── AuctionExtensionManager.java
│   └── ServerSocket.java
│
├── repository/              (Interface)
│   ├── UserDAO.java
│   ├── ItemDAO.java
│   ├── AuctionDAO.java
│   ├── BidDAO.java
│   └── BaseDAO.java
│
├── repository/impl/         (Implementation)
│   ├── UserDAOImpl.java
│   ├── ItemDAOImpl.java
│   ├── AuctionDAOImpl.java
│   └── BidDAOImpl.java
│
├── socket/                  (Socket komunikasi)
│   ├── AuctionSocketServer.java
│   ├── ClientHandler.java
│   └── observer/
│       ├── Observer.java
│       ├── Subject.java
│       ├── SocketClientObserver.java
│
├── factory/                 (Design Pattern)
│   ├── ItemFactory.java
│   ├── ArtItemFactory.java
│
├── exception/               (Custom exceptions)
│   └── AuthenticationException.java
│
├── concurrency/             (Concurrency utilities)
│
└── AuctionApplication.java  (Main entry point)
```

---

## 🚀 QUY TRÌNH KHỞI ĐỘNG SERVER

```
java AuctionApplication [port]

1️⃣ Khởi tạo DAOs:
   ├─ UserDAOImpl
   ├─ ItemDAOImpl
   ├─ AuctionDAOImpl
   └─ BidDAOImpl

2️⃣ Khởi tạo Services:
   ├─ UserServiceImpl(userDAO)
   ├─ ItemServiceImpl(itemDAO)
   ├─ AuctionServiceImpl(auctionDAO, bidDAO, userDAO)
   └─ BidServiceImpl(bidDAO, auctionDAO, userDAO)

3️⃣ Tạo Socket Server:
   ├─ Port: 5000 (default)
   ├─ Lắng nghe connections
   └─ Mỗi client → thread riêng

4️⃣ Khởi động Background Tasks:
   ├─ AuctionManager.startAutoClose()
   ├─ Task: Đóng phiên hết hạn mỗi 5 giây
   └─ ShutdownHook: Clean up khi tắt

5️⃣ Server ready:
   ✅ Sẵn sàng nhận kết nối từ clients
```

---

## 💡 KEY LEARNINGS

| Concept | Áp Dụng | Lợi Ích |
|---------|--------|--------|
| **MVC Pattern** | Controller → Service → DAO | Tách biệt logic, dễ maintain |
| **DAO Pattern** | Trừu tượng hóa DB | Dễ thay đổi DB impl |
| **Observer Pattern** | Real-time notifications | Tất cả clients cập nhật liên tục |
| **Factory Pattern** | Tạo Items | Đa hình hóa item types |
| **Transaction** | FOR UPDATE + commit/rollback | Tính ACID |
| **Thread Pool** | Mỗi client 1 thread | Xử lý concurrent requests |
| **Singleton** | AuctionManager | 1 instance cho background tasks |

---

**Tác giả**: GitHub Copilot  
**Ngày**: May 8, 2026

