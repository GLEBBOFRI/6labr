package org.example;

import org.example.comands.*;
import org.example.consol.Console;
import org.example.consol.StandartConsole;
import org.example.exceptions.CommandExecutionError;
import org.example.exceptions.CommandNotFoundException;
import org.example.network.Request;
import org.example.network.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClientMain {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int RECONNECTION_DELAY_MS = 300;
    private static final int MAX_RECONNECTION_ATTEMPTS = 3;

    private final Console console = new StandartConsole();
    private final CommandManager commandManager = new CommandManager();
    private SocketChannel socket;
    private ObjectOutputStream writer;
    private ObjectInputStream reader;

    public static void main(String[] args) {
        new ClientMain().run();
    }

    private void initializeCommands() {
        commandManager.registerCommand(new Help(console, commandManager));
        commandManager.registerCommand(new Exit(console));
        commandManager.registerCommand(new ExecuteScript(console, commandManager));

        commandManager.registerCommand(new ServerCommand("info", "показывает инфо о коллекциях"));
        commandManager.registerCommand(new ServerCommand("show", "показывает все элементы коллекции"));
        commandManager.registerCommand(new ServerCommand("insert", "добавляет новый элемент"));
        commandManager.registerCommand(new ServerCommand("update", "обновляет элемент по айди"));
        commandManager.registerCommand(new ServerCommand("remove_key", "удаляет элемент по айди"));
        commandManager.registerCommand(new ServerCommand("clear", "очищает коллекцию"));
        commandManager.registerCommand(new ServerCommand("replace_if_greater", "замена если больше"));
        commandManager.registerCommand(new ServerCommand("remove_greater_key", "удаляет элементы с айди больше чем"));
        commandManager.registerCommand(new ServerCommand("remove_lower_key", "удаляет элементы с айди меньше чем"));
        commandManager.registerCommand(new ServerCommand("remove_all_by_standard_of_living",
                "удаляет все города с этими стандартами проживания"));
        commandManager.registerCommand(new ServerCommand("average_of_meters_above_sea_level",
                "считает среднее значение высоты над уровнем моря"));
        commandManager.registerCommand(new ServerCommand("filter_starts_with_name",
                "фильтрует элементы по названию"));
    }

    private class ServerCommand extends Command {
        public ServerCommand(String name, String description) {
            super(name, description);
        }

        @Override
        public void execute(String[] args) throws CommandExecutionError {
            try {
                Request request = new Request(this.getName(), args);
                Response response = sendRequest(request);
                console.writeln(response.getMessage());
                if (response.getData() != null) {
                    console.writeln(response.getData().toString());
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new CommandExecutionError("Error sending request to server: " + e.getMessage());
            }
        }
    }

    private Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        writer.writeObject(request);
        writer.flush();
        return (Response) reader.readObject();
    }

    private void connect() throws IOException {
        socket = SocketChannel.open(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
        writer = new ObjectOutputStream(socket.socket().getOutputStream());
        reader = new ObjectInputStream(socket.socket().getInputStream());
        console.writeln("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
    }

    private void disconnect() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
            socket = null;
            writer = null;
            reader = null;
            console.writeln("Disconnected from server.");
        } catch (IOException e) {
            console.writeln("Error closing connection: " + e.getMessage());
        }
    }

    public void run() {
        initializeCommands();

        try (Scanner scanner = new Scanner(System.in);
             Console console = this.console) {

            int attempts = 0;
            while (socket == null || !socket.isConnected()) {
                try {
                    connect();
                    attempts = 0;
                } catch (IOException e) {
                    attempts++;
                    console.writeln("Failed to connect to server (attempt " + attempts + "/" + MAX_RECONNECTION_ATTEMPTS + "): " + e.getMessage());
                    if (attempts >= MAX_RECONNECTION_ATTEMPTS) {
                        console.writeln("Failed to connect to server after " + MAX_RECONNECTION_ATTEMPTS + " attempts. Exiting.");
                        return;
                    }
                    try {
                        Thread.sleep(RECONNECTION_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        console.writeln("Connection interrupted.");
                        return;
                    }
                }
            }

            console.writeln("Type 'help' for command list");

            while (true) {
                try {
                    console.write("> ");
                    String input = console.read().trim();
                    if (input.isEmpty()) continue;

                    String[] parts = input.split("\\s+", 2);
                    String commandName = parts[0];
                    String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

                    Command command = commandManager.getCommand(commandName);
                    command.execute(args);

                    if ("exit".equalsIgnoreCase(commandName)) {
                        break;
                    }
                } catch (CommandNotFoundException e) {
                    console.writeln("Error: Command not found. Type 'help' for available commands");
                } catch (CommandExecutionError e) {
                    console.writeln("Error: " + e.getMessage());
                    if (e.getCause() instanceof IOException) {
                        console.writeln("Connection to server lost. Attempting to reconnect...");
                        disconnect();
                        attempts = 0;
                        while (socket == null || !socket.isConnected()) {
                            try {
                                connect();
                                attempts = 0;
                                break;
                            } catch (IOException reconnectException) {
                                attempts++;
                                console.writeln("Failed to reconnect (attempt " + attempts + "/" + MAX_RECONNECTION_ATTEMPTS + "): " + reconnectException.getMessage());
                                if (attempts >= MAX_RECONNECTION_ATTEMPTS) {
                                    console.writeln("Failed to reconnect after " + MAX_RECONNECTION_ATTEMPTS + " attempts. Exiting.");
                                    return;
                                }
                                try {
                                    Thread.sleep(RECONNECTION_DELAY_MS);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    console.writeln("Reconnection interrupted.");
                                    return;
                                }
                            }
                        }
                        if (socket != null && socket.isConnected()) {
                            console.writeln("Reconnected to server. Please re-enter the last command.");
                            continue;
                        } else {
                            console.writeln("Failed to reconnect. Closing client.");
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            console.writeln("Fatal error: " + e.getMessage());
        } finally {
            disconnect();
            console.writeln("Client stopped");
        }
    }
}