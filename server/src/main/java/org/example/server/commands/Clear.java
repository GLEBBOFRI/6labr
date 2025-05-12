package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public class Clear extends Command {
    private final CollectionManager collectionManager;

    public Clear(CollectionManager collectionManager) {
        super("clear", "очистить коллекцию");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            collectionManager.clearCollection();
            return new Response("Коллекция успешно очищена.");
        } catch (Exception e) {
            return new Response("Ошибка при очистке коллекции: " + e.getMessage());
        }
    }
}