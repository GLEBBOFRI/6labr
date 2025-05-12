package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public class Info extends Command {
    private final CollectionManager collectionManager;

    public Info(CollectionManager collectionManager) {
        super("info", "show collection info");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        return new Response(collectionManager.getCollectionInfo());
    }
}