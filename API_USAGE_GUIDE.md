# 📖 HƯỚNG DẪN SỬ DỤNG CÁC API - AUCTION SYSTEM

> Tài liệu này hướng dẫn cách sử dụng các Controllers và Services trong Auction Server

## 📍 I. KIẾN TRÚC TẦNG CONTROLLER

Tất cả Controllers theo mô hình này:
```java
public class XyzController {
    private final XyzService xyzService;  // Inject Service
    
    public XyzController(XyzService xyzService) {
        this.xyzService = xyzService;
    }
    
    // Các phương thức công khai (public)
    public void doSomething(...) {
        xyzService.methodCall(...);  // Gọi Service để xử lý logic
    }
}
```

**Tại sao chia thành Controllers & Services?**
- Controller: Tiếp nhận yêu cầu, validate input cơ bản
- Service: Xử lý logic nghiệp vụ (business logic)
- DAO: Tương tác với database

---

## 👤 1. USER MANAGEMENT - UserController

### **1.1 Đăng Ký Tài Khoản**

```java
// Code trong server
UserController userController = new UserController(userService);

Bidder newUser = new Bidder("alice", "password123", "alice@example.com");
userController.register(newUser);  // ≈ INSERT vào database

// Nếu username đã tồn tại → throw IllegalArgumentException
// Nếu OK → User được lưu vào database
```

**Luồng xử lý:**
```
register(user)
  ↓
UserServiceImpl.register()
  ├─ if (user == null) → throw error
  ├─ if (findByUsername(user.name) != null) → throw error
  │    (Username đã tồn tại)
  ├─ user.setId(UUID)
  ├─ userDAO.save(user)
  └─ return
```

---

### **1.2 Đăng Nhập**

```java
User loginUser = userController.login("alice", "password123");

// Nếu sai mật khẩu → throw UserAuthException
// Nếu user không tồn tại → throw IllegalArgumentException
// Nếu OK → return User object
```

**Luồng xử lý:**
```
login(username, password)
  ↓
UserServiceImpl.login()
  ├─ User user = userDAO.findByUsername(username)
  ├─ if (user == null) → throw error
  ├─ if (!password.equals(user.getPassword())) → throw error
  │    ⚠️ LƯU Ý: Trong production nên hash password!
  └─ return user
```

---

### **1.3 Quản Lý Tiền**

```java
// Lấy số dư
double balance = userController.getBalance(userId);
// → return 1500000.0  (VND)

// Nạp tiền
userController.deposit(userId, 500000);
// → balance = 2000000.0

// Rút tiền
userController.withdraw(userId, 300000);
// → balance = 1700000.0

// Nếu rút nhiều hơn số dư → throw IllegalArgumentException
```

**Luồng xử lý:**
```
deposit(userId, amount)
  ↓
UserServiceImpl.deposit()
  ├─ User user = userDAO.findById(userId)
  ├─ if (user == null) → throw error
  ├─ if (amount <= 0) → throw error
  ├─ user.setBalance(user.getBalance() + amount)
  ├─ userDAO.save(user)
  └─ return
```

---

### **1.4 Lấy Thông Tin User**

```java
User profile = userController.getUserProfile(userId);

// profile.getUsername()   → "alice"
// profile.getBalance()    → 1500000.0
// profile.getEmail()      → "alice@example.com"
```

---

## 📦 2. ITEM MANAGEMENT - ItemController

### **2.1 Tạo Vật Phẩm**

```java
// Tạo vật phẩm loại Art (Tranh vẽ)
Art artItem = new Art(
    description = "Tranh Mona Lisa",
    name = "Mona Lisa",
    startDateString = "2026-05-08T16:00:00",
    endDateString = "2026-05-08T18:00:00",
    startPrice = 1000000.0,
    minIncreasement = 50000.0,
    author = "Leonardo da Vinci"
);
artItem.setSellerId(sellerId);

itemController.createItem(artItem);
// → Item được lưu vào database
```

**Các loại Item:**

| Loại | Lớp | Trường Đặc Biệt |
|------|-----|----------------|
| Tranh | Art | author |
| Điện tử | Electronics | warrantyMonths |
| Xe | Vehicle | brand |

