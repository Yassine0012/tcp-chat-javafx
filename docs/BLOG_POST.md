# Building a Multi-Client Chat with Java 21 Virtual Threads and JavaFX

*Draft for Medium / Dev.to - replace the repository link, add your own screenshots and tweak the voice.*

I recently built a group chat as a networking mini-project: a TCP server that fans messages out to many
clients, and JavaFX front-ends for both sides. Here is how it is put together and what I learned.

## The goal

- A **server** that accepts many clients at once and broadcasts each message with the sender's name and time.
- A **client** with a username screen, a **READ-ONLY MODE** for users without a name, `allUsers`, and `end` / `bye` / **LEAVE**.
- A server window with a live log and a coloured list of connected users.
- **Model and view fully decoupled**, IP/port in a config file, and runnable JARs.

## Architecture in one picture

```
 JavaFX View  <--  Controller  <--(listener interface)--  Model (sockets, threads, protocol)
```

The model never imports JavaFX. It reports what happens ("a client joined", "a message arrived") through a tiny
listener interface. The controller implements that interface and moves each event to the JavaFX thread with
`Platform.runLater`. That single rule gave me two benefits: no threading bugs in the UI, and a server I can
test without a display.

## Virtual threads: the simple concurrency model

The classic approach is "one thread per client", which is easy to reason about but expensive with platform threads.
Java 21 virtual threads make it cheap again, and the code stays blocking and straightforward:

```java
executor = Executors.newVirtualThreadPerTaskExecutor();
...
Socket socket = serverSocket.accept();
executor.submit(new ClientHandler(socket, this));
```

Each `ClientHandler` just loops on `readLine()`. When it blocks, the virtual thread is parked and the carrier
thread moves on. No selectors, no callbacks, no thread-pool tuning. The client uses the same trick for its
reader: `Thread.ofVirtual().start(() -> readLoop(in))`.

## A tiny text protocol

I kept the wire format human-readable: one line per message, fields separated by `|`.

```
JOIN|alice            OK|alice|false
MSG|hello             MSG|14:03:22|alice|hello
ALLUSERS              USERS|alice,bob
BYE                   SYS|bob left the chat
```

An empty `JOIN|` means "guest": the server names the user `Guest-N (read-only)` and refuses any `MSG` from it.
Being able to type protocol lines with `nc` while debugging saved a lot of time.

## Challenges and fixes

**1. Ordering at login.** If a new client were added to the broadcast list before its `OK` reply was sent, another user's
message could arrive first and break the handshake. Design choice: send `OK` first and only then add the handler to the list.

**2. "JavaFX runtime components are missing".** A shaded JAR whose main class extends `Application` refuses to
start. Fix: a plain `TCPClient` / `TCPServer` class with `main()` that calls `Application.launch(...)`.

**3. Keeping the UI responsive.** `connect()` blocks, so the controller runs it on a virtual thread and updates the
UI with `Platform.runLater`. Socket writes go through a single-thread executor, which also preserves message order.

**4. Wrapping long messages in a `ListView`.** Cells grow horizontally by default. Setting
`setWrapText(true)` plus `setPrefWidth(0)` makes them wrap to the list width.

**5. A flaky test taught me about my own design.** My 50-client test occasionally counted fewer users than expected right after the last
`OK` - exactly because `OK` is sent *before* registration finishes (challenge 1). The test now waits for the count to settle.

## Testing without a GUI

Since the model has no UI dependency, `ChatServerTest` boots the real server on a random port and drives it with
raw sockets: broadcast with timestamp, read-only guests, duplicate usernames, `allUsers` privacy, `bye`, and 50
concurrent clients - all headless, in well under a second.

## What I would improve next

- A per-client outbound queue so one slow client cannot delay a broadcast.
- TLS and real authentication (today "authentication" only means choosing a unique name).
- Private messages (`/w bob ...`) and chat rooms.

## Try it

```bash
git clone <your-repo-url>
./build-all.sh
java -cp TCPServer/target/TCPServer.jar TCPServer
java -cp TCPClient/target/TCPClient.jar TCPClient localhost 3000
```

Source, UML diagrams and the demo video are in the repository: **<your-repo-url>**.
