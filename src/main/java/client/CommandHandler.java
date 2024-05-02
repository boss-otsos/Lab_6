package client;

import client.console.Console;
import common.Request;

import java.io.*;
import java.net.*;

import java.util.*;

public class CommandHandler {

    public static class ServerException extends Exception{}
    public static class ScriptException extends Exception {}
    private final Console console;
    private final CommandProcessor commandProcessor;
    private final Stack<Scanner> scannerStack = new Stack<>();
    private final TreeSet<String> fileNames = new TreeSet<>();
    private int z = 0;


    public CommandHandler(Console console, CommandProcessor commandProcessor) {
        this.console = console;
        this.commandProcessor = commandProcessor;
    }

    public void manualMode() throws ServerException{
        String[] line = {"", ""};
        do{
            console.print("Введите команду: ");
            line = console.readln().trim().split(" ");
            if(line[0].equals("execute_script")) {
                try {
                    script(line[1]);
                } catch (NoSuchElementException e) {
                    console.printError("Скрипт содержит некорректные данные!!!!");
                } catch (ScriptException e) {
                    console.printError("Скрипт зацикливается!!!");
                } catch (FileNotFoundException e) {
                    console.printError("Файл не найден!!!");
                }
            } else if (line[0].equals("exit"))
                break;
            try{
                executionCommand(line);
            }catch (IOException ignored){} catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }while(!line[0].equals("exit"));
    }

    private void script(String fileName) throws FileNotFoundException, ScriptException, ServerException {
        FileReader fileReader = new FileReader(fileName);
        Scanner scanner = new Scanner(fileReader);
        scannerStack.add(scanner);
        if (fileNames.contains(fileName)) {
            scannerStack.pop();
            if (scannerStack.isEmpty())
                console.setFileScanner(null);
            else
                console.setFileScanner(scannerStack.peek());
            throw new ScriptException();
        }
        console.setFileScanner(scannerStack.peek());
        fileNames.add(fileName);
        try {
            while (scanner.hasNext()) {
                String[] line = scanner.nextLine().trim().split(" ");
                try {
                    executionCommand(line);
                }catch (IOException ignored){} catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                if (line[0].equals("execute_script"))
                    script(line[1]);
            }
        } catch (NoSuchElementException e) {
            scannerStack.pop();
            if (scannerStack.isEmpty())
                console.setFileScanner(null);
            else
                console.setFileScanner(scannerStack.peek());
            throw new NoSuchElementException();
        }catch (ScriptException e){
            scannerStack.pop();
            if (scannerStack.isEmpty())
                console.setFileScanner(null);
            else
                console.setFileScanner(scannerStack.peek());
            throw new ScriptException();
        }
    }

    private void executionCommand(String[] c) throws IOException, ClassNotFoundException, ServerException {
        if (c[0].isEmpty())
            return;
        var command = commandProcessor.getCommands().get(c[0]);
        if (command == null) {
            console.printError("Команда " + c[0] + " не найдена, введите help, чтобы узнать доступные команды");
            return;
        }

        Request request = new Request();

        if (command.execution(c, request)) {

            if(request.getCommandName().equals("help") || request.getCommandName().equals("history"))
                return;

            DatagramSocket clientSocket = new DatagramSocket();

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(request);
            byte[] data = byteStream.toByteArray();

            InetAddress IPAddress = InetAddress.getByName("localhost");
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, 9999);
            clientSocket.send(sendPacket);

            data = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);

            clientSocket.setSoTimeout(3000);
            try {
                clientSocket.receive(receivePacket);
                console.println(new String(receivePacket.getData()).trim());

                clientSocket.close();

                commandProcessor.addHistory(c[0]);
                z -= 1;
            }catch (SocketTimeoutException e){
                console.printError("Слишком долгое ожидание ответа от сервера");
            }catch (IOException e){
                console.printError("Ошибка сетевого подключения!!!");
            }
            z += 1;
            if(z > 5){
                console.printError("Не удалось восстановить подключение к серверу...");
                throw new ServerException();
            }
        }
    }
}