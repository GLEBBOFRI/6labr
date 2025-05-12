package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.*;
import org.example.collection.exceptions.ValidationException;

public class Update extends Command {
    private final CollectionManager collectionManager;

    public Update(CollectionManager collectionManager) {
        super("update", "обновить элемент по заданному ключу, получая все новые данные в одной строке");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        Object[] args = (Object[]) request.getArguments();
        if (args == null || args.length < 10 || args.length > 11) {
            return new Response("Неверное количество аргументов для команды 'update'. Ожидается: ключ, название, координата X, координата Y, площадь, население, высота, код_климата, код_правительства, код_уровня_жизни [, имя_губернатора]");
        }

        try {
            Integer key;
            try {
                key = Integer.parseInt((String) args[0]);
            } catch (NumberFormatException e) {
                return new Response("Неверный формат ключа. Ожидается целое число.");
            }

            String name = (String) args[1];
            Integer coordinateX = Integer.parseInt((String) args[2]);
            Long coordinateY = Long.parseLong((String) args[3]);
            Integer area = Integer.parseInt((String) args[4]);
            Long population = Long.parseLong((String) args[5]);
            Float metersAboveSeaLevel = Float.parseFloat((String) args[6]);

            int climateCode = Integer.parseInt((String) args[7]);
            if (climateCode < 1 || climateCode > Climate.values().length) {
                return new Response("Неверный код климата.");
            }
            Climate climate = Climate.values()[climateCode - 1];

            int governmentCode = Integer.parseInt((String) args[8]);
            if (governmentCode < 1 || governmentCode > Government.values().length) {
                return new Response("Неверный код правительства.");
            }
            Government government = Government.values()[governmentCode - 1];

            int standardOfLivingCode = Integer.parseInt((String) args[9]);
            if (standardOfLivingCode < 1 || standardOfLivingCode > StandardOfLiving.values().length) {
                return new Response("Неверный код уровня жизни.");
            }
            StandardOfLiving standardOfLiving = StandardOfLiving.values()[standardOfLivingCode - 1];

            Human governor = null;
            if (args.length == 11) {
                governor = new Human((String) args[10]);
            }

            City updatedCity = new City();
            updatedCity.setId((Integer) key); // Устанавливаем ID обновляемого элемента равным ключу
            updatedCity.setName(name);
            updatedCity.setCoordinates(new Coordinates(coordinateX, coordinateY));
            updatedCity.setArea(area);
            updatedCity.setPopulation(population);
            updatedCity.setMetersAboveSeaLevel(metersAboveSeaLevel);
            updatedCity.setClimate(climate);
            updatedCity.setGovernment(government);
            updatedCity.setStandardOfLiving(standardOfLiving);
            updatedCity.setGovernor(governor);

            updatedCity.validate();

            if (!collectionManager.containsKey(key)) {
                return new Response("Элемента с ключом " + key + " не существует.");
            }

            if (collectionManager.update(key, updatedCity)) {
                return new Response("Элемент с ключом " + key + " успешно обновлен.");
            } else {
                return new Response("Не удалось обновить элемент с ключом " + key + ".");
            }

        } catch (NumberFormatException e) {
            return new Response("Неверный формат числового аргумента.");
        } catch (IllegalArgumentException e) {
            return new Response("Неверный формат аргумента.");
        } catch (ValidationException e) {
            return new Response("Ошибка валидации города: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return new Response("Неверный код для климата, правительства или уровня жизни.");
        } catch (Exception e) {
            return new Response("Ошибка при выполнении команды 'update': " + e.getMessage());
        }
    }
}