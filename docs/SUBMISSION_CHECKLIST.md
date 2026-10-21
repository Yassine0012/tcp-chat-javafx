# Submission Checklist and Requirements Traceability

## 1. Deliverables

| Deliverable | Status | Where / how |
|---|---|---|
| Fully functional Maven projects (TCPServer, TCPClient) | Included | `TCPServer/pom.xml`, `TCPClient/pom.xml` |
| Executable JAR files | **You build them** (`./build-all.sh`) | `TCPServer/target/TCPServer.jar`, `TCPClient/target/TCPClient.jar` |
| 3-minute demo video | **You record it** | Follow [`DEMO_SCRIPT.md`](DEMO_SCRIPT.md) |
| UML class diagram | Included | `docs/uml/class-diagram-server.png`, `docs/uml/class-diagram-client.png` |
| UML deployment diagram | Included | `docs/uml/deployment-diagram.png` |
| Optional: use case + sequence diagrams | Included | `docs/uml/usecase-diagram.png`, `docs/uml/sequence-diagram.png` |
| GitHub repository + README + commit history | **You push it** (steps below) | root `README.md` |
| Optional technical post | Draft included | [`BLOG_POST.md`](BLOG_POST.md) |

Editable UML sources are the `.puml` files (render with `java -jar plantuml.jar -tpng docs/uml/*.puml`, the PlantUML VS Code / IntelliJ plugin, or plantuml.com).

## 2. Requirement -> implementation map

| Requirement | Implemented in |
|---|---|
| Server-client architecture over TCP | `ChatServer` (`ServerSocket`), `ChatClient` (`Socket`) |
| Username before chat | `LoginView`, `ChatClient.connect`, `ChatServer.register` |
| READ-ONLY MODE without username | Empty name -> `OK\|Guest-N (read-only)\|true`; `ChatView.setUser` disables input + shows badge; server refuses `MSG` |
| Send via SEND button or Enter | `ChatView` (Enter filter fires the SEND button), `ClientController.send` |
| `allUsers` -> list to that user only | `ChatClient.processInput` -> `ALLUSERS` -> `ClientHandler` replies `USERS\|...` to requester |
| Disconnect via `end`, `bye`, LEAVE | `ChatClient.processInput` / `disconnect`; LEAVE button in `ClientController.leave` |
| Online label + status circle | `ChatView` (`statusLabel`, `statusDot`) |
| Many concurrent connections | `Executors.newVirtualThreadPerTaskExecutor()` in `ChatServer`; 50-client test |
| Message formatted with username + time, sent to all | `ChatServer.broadcastChat` -> `MSG\|HH:mm:ss\|sender\|text` |
| Live ListView of connected users | `ServerView.usersView`, `ServerController.onClientJoined/Left` |
| Random background colours per user | `ServerView.colorFor` + cell factory |
| Log: "Server Started", "Waiting for Client", "Welcome [User]" | `ChatServer.start/acceptLoop/announceJoin` -> `ServerView.appendLog` |
| JavaFX with GridPane + CSS | `ServerView`, `ChatView`, `LoginView`; `server.css`, `client.css` |
| Virtual threads | `ChatServer` executor; `ChatClient` reader (`Thread.ofVirtual()`) |
| IP/port from config file | `ServerConfig`, `ClientConfig` (`*.properties`, no recompilation) |
| Model-View decoupling | `chat.model` has no JavaFX imports; headless `ChatServerTest` |
| `java TCPClient <ip> <port>` / `java TCPServer` | `TCPClient.java`, `TCPServer.java` (default package) |

## 3. Publishing to GitHub (so the commit history is real)

Make your own commits as you review/modify the project - graders look for incremental history. A sensible sequence:

```bash
cd chat-project
git init -b main
git add TCPServer/pom.xml TCPServer/config TCPServer/src/main/resources TCPServer/src/main/java/chat/config
git commit -m "server: project skeleton, pom and network configuration"
git add TCPServer/src/main/java/chat/model
git commit -m "server: socket model with virtual-thread client handlers and line protocol"
git add TCPServer/src/test
git commit -m "server: headless tests for join, broadcast, allUsers, read-only, concurrency"
git add TCPServer/src/main/java
git commit -m "server: JavaFX monitor (log, coloured user list) with controller"
git add TCPClient/pom.xml TCPClient/config TCPClient/src/main/resources TCPClient/src/main/java/chat/config TCPClient/src/main/java/chat/model
git commit -m "client: config and socket model"
git add TCPClient
git commit -m "client: login/chat views, read-only mode, controller"
git add docs/uml
git commit -m "docs: UML class, deployment, sequence and use case diagrams"
git add .
git commit -m "docs: README, demo script, screenshots, build scripts"

git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

Then add the JARs as a **GitHub Release** asset (Releases > Draft a new release > attach both JARs) and paste the repository link in your submission.

## 4. Final pre-submission check
- [ ] `./build-all.sh` succeeds on your machine; both JARs start (`java -cp ... TCPServer` / `TCPClient localhost 3000`).
- [ ] Tried 3 clients: one named, one second named, one with empty username (read-only).
- [ ] `allUsers`, `bye`, `end`, LEAVE all verified.
- [ ] Video is <= 3 minutes, shows code + live demo.
- [ ] README renders on GitHub (screenshots visible); repository is public or shared with the instructor.
- [ ] Blog post (optional) published and linked in README.
