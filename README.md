# 💬 ConnectX – Secure Desktop Messaging Application

**ConnectX** is a Java-based desktop messaging application developed using Java Swing and SQLite. It provides a secure client-server communication environment where users can register, log in, exchange private messages, share files, and manage their profiles through a modern desktop interface.

## ✨ Features

- **User Authentication:** User registration and login with server-side authentication.
- **Password Security:** Password hashing using PBKDF2WithHmacSHA256.
- **Secure Communication:** TLS-encrypted client-server connections.
- **Private Messaging:** Send and receive private messages between users.
- **Chat History:** Retrieve previous conversations.
- **Online Users:** View users currently connected to the server.
- **File Sharing:** Share image and document attachments (up to 5 MB).
- **Image Preview:** Preview shared images inside the chat interface.
- **User Profiles:** Manage display names, biographies, and profile pictures.
- **Typing Indicator:** See when another user is typing.
- **Modern GUI:** Dark-themed interface with purple and cyan accents.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Core application development |
| Java Swing | Desktop graphical user interface |
| SQLite | Database management |
| JDBC | Database connectivity |
| Java Sockets | Client-server communication |
| SSL/TLS | Encrypted communication |
| PBKDF2 | Password hashing |
| Git & GitHub | Version control and source code hosting |

## 🏗️ Project Architecture

ConnectX follows a client-server architecture.

1. **Client:** Provides the graphical interface and allows users to interact with the application.
2. **Server:** Handles client connections, authentication, messaging, and communication.
3. **Database:** SQLite stores user information and chat-related data.
4. **Security:** TLS protects communication between the client and server.

## 📂 Project Structure

```text
ConnectX/
│
├── assets/
│   └── fonts/
│
├── lib/
│   └── sqlite-jdbc-3.54.1.0.jar
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── connectx/
│                   ├── app/
│                   ├── client/
│                   ├── gui/
│                   ├── server/
│                   ├── security/
│                   ├── dao/
│                   ├── database/
│                   └── util/
│
├── attachments/       # Local attachment storage
├── data/              # Local SQLite database
├── certs/             # Local TLS certificates
├── out/               # Compiled Java classes
└── .gitignore
```

## 💻 Requirements

- Java Development Kit (JDK 25 tested)
- Visual Studio Code or any Java IDE
- SQLite JDBC Driver (included in the `lib` folder)
- Windows / compatible Java environment

## 🚀 How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/tanu-2104/ConnectX.git
```

### 2. Open the Project

Open the ConnectX folder in Visual Studio Code or PowerShell.

### 3. Configure TLS Certificates

The local TLS certificate and truststore files are intentionally excluded from GitHub for security reasons.

Create/configure the required local certificate files in the `certs/` folder according to the application's `TlsConfig.java` settings before running the application.

### 4. Compile the Project

Run this command in PowerShell from the project directory:

```powershell
javac -encoding UTF-8 -cp ".\lib\sqlite-jdbc-3.54.1.0.jar" -d .\out (Get-ChildItem -Recurse .\src\main\java -Filter *.java).FullName
```

### 5. Start the Server

Open the first terminal and run:

```powershell
java -cp ".\out;.\lib\sqlite-jdbc-3.54.1.0.jar" com.connectx.server.ChatServer
```

### 6. Start the Client

Open a second terminal and run:

```powershell
java -cp ".\out;.\lib\sqlite-jdbc-3.54.1.0.jar" com.connectx.app.Main
```

The server must be running before launching the client.

## 🔐 Security

ConnectX includes security features such as:

- PBKDF2WithHmacSHA256 password hashing.
- TLS-based encrypted client-server communication.
- Server-side authentication.
- Local certificate and database file protection through `.gitignore`.

**Note:** This project currently uses a locally configured client-server environment. It is not a publicly hosted cloud messaging service.

## 🔮 Future Scope

- Cloud-based server deployment.
- Real-time group messaging improvements.
- Voice and video calling.
- Push notifications.
- Improved mobile application support.
- Enhanced security and scalability.

## 🎓 Project Information

This project was developed as part of the academic Mini Project for the Bachelor of Technology in Artificial Intelligence and Data Science.

## 📜 License

This project is developed for academic and educational purposes.
