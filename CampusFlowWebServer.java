package web;

import com.sun.net.httpserver.HttpServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.Executors;

/**
 * Mobile Web Server entry point for CampusFlow.
 * Pure Java embedded HTTP server binding to 0.0.0.0.
 */
public class CampusFlowWebServer {

    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        } else if (System.getenv("PORT") != null) {
            try {
                port = Integer.parseInt(System.getenv("PORT"));
            } catch (NumberFormatException ignored) {}
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            // API handler for all backend REST endpoints
            server.createContext("/api", new ApiHandler());

            // Static file handler for mobile HTML5 / CSS / PWA frontend
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();

            System.out.println("=================================================================");
            System.out.println("        CAMPUSFLOW MOBILE WEB APPLICATION SERVER STARTED        ");
            System.out.println("                   Your Campus. Connected.                       ");
            System.out.println("=================================================================");
            System.out.println("Server listening on all network interfaces (0.0.0.0:" + port + ")");
            System.out.println();
            System.out.println("Local Desktop URL:");
            System.out.println("  -> http://localhost:" + port + "/");
            System.out.println();
            System.out.println("Android Phone Access (Same Wi-Fi Network):");

            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;

                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr instanceof java.net.Inet4Address) {
                            System.out.println("  -> http://" + addr.getHostAddress() + ":" + port + "/");
                        }
                    }
                }
            } catch (Exception ignored) {}

            System.out.println();
            System.out.println("Default Demo Roles:");
            System.out.println("  - Admin:   username: admin        / password: Admin@123");
            System.out.println("  - Faculty: username: robert.vance / password: Faculty@123");
            System.out.println("  - Student: username: alex.chen    / password: Student@123");
            System.out.println("=================================================================");

        } catch (Exception e) {
            System.err.println("Fatal: Failed to start web server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
