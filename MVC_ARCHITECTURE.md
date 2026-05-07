# MVC Architecture Documentation

## Overview
The Auction System follows the **Model-View-Controller (MVC)** architectural pattern to separate concerns and maintain clean code structure.

---

## Architecture Layers

### 1. **Model Layer** (`Model`)
Located in: `auction-common/src/main/java/com/app/common/entity/`

**Entities**:
- `User` (abstract) → `Admin`, `Seller`, `Bidder`
- `Item` (abstract) → `Art`, `Electronics`, `Vehicle`
- `Auction` - Represents an auction session
- `BidTransaction` - Represents a bid in an auction
- `BaseEntity` - Base class with ID and timestamp

**Data Transfer Objects (DTOs)**:
- `LoginRequestDTO` - Login credentials
- `LoginResponseDTO` - Login result with user data
- `BidRequestDTO` - Bid placement request
- `AuctionListDTO` - Auction list response
- `AuctionUpdateDTO` - Auction status update

**Enums**:
- `Status` - Auction states: OPEN, RUNNING, FINISHED, CANCELED

---

### 2. **View Layer** (`View`)
Located in: `auction-client/src/main/java/com/auction/client/`

**Components**:
- `MainApp.java` - JavaFX application entry point
- FXML templates in `src/main/resources/`
- ClientHandler for socket communication

**Responsibilities**:
- Display UI elements using JavaFX
- Capture user interactions
- Send commands to controllers via socket
- Display real-time updates from server

---

### 3. **Controller Layer** (`Controller`)
Located in: `auction-server/src/main/java/com/auction/app/controller/`

**Controllers**:

#### **UserController**
```java
- register(User) - Register new user
- login(String username, String password) - Authenticate
- getUserProfile(String userId) - Get user info
- getBalance(String userId) - Get account balance
- deposit(String userId, double amount) - Add funds
- withdraw(String userId, double amount) - Withdraw funds
- deleteUser(String userId) - Delete account
```

#### **ItemController**
```java
- createItem(Item) - Create new item
- getItem(String itemId) - Retrieve item
- getAllItems() - List all items
- deleteItem(String itemId) - Delete item
```

#### **AuctionController**
```java
- createAuction(Auction) - Create new auction
- startAuction(String auctionId) - Start OPEN → RUNNING
- endAuction(String auctionId) - End RUNNING → FINISHED
- getAuction(String auctionId) - Get auction details
- getAllAuctions() - List all auctions
- getActiveAuctions() - List active (RUNNING) auctions
```

#### **BidController**
```java
- placeBid(BidTransaction) - Place a bid
- getBid(String bidId) - Get bid details
- getAllBids() - List all bids
- getAuctionBidHistory(String auctionId) - Get bids for auction
- deleteBid(String bidId) - Delete bid (admin)
```

---

### 4. **Service Layer** (`Service`)
Located in: `auction-server/src/main/java/com/auction/app/service/`

**Services**:
- `UserService` - User management business logic
- `ItemService` - Item management business logic
- `AuctionService` - Auction lifecycle management
- `BidService` - Bidding logic and validation
- `AuctionManager` - Background tasks (auto-close auctions)

**Responsibilities**:
- Implement business rules
- Validate input data
- Coordinate with DAO layer
- Handle exceptions

---

### 5. **Data Access Layer** (`Repository/DAO`)
Located in: `auction-server/src/main/java/com/auction/app/repository/`

**DAOs**:
- `UserDAO` / `UserDAOImpl` - User CRUD operations
- `ItemDAO` / `ItemDAOImpl` - Item CRUD operations
- `AuctionDAO` / `AuctionDAOImpl` - Auction CRUD operations
- `BidDAO` / `BidDAOImpl` - Bid CRUD and query operations

**Responsibilities**:
- Database connection management
- SQL query execution
- Transaction management
- Concurrency handling (row-level locking)

---

### 6. **Socket Communication Layer**
Located in: `auction-server/src/main/java/com/auction/app/socket/`

**Components**:
- `AuctionSocketServer` - TCP socket server (port 5000)
- `ClientHandler` - Processes client commands
- `SocketClientObserver` - Real-time notification delivery

**Communication Flow**:
```
Client Socket Connection
    ↓
AuctionSocketServer.accept()
    ↓
ClientHandler (threaded)
    ↓
Parse command → Route to Controller
    ↓
Controller → Service → DAO
    ↓
Response back to client
    ↓
Observer Pattern: Notify all connected clients
```

