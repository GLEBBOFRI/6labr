package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public class RemoveLowerKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveLowerKey(CollectionManager collectionManager) {
        super("remove_lower_key", "удалить из коллекции все элементы, ключ которых меньше, чем заданный");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String arg = (String) request.getArguments();
        if (arg == null || arg.isEmpty()) {
            return new Response("Не указан ключ.");
        }
        try {
            int key = Integer.parseInt(arg);
            int removedCount = collectionManager.removeLowerKey(key);
            return new Response("Удалено " + removedCount + " элементов с ключом, меньшим чем " + key + ".");
        } catch (NumberFormatException e) {
            return new Response("Неверный формат ключа. Ожидается целое число.");
        } catch (Exception e) {
            return new Response("Ошибка при выполнении команды 'remove_lower_key': " + e.getMessage());
        }
    }
}