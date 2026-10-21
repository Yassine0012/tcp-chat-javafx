# 3-Minute Demo Video - Script and Shot List

**Target length: 3:00.** Record the screen with OBS Studio / Xbox Game Bar / QuickTime and read the narration naturally (it is a guide, not a teleprompter).

## Before you press record
1. Build both JARs (`./build-all.sh`) and open the project in IntelliJ with `ChatServer.java`, `ChatClient.java`, `ChatView.java` and `server.properties` in tabs.
2. Arrange the screen: IntelliJ on the left, space on the right for **1 server + 3 client windows**.
3. Open a terminal in `TCPServer/target` and two more in `TCPClient/target`.
4. Increase IntelliJ font size (Ctrl +) so the code is readable in the video.

## Part 1 - Source walkthrough (0:00 - 1:00)

| Time | Show | Say |
|---|---|---|
| 0:00 | Project tree, both Maven projects | "This is a multi-client chat over TCP sockets with JavaFX. Two Maven projects: TCPServer and TCPClient." |
| 0:10 | Package folders `model / view / controller / config` | "I separated the code into Model, View and Controller. The **model package contains all socket logic and has zero JavaFX imports**; the view only builds GridPane layouts." |
| 0:22 | `ChatServer.java` -> `acceptLoop()` and `Executors.newVirtualThreadPerTaskExecutor()` | "The server accepts connections in a loop and starts one **virtual thread per client**, so hundreds of clients can be served concurrently without heavy OS threads." |
| 0:35 | `ClientHandler.run()` (JOIN, MSG, ALLUSERS, BYE branches) | "Each handler speaks a small line protocol: JOIN, MSG, ALLUSERS and BYE. Read-only guests are refused when they try to send." |
| 0:45 | `ServerListener` interface, then `ServerController` | "The model reports events through this listener interface. The controller implements it and uses Platform.runLater to update the UI, which keeps model and view decoupled." |
| 0:55 | `server.properties` | "IP and port come from a properties file, so I can change the environment without recompiling." |

## Part 2 - Live demo (1:00 - 2:40)

| Time | Action | Say |
|---|---|---|
| 1:00 | Terminal: `java -cp TCPServer.jar TCPServer` -> server window opens | "Starting the server: the log shows 'Server Started' and 'Waiting for Client'." |
| 1:10 | Terminal: `java -cp TCPClient.jar TCPClient localhost 3000` (client 1). Type `alice`, Join | "Client one joins as alice. The server logs 'Welcome alice' and the user appears in the list with its own random colour." |
| 1:25 | Start client 2 -> `bob`. Start client 3 -> **leave username empty**, click "Join read-only" | "Bob joins. The third client leaves the username empty, so it enters **READ-ONLY MODE** - the input box is disabled." |
| 1:45 | Alice sends "Hello everyone!" (Enter). Bob replies using the SEND button | "Messages are sent with Enter or the SEND button. Every client receives them with sender and timestamp, including the read-only guest." |
| 2:00 | Point at the Online label + green circle | "The header shows the Online status indicator." |
| 2:05 | In Bob's window type `allUsers` | "Typing allUsers returns the active users - only to Bob; Alice does not see it." |
| 2:15 | Try a duplicate: relaunch a client with `alice` | "Duplicate usernames are rejected." |
| 2:25 | Bob types `bye` -> returns to login. Alice clicks **LEAVE** | "bye, end or the LEAVE button disconnect cleanly. Others get 'bob left the chat' and the server list and log update immediately." |
| 2:40 | Show server window: log + remaining users | "The server log has the full activity history." |

## Part 3 - Wrap-up (2:40 - 3:00)

| Time | Show | Say |
|---|---|---|
| 2:40 | `mvn test` output (green) or `ChatServerTest` | "The server model is covered by headless tests, including 50 concurrent clients - possible because the model is independent of JavaFX." |
| 2:50 | README + UML folder | "The repository contains the README, UML class, deployment and sequence diagrams, and runnable JARs. Thanks for watching." |

## Tips
- Rehearse once with a stopwatch; the client launches are the part most likely to run long, so **pre-start** the three client windows on the login screen if you are short on time.
- Keep the mouse slow and zoom the video editor on the code when needed.
- Export as MP4 (1080p, <100 MB) and upload to YouTube (unlisted) / Drive if the platform has a size limit.
