# Auction System

Link GitHub repository: https://github.com/Creampufffff/auction-system

## 1. Mo ta bai toan va pham vi he thong

Auction System la ung dung dau gia truc tuyen theo mo hinh client/server. He thong cho phep nguoi dung dang ky tai khoan, dang nhap, quan ly san pham dau gia, tham gia dau gia theo thoi gian thuc va theo doi lich su giao dich dau gia.

Pham vi he thong:

- Bidder co the xem danh sach phien dau gia, xem chi tiet san pham, dat gia, dat auto-bid, xem lich su bid va quan ly so du.
- Seller co the tao, sua, xoa, quan ly san pham/phien dau gia va upload hinh anh san pham.
- Admin co console co ban gom dashboard, danh sach user, danh sach auction va bid history.
- Server xu ly logic nghiep vu dau gia, concurrent bidding, cap nhat realtime, auto-close auction va anti-sniping.
- Database luu user, item, auction, bid transaction, so du va cac thong tin lien quan.

## 2. Cong nghe su dung

- Ngon ngu: Java
- UI client: JavaFX, FXML, CSS
- Server: Java Socket TCP, multithreading, scheduled task
- Database: MySQL 8, JDBC, HikariCP
- Build tool: Maven
- Test: JUnit 5, Mockito
- DevOps: Docker, Docker Compose, GitHub Actions, GitHub Container Registry, Watchtower

## 3. Moi truong chay va yeu cau cai dat

Chay local:

- JDK 17 tro len
- Maven 3.8 tro len
- MySQL 8
- Windows, Linux hoac macOS

Chay server bang Docker:

- Docker
- Docker Compose v2, dung lenh `docker compose`
- VPS Linux, khuyen dung Ubuntu 22.04/24.04

Kiem tra moi truong local:

```bash
java -version
mvn -version
mysql --version
```

Kiem tra Docker tren VPS:

```bash
docker --version
docker compose version
```

## 4. Cau truc thu muc

```text
auction-system/
|-- pom.xml                         # Maven parent project
|-- docker-compose.yml              # Chay server, MySQL va watchtower tren VPS
|-- docker/
|   `-- mysql/
|       `-- Dockerfile              # Custom MySQL image co san schema.sql
|-- auction-common/                 # Module dung chung
|   |-- pom.xml
|   `-- src/main/java/com/app/common/
|       |-- dto/                    # Request/response DTO
|       |-- entity/                 # User, Auction, Item, Bid...
|       |-- enums/                  # Enum trang thai
|       |-- exception/              # Exception dung chung
|       `-- mapper/                 # Mapper entity <-> DTO
|-- auction-server/                 # Module server
|   |-- Dockerfile                  # Docker image cho Java server
|   |-- pom.xml
|   |-- schema.sql                  # Script tao database
|   `-- src/main/java/com/auction/app/
|       |-- AuctionApplication.java # Entry point server
|       |-- config/                 # Cau hinh database
|       |-- controller/             # Controller xu ly request
|       |-- factory/                # Factory tao item
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

## 5. Cau hinh moi truong

Tao file `.env` tai thu muc goc khi chay local:

```env
AUCTION_DB_URL=jdbc:mysql://localhost:3306/auction_system
AUCTION_DB_USERNAME=root
AUCTION_DB_PASSWORD=123456
AUCTION_SERVER_HOST=127.0.0.1
AUCTION_SERVER_PORT=5000
```

Neu client ket noi toi server tren VPS, doi host:

```env
AUCTION_SERVER_HOST=<IP_VPS>
AUCTION_SERVER_PORT=5000
```

Khong commit file `.env` co mat khau that.

## 6. Khoi tao database khi chay local

Tao database va bang bang file `auction-server/schema.sql`.

Linux/macOS:

```bash
mysql -u root -p < auction-server/schema.sql
```

Windows PowerShell:

```powershell
Get-Content auction-server\schema.sql | mysql -u root -p
```

## 7. Build va test

Chay tu thu muc goc project.

Linux/macOS/Windows PowerShell:

```bash
mvn clean install
mvn test
```

Build rieng server:

```bash
mvn -pl auction-server -am clean package
```

Build rieng client:

```bash
mvn -pl auction-client -am clean package
```

## 8. Huong dan chay local Server/Client

Thu tu chay:

1. Khoi dong MySQL.
2. Import `auction-server/schema.sql`.
3. Tao/cap nhat file `.env`.
4. Chay server.
5. Mo terminal moi va chay client.

Chay server:

```bash
mvn -pl auction-server -am exec:java -Dexec.mainClass=com.auction.app.AuctionApplication
```

Neu dung Windows PowerShell va gap loi tham so `-D`, dung:

```powershell
mvn -pl auction-server -am exec:java "-Dexec.mainClass=com.auction.app.AuctionApplication"
```

