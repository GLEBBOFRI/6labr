package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public class RemoveKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveKey(CollectionManager collectionManager) {
        super("remove_key", "удалить элемент из коллекции по его ключу");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String arg = (String) request.getArguments();
        if (arg == null || arg.isEmpty()) {
            return new Response("Не указан ключ для удаления.");
        }
        try {
            int key = Integer.parseInt(arg);
            if (collectionManager.remove(key)) {
                return new Response("Элемент с ключом " + key + " успешно удален.");
            } else {
                return new Response("Элемент с ключом " + key + " не найден.");
            }
        } catch (NumberFormatException e) {
            return new Response("Неверный формат ключа. Ожидается целое число.");
        } catch (Exception e) {
            return new Response("Ошибка при удалении элемента: " + e.getMessage());
        }
    }
}