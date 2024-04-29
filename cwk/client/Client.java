import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.stream.Collectors;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 9876;
    private static final String USAGE = "Usage:\n1. java Client list\n2. java Client put fname";

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
        }
    }
}