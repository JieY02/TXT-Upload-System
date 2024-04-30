import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final String ERROR_LOG_FILE_NOT_CREATE = "Can't create a new log file.";

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd|HH:mm:ss");
    private static final int PORT = 9876;
    private static final int THREAD_POOL_SIZE = 20;
    private static ExecutorService threadPoolExecutor;

    /**
     * The main entry point of the server application.
     * Initializes the log file, thread pool, and starts listening for client connections.
     *
     * @param args The command-line arguments (not used).
     * @throws IOException If an I/O error occurs while initializing the server socket.
     */
    public static void main(String[] args) throws IOException {
        logInit();

        threadPoolExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            threadPoolExecutor.submit(() -> {
                try {
                    handleClient(clientSocket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * Handles an incoming client connection.
     * Reads the client's command, processes it, and logs the request.
     *
     * @param clientSocket The socket representing the client connection.
     */
    private static void handleClient(Socket clientSocket) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // 读取客户端发送的命令
            String command = reader.readLine();
            String[] commandParts = command.split(" ");

            // 处理不同的命令
            switch (commandParts[0])
            {
                case "list" -> listFiles(writer);
                case "put" ->
                {
                    if (commandParts.length < 2)
                    {
                        writer.write("Error: Invalid command format.");
                        writer.flush();
                        break;
                    }
                    String fileName = commandParts[1];
                    receiveFile(clientSocket, commandParts, fileName);
                }
                default ->
                {
                    writer.write("Error: Unknown command.");
                    writer.flush();
                }
            }

            // 记录日志
            logRequest(clientSocket.getInetAddress().getHostAddress(), commandParts[0]);

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

    /**
     * Lists files in the server directory and sends the list to the client.
     *
     * @param writer The buffered writer to write the file list to.
     * @throws IOException If an I/O error occurs while listing files or writing to the writer.
     */
    private static void listFiles(PrintWriter writer) throws IOException {
        File folder = new File("serverFiles");
        File[] files = folder.listFiles();
        List<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }
        }
        String content = "Listing " + fileNames.size() + " file(s):\n" + String.join("\n", fileNames);
        writer.write(content);
        System.out.println("Sent file list to client.");
    }

    /**
     * Receives a file from the client and saves it to the server directory.
     *
     * @param clientSocket The socket representing the client connection.
     * @param commandParts The file contents.
     * @param fileName     The name of the file to be received.
     * @throws IOException If an I/O error occurs while receiving or saving the file.
     */
    private static void receiveFile(Socket clientSocket, String[] commandParts, String fileName) throws IOException {
        File file = new File("serverFiles", fileName);
        if (file.exists()) {
            System.out.println("Received file '" + fileName + "' from client, but file already exists.");
            // 如果文件已存在，则返回错误信息
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
                writer.write("Error: Cannot upload file ’"+ fileName +"’; already exists on server.");
            }
            return;
        }

        if (commandParts.length > 2) {
            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 2; i < commandParts.length; i++) {
                contentBuilder.append(commandParts[i]);
                if (i < commandParts.length - 1) {
                    contentBuilder.append(" ");
                }
            }
            String content = contentBuilder.toString().replace("$@$", "\n");

            try (PrintWriter fileOut = new PrintWriter(file)) {
                fileOut.print(content);
            }
        } else {
            file.createNewFile();
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            writer.write("Success");
        }
    }

    /**
     * Logs a client request along with the client's IP address and timestamp.
     *
     * @param clientAddress The IP address of the client.
     * @param request       The request made by the client.
     */
    private static void logRequest(String clientAddress, String request) {
        try (PrintWriter logWriter = new PrintWriter(new FileWriter("log.txt", true))) {
            // 获取当前日期和时间
            String dateTime = LocalDateTime.now().format(formatter);

            // 记录请求到日志文件
            logWriter.write(dateTime + "|" + clientAddress + "|" + request + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the log file by creating a new empty log file.
     * If a log file already exists, it is deleted and a new one is created.
     * Prints an error message if the log file cannot be created.
     */
    public static void logInit() {
        File logFile = new File("log.txt");

        if (logFile.exists()) {
            logFile.delete();
        }

        try {
            if (!logFile.createNewFile()) {
                System.err.println(ERROR_LOG_FILE_NOT_CREATE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}