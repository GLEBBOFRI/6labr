package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.*;
import org.example.collection.exceptions.ValidationException;

public class Insert extends Command {
    private final CollectionManager collectionManager;

    public Insert(CollectionManager collectionManager) {
        super("insert", "добавить новый элемент с автоматически сгенерированным ID");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        Object[] args = (Object[]) request.getArguments();
        if (args == null || args.length < 9 || args.length > 10) {
            return new Response("Неверное количество аргументов для команды 'insert'. Ожидается: название, координата X, координата Y, площадь, население, высота, код_климата, код_правительства, код_уровня_жизни [, имя_губернатора]");
        }

        try {
            String name = (String) args[0];
            Integer coordinateX = Integer.parseInt((String) args[1]);
            Long coordinateY = Long.parseLong((String) args[2]);
            Integer area = Integer.parseInt((String) args[3]);
            Long population = Long.parseLong((String) args[4]);
            Float metersAboveSeaLevel = Float.parseFloat((String) args[5]);

            int climateCode = Integer.parseInt((String) args[6]);
            if (climateCode < 1 || climateCode > Climate.values().length) {
                return new Response("Неверный код климата.");
            }
            Climate climate = Climate.values()[climateCode - 1];

            int governmentCode = Integer.parseInt((String) args[7]);
            if (governmentCode < 1 || governmentCode > Government.values().length) {
                return new Response("Неверный код правительства.");
            }
            Government government = Government.values()[governmentCode - 1];

            int standardOfLivingCode = Integer.parseInt((String) args[8]);
            if (standardOfLivingCode < 1 || standardOfLivingCode > StandardOfLiving.values().length) {
                return new Response("Неверный код уровня жизни.");
            }
            StandardOfLiving standardOfLiving = StandardOfLiving.values()[standardOfLivingCode - 1];

            Human governor = null;
            if (args.length == 10) {
                governor = new Human((String) args[9]);
            }

            City newCity = new City();
            newCity.setName(name);
            newCity.setCoordinates(new Coordinates(coordinateX, coordinateY));
            newCity.setArea(area);
            newCity.setPopulation(population);
            newCity.setMetersAboveSeaLevel(metersAboveSeaLevel);
            newCity.setClimate(climate);
            newCity.setGovernment(government);
            newCity.setStandardOfLiving(standardOfLiving);
            newCity.setGovernor(governor);

            newCity.validate();

            collectionManager.addElement(null, newCity); // Передаем null в качестве ключа, чтобы CollectionManager сгенерировал его

            return new Response("Город успешно добавлен с ID: " + newCity.getId());

        } catch (NumberFormatException e) {
            return new Response("Неверный формат числового аргумента.");
        } catch (IllegalArgumentException e) {
            return new Response("Неверный формат аргумента.");
        } catch (ValidationException e) {
            return new Response("Ошибка валидации города: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return new Response("Неверный код для климата, правительства или уровня жизни.");
        } catch (Exception e) {
            return new Response("Ошибка при выполнении команды 'insert': " + e.getMessage());
        }
    }
}