---

### **2.2 Truy Vấn Vật Phẩm**

```java
// Lấy 1 vật phẩm theo ID
Item item = itemController.getItem(itemId);

// Lấy tất cả vật phẩm
List<Item> allItems = itemController.getAllItems();
// Thường được dùng để hiển thị marketplace
```

---

### **2.3 Xóa Vật Phẩm**

```java
itemController.deleteItem(itemId);
// ⚠️ Lưu ý: Chỉ được xóa nếu item chưa được đấu giá
```

---

## 🏆 3. AUCTION MANAGEMENT - AuctionController

### **3.1 Tạo Phiên Đấu Giá**

```java
// Bước 1: Tạo vật phẩm
Art item = new Art(...);
item.setSellerId("seller_id_123");

// Bước 2: Tạo Auction từ Item
Auction auction = new Auction(item);
// Trạng thái mặc định: OPEN

// Bước 3: Lưu Auction
auctionController.createAuction(auction);
// → Auction được lưu vào database
// → auction.getId() được tạo tự động
```

---

### **3.2 Bắt Đầu Phiên**

```java
// Phiên chuyển từ OPEN → RUNNING
// Lúc này người dùng mới có thể đư trị được bid

auctionController.startAuction(auctionId);

// Nếu phiên không ở OPEN → throw error
// Nếu OK → Status thành RUNNING
```

**Trạng thái Auction:**
```
OPEN ──→ RUNNING ──→ FINISHED
├─ Chưa bắt đầu  ├─ Đang diễn ra  ├─ Kết thúc
└─ Chỉ được      └─ Có thể bid     └─ Đã quyết toán
  start/delete      và xem giá       tiền
```

---

### **3.3 Xem Danh Sách Phiên**

```java
// Lấy tất cả phiên (OPEN + RUNNING + FINISHED)
List<Auction> allAuctions = auctionController.getAllAuctions();

// Chỉ lấy phiên đang hoạt động (RUNNING)
// Đây là những phiên mà người dùng có thể bid
List<Auction> activeAuctions = auctionController.getActiveAuctions();

// Lấy 1 phiên cụ thể
Auction auction = auctionController.getAuction(auctionId);
```

---

### **3.4 Kết Thúc Phiên**

```java
// Phiên chuyển từ RUNNING → FINISHED
// Lúc này hệ thống sẽ thanh toán tiền

auctionController.endAuction(auctionId);

// Luồng thanh toán:
// 1. Tìm người thắng (bid cao nhất)
// 2. Trừ tiền tài khoản người thắng
// 3. Cộng tiền tài khoản người bán
// 4. Lưu cả 2 để đảm bảo consistency
```

---

## 💰 4. BIDDING - BidController

### **4.1 Đặt Giá Thầu**

```java
// Tạo BidTransaction
Bidder bidder = ...;  // Người đặt giá
Auction auction = ...;
double amount = 1050000.0;  // Giá đặt

BidTransaction bid = new BidTransaction(bidder, auction, amount);

// Đặt giá
bidController.placeBid(bid);

// Validation trong Service:
// ✓ Bid amount > 0
// ✓ Bidder tồn tại
// ✓ Bidder có đủ tiền
// ✓ Giá > (hiện tại + mức tăng)
// ✓ Phiên ở trạng thái RUNNING
```

**⚠️ CẤU HÌNH CÁCH TÍN:**

```
Giá khởi điểm: 1000000 VND
Mức tăng tối thiểu: 50000 VND

Bid hợp lệ:
├─ Bid 1: 1050000 (1000000 + 50000) ✓
├─ Bid 2: 1100000 (1050000 + 50000) ✓
├─ Bid 3: 1040000 (< 1050000 + 50000) ✗ REJECTED

Person A bid: 1050000
Person B bid: 1100000
Person C bid: 1050000 ✗ Không được vì < 1100000 + 50000
Person C bid: 1150000 ✓ Được vì > 1100000 + 50000
```

---

### **4.2 Xem Lịch Sử Giá Thầu**

