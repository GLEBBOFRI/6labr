package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.City;
import java.util.List;
import java.util.stream.Collectors;

public class FilterStartsWithName extends Command {
    private final CollectionManager collectionManager;

    public FilterStartsWithName(CollectionManager collectionManager) {
        super("filter_starts_with_name", "вывести элементы, значение поля name которых начинается с заданной подстроки");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String prefix = (String) request.getArguments();
        if (prefix == null || prefix.length() != 1) {
            return new Response("Неверное количество аргументов для команды 'filter_starts_with_name'. Ожидается один аргумент: префикс.");
        }
        List<City> filteredCities = collectionManager.filterStartsWithName(prefix);
        if (filteredCities.isEmpty()) {
            return new Response("Нет городов, название которых начинается с '" + prefix + "'.");
        } else {
            String data = filteredCities.stream().map(City::toString).collect(Collectors.joining("\n"));
            return new Response("Города, название которых начинается с '" + prefix + "':\n" + data, filteredCities);
        }
    }
}