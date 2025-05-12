package org.example.comands;

import org.example.comands.Command;
import org.example.consol.Console;
import org.example.exceptions.CommandExecutionError;

public class Exit extends Command {
    private final Console console;

    public Exit(Console console) {
        super("exit", "exit the application");
        this.console = console;
    }

    @Override
    public void execute(String[] args) throws CommandExecutionError {
        console.writeln("Closing application...");
        System.exit(0);
    }
}