```java
// Lấy lịch sử bid của một phiên
List<BidTransaction> history = bidController.getAuctionBidHistory(auctionId);

// Kết quả được sắp xếp cao → thấp:
// Bid 1: 1150000 VND (cao nhất)
// Bid 2: 1100000 VND
// Bid 3: 1050000 VND

// Người bid cuối cùng là người thắng
BidTransaction winning = history.get(0);
```

---

### **4.3 Xóa Bid (Admin)**

```java
bidController.deleteBid(bidId);
// ⚠️ Thường dùng để xóa bid lỗi hoặc gian lận
```

---

## 🤖 5. AUTO-BIDDING - AutoBidController

### **5.1 Tạo Lệnh AutoBid**

```java
// Tạo AutoBid: "Tôi sẵn sàng trả tối đa 2 triệu VND"
AutoBid autoBid = new AutoBid(
    auctionId = "auction_123",
    bidderId = "bidder_456",
    maxAutoAmount = 2000000.0
);

autoBidController.setAutoBid(autoBid);

// Hệ thống sẽ:
// ├─ Lưu AutoBid
// ├─ Kích hoạt nó khi có bid mới
// └─ Tự động bid lên nếu chưa vượt 2 triệu
```

---

### **5.2 Cơ Chế AutoBid**

```
Ví dụ:
├─ Person A: Lập AutoBid maxAutoAmount = 1 triệu
├─ Person B: Lập AutoBid maxAutoAmount = 3 triệu (ưu tiên cao hơn)
├─ Person C: Bid thủ công = 900 nghìn
│
├─ Hệ thống xử lý:
│  1. Giá hiện tại: 900 nghìn
│  2. Giá sắp tới: 950 nghìn (900k + 50k min_increment)
│  3. Kiểm tra Person B (3 triệu >= 950k) ✓ BID LÊN 950K
│  4. Kiểm tra Person A (1 triệu >= 1 triệu) ✓ BID LÊN 1 TRIỆU
│  5. Kiểm tra Person B (3 triệu >= 1.05 triệu) ✓ BID LÊN 1.05 TRIỆU
│  6. Kiểm tra Person A (1 triệu >= 1.1 triệu) ✗ STOP
│
└─ Kết quả: Person B thắng với giá 1.05 triệu
```

---

### **5.3 Xem & Hủy AutoBid**

```java
// Xem tất cả AutoBid của một phiên
List<AutoBid> auctionBids = autoBidController.getAuctionAutoBids(auctionId);

// Xem AutoBid của một người dùng
List<AutoBid> myBids = autoBidController.getBidderAutoBids(bidderId);

// Hủy AutoBid
autoBidController.cancelAutoBid(autoBidId);
// → AutoBid sẽ bị đánh dấu inactive
// → Không còn bid tự động nữa
```

---

## ⏱️ 6. ANTI-SNIPING - AuctionExtensionManager

### **6.1 Cơ Chế Gia Hạn**

```
Tình huống:
├─ Phiên kết thúc lúc 15:00:00
├─ Hiện tại: 14:56:30 (3 phút 30 giây trước hết)
├─ Person X bid vừa lúc 14:56:30
│
└─ CÓ TRONG 5 PHÚT CUỐI? ✓ YES
   ├─ Gia hạn thêm 5 phút
   ├─ Kết thúc mới: 15:05:00
   ├─ Lần gia hạn: 1/3
   └─ Người khác có cơ hội bid lại

Tại sao gia hạn?
├─ Ngăn chặn "sniping" (bid lén lút phút cuối)
├─ Công bằng cho tất cả người dùng
└─ Mỗi bid mới → reset đồng hồ
```

---

### **6.2 Sử Dụng AuctionExtensionManager**

```java
// ⚠️ Đây là utility class (static methods)
// Không cần khởi tạo instance

// Kiểm tra thời gian còn lại
long secondsLeft = AuctionExtensionManager.getTimeRemaining(auction);
// → return 300 (5 phút)

// Kiểm tra nên đóng phiên không
boolean shouldClose = AuctionExtensionManager.shouldClose(auction);
// → true nếu đã quá hết giờ

// Kiểm tra & gia hạn
boolean extended = AuctionExtensionManager.checkAndExtend(auction);
// → true nếu được gia hạn
// → false nếu không cần gia hạn
```

