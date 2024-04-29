import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final String ERROR_LOG_FILE_NOT_CREATE = "Can't create a new log file.";

    private static final int PORT = 9876;

    public static void main(String[] args) throws IOException {
        logInit();

        ExecutorService executor = Executors.newFixedThreadPool(20);

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new FileWriter("received_file.txt"))) {


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logInit() {
        File logFile = new File("log.txt");

        if (logFile.exists()) {
            logFile.delete();
        }

        try {
            if (!logFile.createNewFile()) {
                System.err.println(ERROR_LOG_FILE_NOT_CREATE);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}