---

## Data Flow Example: Placing a Bid

### 1. **Client Side** (View)
```
User clicks "Place Bid" button
  ↓
Enter bid amount in UI
  ↓
Send command: "PLACE_BID auctionId|bidderId|amount"
  ↓
Socket sends to server
```

### 2. **Server Side** (Controller)
```
ClientHandler receives: "PLACE_BID auction1|user1|250"
  ↓
Parse command → Route to BidController.placeBid()
  ↓
Create BidTransaction object
```

### 3. **Business Logic** (Service)
```
BidService.placeBid(bid)
  ↓
Validate: Bid > 0, Bidder exists, Bidder has balance
  ↓
Call BidDAO.placeBidSafely()
```

### 4. **Database** (DAO)
```
BidDAOImpl.placeBidSafely()
  ↓
Start transaction
  ↓
Lock auction row (FOR UPDATE)
  ↓
Validate bid amount against current max bid + increment
  ↓
Insert bid into bid_transactions table
  ↓
Update auction highest_current_price
  ↓
Commit transaction
```

### 5. **Response & Notification** (Socket Observer)
```
Success response back to client: "OK|BID_PLACED|bidId"
  ↓
AuctionSocketServer.notifyObservers()
  ↓
ALL connected clients receive: "EVENT|BID_UPDATE|auctionId|newPrice"
  ↓
Clients update UI in real-time
```

---

## Design Patterns Used

### **Observer Pattern**
- `Observer<T>` interface
- `Subject<T>` interface
- `AuctionSocketServer` implements Subject
- `SocketClientObserver` implements Observer
- **Purpose**: Real-time notifications to multiple clients

### **Factory Pattern**
- `ItemFactory` interface
- `ArtItemFactory` implementation
- **Purpose**: Polymorphic item creation

### **DAO Pattern**
- Interface-based data access
- Abstraction of database operations
- **Purpose**: Loose coupling between service and database

### **Singleton Pattern**
- `AuctionManager.getInstance()`
- **Purpose**: Single instance manages background tasks

---

## Transaction & Concurrency Management

### **Atomicity** (All-or-Nothing)
```java
connection.setAutoCommit(false);

try {
    // Multiple operations in one transaction
    insertBid();
    updateAuctionPrice();
    updateUserBalance();
    
    connection.commit();
} catch (Exception e) {
    connection.rollback();
}
```

### **Isolation** (Row-level Locking)
```sql
SELECT * FROM auctions a
JOIN items i ON i.id = a.item_id
WHERE a.id = ?
FOR UPDATE;  -- Locks this row until transaction completes
```

### **Consistency** (Business Rules)
- Bid must be > current max bid + min increment
- Bidder must have sufficient balance
- Auction must be in RUNNING status

---

## Exception Handling

Custom exceptions for domain-specific errors:
- `UserAuthException` - Authentication failure
- `InvalidBidException` - Bid validation failure
- `InsufficientBalanceException` - Insufficient funds
- `AuctionClosedException` - Auction not in valid state
- `AuctionNotFoundException` - Auction not found

---

## Testing Strategy

### Unit Tests (`src/test/java/`)
- `UserServiceImplTest` - User operations
- `BidServiceImplTest` - Bidding logic
- `AuctionServiceImplTest` - Auction lifecycle

### Mock Objects
- `FakeUserDAO` - In-memory user repository
- `FakeBidDAO` - In-memory bid repository
- `FakeAuctionDAO` - In-memory auction repository

```
All 10 tests: ✅ PASS
Coverage: User registration, login, bid placement, auction state transitions
```

---

## Build & Deployment

### Maven Project Structure
```
auction-system/
├── auction-common/    (Entities, DTOs, Exceptions)
├── auction-server/    (Business logic, API)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── socket/
├── auction-client/    (UI, Socket client)
└── pom.xml
```

### Build Command
```bash
mvn clean compile
mvn test
mvn package
```

### Run Server
```bash
java -cp target/auction-server-1.0-SNAPSHOT.jar com.auction.app.AuctionApplication [port]
```

---

## Summary

The **MVC Architecture** ensures:
- ✅ **Separation of Concerns**: Each layer has distinct responsibility
- ✅ **Reusability**: Services and DAOs can be reused by different controllers
- ✅ **Testability**: Components can be tested independently with mocks
- ✅ **Maintainability**: Changes isolated to specific layers
- ✅ **Scalability**: Easy to add new controllers and services