---

## 🔄 7. LUỒNG HOÀN CHỈNH - Từ Tạo đến Kết Thúc

```
BƯỚC 1: Seller tạo vật phẩm
├─ itemController.createItem(item)
└─ Item được lưu

BƯỚC 2: Tạo phiên đấu giá
├─ auctionController.createAuction(auction)
└─ Auction status = OPEN

BƯỚC 3: Bắt đầu phiên
├─ auctionController.startAuction(auctionId)
├─ Auction status = RUNNING
└─ Người dùng có thể bid

BƯỚC 4: Phiên diễn ra
├─ Bidders lập AutoBids (nếu muốn)
├─ Bidders bid thủ công
├─ Hệ thống xử lý AutoBids
├─ Mỗi bid → kiểm tra & gia hạn nếu cần
└─ Thông báo real-time đến tất cả clients

BƯỚC 5: Phiên hết giờ
├─ AuctionManager (background) phát hiện
├─ hoặc Admin gọi auctionController.endAuction()
├─ Tìm person thắng (bid cao nhất)
└─ Thanh toán tiền

BƯỚC 6: Phiên kết thúc
├─ Trừ tiền từ bidder thắng
├─ Cộng tiền cho seller
├─ Auction status = FINISHED
└─ Notify all clients
```

---

## 📊 8. ĐẶC BIỆT LƯU Ý

### **Concurrency & Thread Safety**

```java
// ❌ NGUY HIỂM: Nếu 2 bid đến cùng lúc
Thread 1: placeBid(500000) at 10:00:00.001
Thread 2: placeBid(600000) at 10:00:00.002

// ✅ CÓ BẢO VỆ: BidDAOImpl.placeBidSafely()
├─ FOR UPDATE lock trên auctions table
├─ Transaction isolation
└─ Đảm bảo atomicity
```

### **Atomicity - All or Nothing**

```java
// ❌ NGUY HIỂM
userDAO.save(winner);     // Trừ tiền
// Server crash ở đây!!!
seller.deposit(...);
sellerDAO.save(seller);   // Không execute

// ✅ AN TOÀN
connection.setAutoCommit(false);
try {
    userDAO.save(winner);
    sellerDAO.save(seller);
    connection.commit();   // Cùng lúc hoặc không
} catch (Exception e) {
    connection.rollback();  // Quay lại trạng thái cũ
}
```

### **Validation & Input Checking**

```java
// Controller kiểm tra đầu tiên
if (user == null) throw IllegalArgumentException("User is null");

// Service kiểm tra logic
if (wallet < bidAmount) throw InsufficientBalanceException();

// DAO kiểm tra database integrity
if (DB.execute(sql) == 0) throw IllegalStateException();
```

---

## 🎯 CHEAT SHEET

```java
// ====== QUICK REFERENCE ======

// 1. Tạo & Đăng ký
UserController uc = new UserController(userService);
Bidder user = new Bidder("name", "pass", "email");
uc.register(user);

// 2. Đăng nhập
User loggedIn = uc.login("name", "pass");

// 3. Tạo & Bắt đầu phiên
ItemController ic = new ItemController(itemService);
AuctionController ac = new AuctionController(auctionService);
Art item = new Art(...);
ic.createItem(item);
Auction auction = new Auction(item);
ac.createAuction(auction);
ac.startAuction(auction.getId());

// 4. Đặt giá
BidController bc = new BidController(bidService);
BidTransaction bid = new BidTransaction(bidder, auction, 1050000);
bc.placeBid(bid);

// 5. AutoBid
AutoBidController abc = new AutoBidController(autoBidService);
AutoBid autoBid = new AutoBid(auctionId, bidderId, 2000000);
abc.setAutoBid(autoBid);

// 6. Kết thúc
ac.endAuction(auctionId);
```

---

**Tác giả**: GitHub Copilot  
**Ngày**: May 8, 2026  
**Version**: 1.0

