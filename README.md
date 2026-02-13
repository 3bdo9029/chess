# ♕ BYU CS 240 Chess

This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html?presentationMode=readOnly#initialData=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-wABZg2YCAA


## Modules

The application has three modules.

- **Client**: The command line program used to play a game of chess over the network.
- **Server**: The command line program that listens for network requests from the client and manages users and games.
- **Shared**: Code that is used by both the client and the server. This includes the rules of chess and tracking the state of a game.

## Starter Code

As you create your chess application you will move through specific phases of development. This starts with implementing the moves of chess and finishes with sending game moves over the network between your client and server. You will start each phase by copying course provided [starter-code](starter-code/) for that phase into the source code of the project. Do not copy a phases' starter code before you are ready to begin work on that phase.

## IntelliJ Support

Open the project directory in IntelliJ in order to develop, run, and debug your code using an IDE.

## Maven Support

You can use the following commands to build, test, package, and run your code.

| Command                    | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `mvn compile`              | Builds the code                                 |
| `mvn package`              | Run the tests and build an Uber jar file        |
| `mvn package -DskipTests`  | Build an Uber jar file                          |
| `mvn install`              | Installs the packages into the local repository |
| `mvn test`                 | Run all the tests                               |
| `mvn -pl shared test`      | Run all the shared tests                        |
| `mvn -pl client exec:java` | Build and run the client `Main`                 |
| `mvn -pl server exec:java` | Build and run the server `Main`                 |

These commands are configured by the `pom.xml` (Project Object Model) files. There is a POM file in the root of the project, and one in each of the modules. The root POM defines any global dependencies and references the module POM files.

## Running the program using Java

Once you have compiled your project into an uber jar, you can execute it with the following command.

```sh
java -jar client/target/client-jar-with-dependencies.jar

♕ 240 Chess Client: chess.ChessPiece@7852e922
```
