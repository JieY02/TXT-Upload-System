import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.stream.Collectors;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 9876;
    private static final String USAGE = "Usage:\n1. java Client list\n2. java Client put fname";

    /**
     * The main entry point of the client application.
     * Connects to the server, sends the specified command, and handles server responses.
     *
     * @param args The command-line arguments specifying the action to be performed.
     */
    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println(USAGE);
            System.exit(1); // Exit with error status code
        }

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())))
        {

            String commandStr = args[0];
            if ("list".equals(commandStr)) {
                listFiles(args, writer, reader);
            } else if ("put".equals(commandStr)) {
                putFile(args, writer, reader);
            } else {
                System.err.println(USAGE);
                System.exit(1);
            }


            System.exit(0);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Sends a 'list' command to the server and displays the list of files received.
     *
     * @param args   The command-line arguments passed to the client.
     * @param writer The PrintWriter used to send commands to the server.
     * @param reader The BufferedReader used to receive responses from the server.
     * @throws IOException If an I/O error occurs during communication with the server.
     */
    public static void listFiles(String[] args, PrintWriter writer, BufferedReader reader) throws IOException {
        if (args.length != 1) {
            System.err.println(USAGE);
            System.exit(1);
        }

        String commandStr = args[0];
        writer.println(commandStr);

        String response;
        while ((response = reader.readLine()) != null) {
            System.out.println(response);
        }
    }

    /**
     * Sends a 'put' command to the server along with the specified file's content,
     * and displays the success or error message received from the server.
     *
     * @param args   The command-line arguments passed to the client.
     * @param writer The PrintWriter used to send commands to the server.
     * @param reader The BufferedReader used to receive responses from the server.
     * @throws IOException If an I/O error occurs during communication with the server.
     */
    public static void putFile(String[] args, PrintWriter writer, BufferedReader reader) throws IOException {
        if (args.length != 2) {
            System.err.println(USAGE);
            System.exit(1);
        }

        String fileName = args[1];
        File file = new File(fileName);

        // File not exist.
        if (!file.exists()) {
            System.err.println(fileName + "is not exist.");
            System.exit(1);
        }

        String content = Files.lines(Paths.get(fileName)).collect(Collectors.joining("$$$"));
        String newCommand = args[0] + " " + args[1] + " " + content;
        writer.println(newCommand);

        String response;
        while ((response = reader.readLine()) != null) {
            if (response.startsWith("Error:")) {
                System.out.println(response);
                System.exit(1);
            } else if (response.equals("Success")) {
                System.out.println("Uploaded file " + fileName);
                return;
            }
            else {
                System.err.println(response);
            }
        }
    }
}