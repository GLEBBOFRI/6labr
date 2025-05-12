package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.City;
import org.example.collection.exceptions.ValidationException;

public class ReplaceIfGreater extends Command {
    private final CollectionManager collectionManager;

    public ReplaceIfGreater(CollectionManager collectionManager) {
        super("replace_if_greater", "заменить, если больше");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length < 2) {
                return new Response("Неверное количество аргументов для команды 'replace_if_greater'. Ожидается: key City");
            }

            Integer key;
            try {
                key = Integer.parseInt((String) args[0]);
            } catch (NumberFormatException e) {
                return new Response("Неверный формат ключа. Ожидается целое число.");
            }

            City newCity;
            try {
                newCity = (City) args[1];
            } catch (ClassCastException e) {
                return new Response("Неверный формат объекта City.");
            }

            newCity.validate();
            if (!collectionManager.containsKey(key)) {
                return new Response("Элемента с ключом " + key + " не существует.");
            }
            if (collectionManager.replaceIfGreater(key, newCity)) {
                return new Response("Элемент с ключом " + key + " успешно заменен, так как новый город больше.");
            } else {
                return new Response("Элемент с ключом " + key + " не был заменен, так как новый город не больше.");
            }

        } catch (ValidationException e) {
            return new Response("Ошибка валидации города: " + e.getMessage());
        } catch (ClassCastException e) {
            return new Response("Неверный формат аргументов для команды 'replace_if_greater'. Ожидается: key City");
        } catch (Exception e) {
            return new Response("Ошибка при выполнении команды 'replace_if_greater': " + e.getMessage());
        }
    }
}