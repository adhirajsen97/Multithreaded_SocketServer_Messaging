
import java.io.*;
import java.net.*;
import java.util.*;

public class Server implements Runnable{

    private static int uniqueId = 0;
    private final ArrayList<ClientThread> al; // list of all four client from 1 to 4
    private final int port; // port number of the socket address
    private boolean keepGoing;  // it will be keep going until all the peers disconnected
    private final String notif = " ";
    private Socket socket;
    // initializing port and client threads
    public Server(int port) {
        this.port = port;
        al = new ArrayList<>();
    }
    // it will start server and make it keep going to listen for all peers
    public void start() {
        keepGoing = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (keepGoing) {

                Socket socket = serverSocket.accept();
                if (!keepGoing) {
                    break;
                }
                // creating a new client thread for each process
                ClientThread t = new ClientThread(socket);
                al.add(t);
                t.start();
            }
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                    }
                }
            } catch (IOException e) {
            }
        } catch (IOException e) {
        }
    }
    // it will stop the server
    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        } catch (IOException ex) {
        }
    }
    // send private and public messages
    private synchronized boolean sendMessage(int userId, String message) {
        String[] w = message.split(" ", 3);
        boolean isPrivate = false;
        int id = Integer.parseInt(w[1]);
        if (id != 0) {  // check if the id is 0 then message is public otherwise private
            isPrivate = true;
        }
        String messageLf = userId + ": " + w[2] + "\n";
        if (isPrivate == true) {
            boolean found = false;
            for (int y = al.size(); --y >= 0;) {
                ClientThread ct1 = al.get(y);
                int check = ct1.id;
                if(check!=userId)   // check for own id because a process cannot send message to itself
                    if (check == id) {  // sending private message to particular process
                        if (!ct1.writeMsg(messageLf)) {
                            al.remove(y);
                        }
                        found = true;
                        break;
                    }

            }
            if (found != true) {
                return false;
            }
        } else {
            for (int i = al.size(); --i >= 0;) {
                ClientThread ct = al.get(i);
                if(ct.id!=userId)   // sending messages to all process except itself
                    if (!ct.writeMsg(messageLf)) {
                        al.remove(i);
                    }
            }
        }
        return true;

    }
    // removing process from process list on stop command
    synchronized void remove(int id) {
        boolean disconnectedClient = false;
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            if (ct.id == id) {
                disconnectedClient = true;
                al.remove(i);
                break;
            }
        }
        if(al.isEmpty()){
            stop();
        }
        // broadcast a disconnect message when stopped
        if(disconnectedClient)
            sendMessage(id,  "send 0 Stopped its state");
    }

    @Override
    public void run() {
        start();
    }
    
    // client threads for each of the process
    class ClientThread extends Thread {

        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        Message cm;

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
            }
        }

        @Override
        public void run() {
            boolean keepGoing = true;
            // will listen for the message and send it privately or publicly
            while (keepGoing) {
                try {
                    // wait for the upcoming messages
                    cm = (Message) sInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    break;
                }
                String message = cm.getMessage();

                switch (cm.getType()) {
                    // if the message type is send
                    case Message.SEND:
                        boolean confirmation = sendMessage(id, message);
                        if (confirmation == false) {
                            String msg = notif + "Sorry. No such user exists." + notif;
                            writeMsg(msg);
                        }
                        break;
                    // if the message type is stop
                    case Message.STOP:
                        remove(id);
                        keepGoing = false;
                        break;
                }
            }
            // remove it and close its state when keep going = false
            remove(id);
            close();
        }

        private void close() {
            try {
                if (sOutput != null) {
                    sOutput.close();
                }
            } catch (IOException e) {
            }
            try {
                if (sInput != null) {
                    sInput.close();
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

        private boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(new Message(Message.SEND, msg));
            } catch (IOException e) {
            }
            return true;
        }
    }
}
