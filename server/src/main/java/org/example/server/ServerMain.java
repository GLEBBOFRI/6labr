package org.example.server;

import org.example.collection.CollectionManager;
import org.example.collection.exceptions.ValidationException;
import org.example.network.Request;
import org.example.network.Response;
import org.example.server.commands.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerMain {
    private static final Logger logger = Logger.getLogger(ServerMain.class.getName());
    private final int port;
    private final CollectionManager collectionManager;
    private final Map<String, Command> commands;
    private volatile boolean isRunning;

    public ServerMain(int port, CollectionManager collectionManager, Map<String, Command> commands) {
        this.port = port;
        this.collectionManager = collectionManager;
        this.commands = commands;
        this.isRunning = true;
        setupLogger();
    }

    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("server.log", true);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Error setting up logger: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar server.jar <json_file>");
            System.exit(1);
        }

        try {
            CollectionManager collectionManager = new CollectionManager();
            collectionManager.loadCollection(args[0]);

            Map<String, Command> commands = new HashMap<>();
            registerCommands(commands, collectionManager);

            new ServerMain(12345, collectionManager, commands).start();
        } catch (IOException | ValidationException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                handleClient(clientSocket); // Обрабатываем клиента в основном потоке
            }
        } catch (IOException e) {
            if (isRunning) {
                logger.log(Level.SEVERE, "Server error: " + e.getMessage(), e);
            }
        } finally {
            logger.info("Server stopped");
        }
    }

    public void stop() {
        isRunning = false;
        logger.info("Server is shutting down");
    }

    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            Request request;
            while ((request = (Request) in.readObject()) != null) {
                logger.info("Received request from " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + ": " + request);
                Command command = commands.get(request.getCommandName().toLowerCase());

                if (command == null) {
                    Response response = new Response("Command not found");
                    out.writeObject(response);
                    logger.warning("Command not found from " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + ": " + request.getCommandName());
                } else {
                    Response response = command.execute(request);
                    out.writeObject(response);
                    logger.info("Executed command '" + request.getCommandName() + "' for " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + ", sent response: " + response);
                }
                out.flush();
            }
            logger.info("Client " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + " disconnected.");

        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Invalid request format from " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + ": " + e.getMessage());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Client connection error with " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                logger.log(Level.INFO, "Socket closed for " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error closing client socket for " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + ": " + e.getMessage(), e);
            }
        }
    }

    private static void registerCommands(Map<String, Command> commands,
                                         CollectionManager collectionManager) {
        commands.put("info", new Info(collectionManager));
        commands.put("show", new Show(collectionManager));
        commands.put("insert", new Insert(collectionManager));
        commands.put("update", new Update(collectionManager));
        commands.put("remove_key", new RemoveKey(collectionManager));
        commands.put("clear", new Clear(collectionManager));
        commands.put("replace_if_greater", new ReplaceIfGreater(collectionManager));
        commands.put("remove_greater_key", new RemoveGreaterKey(collectionManager));
        commands.put("remove_lower_key", new RemoveLowerKey(collectionManager));
        commands.put("remove_all_by_standard_of_living",
                new RemoveAllByStandardOfLiving(collectionManager));
        commands.put("average_of_meters_above_sea_level",
                new AverageOfMetersAboveSeaLevel(collectionManager));
        commands.put("filter_starts_with_name",
                new FilterStartsWithName(collectionManager));
    }
}