Chay client:

```bash
mvn -pl auction-client -am exec:java -Dexec.mainClass=com.auction.MainApp
```

Windows PowerShell:

```powershell
mvn -pl auction-client -am exec:java "-Dexec.mainClass=com.auction.MainApp"
```

Server mac dinh lang nghe port `5000`. Client doc `AUCTION_SERVER_HOST` va `AUCTION_SERVER_PORT` tu `.env`.

## 9. Chay server va database bang Docker Compose

Cach nay dung cho VPS/deploy. Client JavaFX van chay tren may nguoi dung va ket noi den IP VPS port `5000`.

File `docker-compose.yml` gom:

- `mysql`: MySQL 8, dung image co san schema.
- `server`: Java auction server.
- `watchtower`: tu dong cap nhat server khi co image moi tren GHCR.

Tao file `.env` tren VPS:

```env
MYSQL_ROOT_PASSWORD=change_me_root_password
MYSQL_DATABASE=auction_system
MYSQL_USER=auction_user
MYSQL_PASSWORD=change_me_user_password
AUCTION_SERVER_PORT=5000
```

Chay lan dau tren VPS:

```bash
docker compose up -d
```

Xem log server:

```bash
docker compose logs -f server
```

Kiem tra timezone container:

```bash
docker exec -it auction-server date
```

Mo firewall port server:

```bash
sudo ufw allow 5000/tcp
```

Luu y:

- MySQL khong expose port `3306` ra ngoai VPS.
- Server ket noi MySQL qua hostname noi bo Docker: `mysql`.
- Khong dung `docker compose down -v` neu khong muon xoa volume database.

## 10. Tu dong cap nhat server khi push code

He thong dung GitHub Actions de build va push image len GitHub Container Registry:

- `ghcr.io/creampufffff/auction-server:latest`
- `ghcr.io/creampufffff/auction-mysql:latest`

Sau khi push code len `main` hoac `master`, workflow `Docker Publish` se build image moi. Watchtower tren VPS kiem tra image moi moi 60 giay va tu restart `auction-server`.

Kiem tra watchtower:

```bash
docker compose logs -f watchtower
```

Log update thanh cong co dang:

```text
Found new ghcr.io/creampufffff/auction-server:latest image
Stopping /auction-server
Creating /auction-server
Session done Failed=0 Scanned=1 Updated=1
```

## 11. Cac chuc nang da hoan thanh

- Dang ky tai khoan bidder/seller.
- Dang nhap va quan ly session client.
- Luu mat khau dang `password_hash`.
- Quan ly so du: xem so du, nap tien, rut tien.
- Tao va quan ly san pham dau gia theo loai: art, electronics, vehicle.
- Upload va hien thi hinh anh san pham.
- Tao, sua, xoa va xem chi tiet auction.
- Tu dong chuyen trang thai auction theo thoi gian: `OPEN`, `RUNNING`, `FINISHED`.
- Dat gia truc tiep qua socket.
- Xu ly concurrent bidding tai DAO bang transaction, row lock va cap nhat so du giu cho bidder.
- Realtime update khi co bid moi.
- Lich su bid cua auction va cua bidder.
- Auto-bid theo muc gia toi da.
- Anti-sniping: gia han auction khi co bid o gan thoi diem ket thuc.
- Ket thuc auction va xu ly nguoi thang.
- Admin console: dashboard, user list, auction list, bid history.
- Docker Compose deploy server + database.
- GitHub Actions build image va Watchtower auto update server tren VPS.
- Unit test cho entity, mapper, factory, controller va service chinh.

## 12. Mot so tinh huong ky thuat quan trong

- Concurrent bidding: nhieu client dat gia cung luc, server dung transaction va lock de tranh bid sai gia/so du.
- Realtime update: server broadcast event bid moi cho cac client dang theo doi.
- Auction lifecycle: background scheduler tu dong start/end auction theo thoi gian.
- Anti-sniping: neu bid xuat hien gan luc ket thuc, server gia han thoi gian auction.
- Docker deployment: server va MySQL chay cung Docker network de giam latency so voi database online ben ngoai.
- Auto update: VPS tu pull image moi va restart server khi GitHub Actions publish image moi.

## 13. Link bao cao PDF va video demo

- Bao cao PDF: TODO - cap nhat link Google Drive/GitHub release tai day.
- Video demo: TODO - cap nhat link video demo tai day.

## 14. Ghi chu khi nop bai

- Nhanh nop cuoi cung: `main`.
- Commit cuoi cung can truoc deadline `23:59, 04/06/2026`.
- Khong commit sau deadline.
- Truoc khi nop nen chay:

```bash
mvn clean test
```

- Kiem tra lai README, link bao cao PDF va link video demo.
- Kiem tra `.env` de khong lo mat khau production.
