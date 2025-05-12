package org.example.server.commands;

import org.example.collection.models.City;
import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import java.util.Collection;
import java.util.stream.Collectors;

public class Show extends Command {
    private final CollectionManager collectionManager;

    public Show(CollectionManager collectionManager) {
        super("show", "show all elements");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        Collection<City> cities = collectionManager.getSortedCollection();
        if (cities.isEmpty()) {
            return new Response("Коллекция пуста.");
        } else {
            String data = cities.stream().map(City::toString).collect(Collectors.joining("\n"));
            return new Response("Элементы коллекции:\n" + data);
        }
    }
}