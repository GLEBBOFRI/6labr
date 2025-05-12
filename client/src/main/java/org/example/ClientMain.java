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
        // Регистрация команд
        commandManager.registerCommand(new Help(console, commandManager));
        commandManager.registerCommand(new Exit(console));
        commandManager.registerCommand(new ExecuteScript(console, commandManager));

        // Серверные команды

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
            } catch (Exception e) {
                throw new CommandExecutionError("Command execution failed: " + e.getMessage());
            }
        }
    }

    private Response sendRequest(Request request) throws CommandExecutionError {
        for (int attempt = 0; attempt < MAX_RECONNECTION_ATTEMPTS; attempt++) {
            try {
                if (socket == null || !socket.isConnected()) {
                    connect();
                }

                writer.writeObject(request);
                writer.flush();
                Response response = (Response) reader.readObject();

                // Больше не проверяем response.isSuccess()
                console.writeln(response.getMessage());
                return response;
            } catch (IOException | ClassNotFoundException e) {
                if (attempt == MAX_RECONNECTION_ATTEMPTS - 1) {
                    throw new CommandExecutionError("Server unavailable after " +
                            MAX_RECONNECTION_ATTEMPTS + " attempts");
                }
                disconnect();
                try {
                    Thread.sleep(RECONNECTION_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CommandExecutionError("Connection interrupted");
                }
            }
        }
        throw new CommandExecutionError("Unable to connect to server");
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
        } catch (IOException e) {
            console.writeln("Error closing connection: " + e.getMessage());
        }
    }

    public void run() {
        initializeCommands();

        try (Scanner scanner = new Scanner(System.in);
             Console console = this.console) {

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