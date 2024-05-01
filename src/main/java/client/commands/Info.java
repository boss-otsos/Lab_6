package client.commands;

import client.console.Console;
import common.Request;

public class Info extends Command {
    private Console console;
    public Info(Console console){
        super("info", "выводит информацию о коллекции");
        this.console = console;
    }
    public boolean execution(String[] args, Request request){
        if (args.length != 1){
            console.printError("Неправильное количество аргументов");
            return false;
        }

        request.setCommandName("info");

        return true;
    }
}