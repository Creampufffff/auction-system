`concurrency: Multitasking
config : config
controller : controller
dto : Data Transfer Object
entity : model
exception : exception
repository : data....
service : Model
socket : Socket.....
`
├── config                <- [1đ Design Pattern] Cấu hình WebSocket, Security
├── controller            <- [0.5đ MVC] Tiếp nhận yêu cầu từ Client
├── service               <- [1đ Chức năng] Xử lý logic đấu giá
│   ├── impl              <- Triển khai cụ thể các Service
│   └── strategy          <- [1đ Design Pattern] Ví dụ: Các chiến lược đấu giá (Auto-bid)
├── repository            <- [0.5đ MVC] DAO - Tương tác Database
├── socket                <- [0.5đ Realtime] Xử lý WebSocket Handler / Observer
├── exception             <- [1đ Xử lý lỗi] Global Exception Handler, Custom Exception
└── concurrency           <- [1đ Concurrency] Quản lý Lock, xử lý Race Condition

Observer Pattern
Singleton Pattern
Factory Pattern
Strategy Pattern
Command Pattern
MVC (Model-View-Controller)