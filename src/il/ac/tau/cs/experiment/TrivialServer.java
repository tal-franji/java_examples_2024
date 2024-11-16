package il.ac.tau.cs.experiment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TrivialServer {
    public static boolean handleOneClient(SocketChannel client) {
        var inBuf = ByteBuffer.allocate(1024);
        try {
            client.read(inBuf);
            if (inBuf.position() > 0) {
                System.out.println("read from client.");
                //String text = StandardCh
                // arsets.UTF_8.decode(inBuf).toString();
                int start = 0;
                int end = inBuf.position() - 1;
                String text = new String(inBuf.array(), start, end, StandardCharsets.UTF_8);
                //System.out.println("read from client:" + text);
                String html = "<html>\n<body>\n";
                html += "<H1>Trivial server</H1>\n";
                html += "<p>" + "client has sent" + "</p>\n";
                html += "<p><pre>" + text + "</pre></p>\n";
                html += "</body>\n</html>";
                int contentLength = html.length();
                String response = String.format("HTTP/1.1 200 OK\r\n"  +
                        "Content-Length: %d\r\n" +
                        "Connection: Closed\r\n" +
                        "Content-Type: text/html\r\n" +
                        "\r\n" + // empty line to finish HTTP header
                        "%s", contentLength,  html);
                System.out.println("response:" + response);
                client.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
                client.close();  // end HTTP response
            }
            return false;

        } catch (IOException e) {
            System.out.println("Client closed." + e.getMessage());
            return false;
        }
    }

    public static void runServer() {
        ServerSocketChannel acceptSocket = null;
        var acceptPort = 8088;
        var activeClients = new ArrayList<SocketChannel>();
        try {
            acceptSocket = ServerSocketChannel.open();
            acceptSocket.configureBlocking(false);  // non-blocking to allow loop to handle other things
            acceptSocket.socket().bind(new InetSocketAddress(acceptPort));
        } catch (IOException e) {
            System.out.printf("ERROR accepting at port %d%n", acceptPort);
            return;
        }
        while (true) {
            try {
                SocketChannel connectionSocket = acceptSocket.accept();
                if (connectionSocket != null) {
                    System.out.println("client added");
                    activeClients.add(connectionSocket);
                }
            } catch (IOException e) {
                System.out.printf("ERROR accept:\n  %s", e.toString());
            }
            for (int i = 0; i < activeClients.size(); i++) {
                var client = activeClients.get(i);
                var clientAlice = handleOneClient(client);
                if (!clientAlice) {
                    activeClients.remove(i);
                    i--;
                }
            }
        }

    }
    public static void main(String[] argv) {
        runServer();
    }
}
