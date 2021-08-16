
import java.net.*;
import java.io.*;
import java.util.*;

public class Client {

    private final String notif = " ";

    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final int port;

    // initializing port and server address
    Client(String server, int port) {
        this.server = server;
        this.port = port;
    }
    // start a new client to the specified server
    public boolean start() {
        try {
            socket = new Socket(server, port);
        } catch (IOException ec) {
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        new ListenFromServer().start();
        return true;
    }

    private void display(String msg) {

        System.out.println(msg);

    }
    // send message to the server to send to the specified processes
    void sendMessage(Message msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }
    // diconnet it when stopped
    private void disconnect() {
        try {
            if (sInput != null) {
                sInput.close();
            }
        } catch (IOException e) {
        }
        try {
            if (sOutput != null) {
                sOutput.close();
            }
        } catch (IOException e) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
        }

    }
    // main method to start new client and server
    public static void main(String[] args) throws IOException {
        int portNumber;
        String serverAddress;
        Client client;
        // prompt user to enter port and server address
        try (Scanner scan = new Scanner(System.in)) {
            System.out.print("Enter Port Number: ");
            portNumber = Integer.parseInt(scan.nextLine());
            System.out.print("Enter Server Address: ");
            serverAddress = scan.nextLine();
            client = new Client(serverAddress, portNumber);
            if (!client.start()) {
                Server server = new Server(portNumber);
                Thread t = new Thread(server);
                t.start();
                if(!client.start()){
                    System.out.println("Server does not exists");
                    System.exit(0);
                }
            }
            while (true) {
                // prompt user to enter stop or send command with message
                System.out.print("> ");
                String msg = scan.nextLine();
                if (msg.equalsIgnoreCase("STOP")) {
                    client.sendMessage(new Message(Message.STOP, ""));
                    break;
                } else if (msg.toUpperCase().contains("SEND")) {
                    if(msg.split(" ")[1].equals("0") || msg.split(" ")[1].equals("1") || msg.split(" ")[1].equals("2") || 
                            msg.split(" ")[1].equals("3") || msg.split(" ")[1].equals("4"))
                        client.sendMessage(new Message(Message.SEND, msg));
                    else
                        System.out.println("Invalid Command!!!");
                } else{
                    System.out.println("Invalid Command!!!");                    
                }
            }
        }
        client.disconnect();
    }
    // this thread will be run for each process to listen from the server.
    class ListenFromServer extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    // sInput.readObject will listen from the server and display it on the console
                    Message msg = (Message) sInput.readObject();
                    System.out.println(msg.getMessage());
                    System.out.print("> ");
                } catch (IOException e) {
                    display(notif + "Server has closed the connection: " + e + notif);
                    break;
                } catch (ClassNotFoundException e2) {
                }
            }
        }
    }

}
