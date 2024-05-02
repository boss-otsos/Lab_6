package client.commands;

import common.Request;
import client.console.Console;

public class Clear extends Command {
    private final Console console;
    public Clear(Console console) {
        super("clear", "очищает коллекцию");
        this.console = console;
    }

    public boolean execution(String[] args, Request request) {
        if (args.length != 1){
            console.printError("Неправильное количество аргументов");
            return false;
        }

        request.setCommandName("clear");

        return true;
    }
}