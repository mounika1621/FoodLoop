package com.foodwaste.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthServlet extends HttpServlet {

    private static final String DB = "jdbc:sqlite:foodloop.db";

    // ============================================================
    // DATABASE
    // ============================================================

    static Connection db() throws SQLException {
        return DriverManager.getConnection(DB);
    }

    // ============================================================
    // JSON RESPONSE
    // ============================================================

    static void json(
            HttpServletResponse response,
            String body,
            int status
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(body);
    }

    // ============================================================
    // JSON ESCAPE
    // ============================================================

    static String esc(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    // ============================================================
    // REQUEST PARAMETER
    // ============================================================

    static String p(
            HttpServletRequest request,
            String name
    ) {

        String value = request.getParameter(name);

        return value == null ? "" : value.trim();
    }

    // ============================================================
    // POST
    // ============================================================

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

        String path = req.getPathInfo();

        /*
         * Support both:
         *
         * /api/auth/login
         * /api/auth/register
         * /api/auth/logout
         *
         * AND:
         *
         * /api/auth?action=login
         * /api/auth?action=register
         * /api/auth?action=logout
         */

        String action = p(req, "action");

        if (path == null || path.isBlank() || "/".equals(path)) {

            if (!action.isBlank()) {
                path = "/" + action;
            }
        }

        try (Connection c = db()) {

            // ----------------------------------------------------
            // LOGIN
            // ----------------------------------------------------

            if ("/login".equalsIgnoreCase(path)) {

                login(req, resp, c);
                return;
            }

            // ----------------------------------------------------
            // REGISTER
            // ----------------------------------------------------

            if ("/register".equalsIgnoreCase(path)) {

                register(req, resp, c);
                return;
            }

            // ----------------------------------------------------
            // LOGOUT
            // ----------------------------------------------------

            if ("/logout".equalsIgnoreCase(path)) {

                HttpSession session = req.getSession(false);

                if (session != null) {
                    session.invalidate();
                }

                json(
                        resp,
                        "{\"ok\":true,\"message\":\"Logged out successfully.\"}",
                        200
                );

                return;
            }

            // ----------------------------------------------------
            // UNKNOWN
            // ----------------------------------------------------

            json(
                    resp,
                    "{\"ok\":false,\"message\":\"Unknown auth action\"}",
                    404
            );

        } catch (Exception e) {

            e.printStackTrace();

            json(
                    resp,
                    "{\"ok\":false,\"message\":\""
                            + esc(e.getMessage())
                            + "\"}",
                    500
            );
        }
    }

    // ============================================================
    // GET
    // ============================================================

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

        String path = req.getPathInfo();

        // --------------------------------------------------------
        // CURRENT USER
        // --------------------------------------------------------

        if ("/me".equalsIgnoreCase(path)) {

            HttpSession session = req.getSession(false);

            if (
                    session == null
                    || session.getAttribute("userId") == null
            ) {

                json(
                        resp,
                        "{\"ok\":true,\"authenticated\":false}",
                        200
                );

                return;
            }

            Object userId = session.getAttribute("userId");
            Object userName = session.getAttribute("userName");
            Object email = session.getAttribute("email");
            Object role = session.getAttribute("role");

            String response =
                    "{"
                    + "\"ok\":true,"
                    + "\"authenticated\":true,"
                    + "\"user\":{"
                    + "\"id\":" + userId + ","
                    + "\"name\":\"" + esc(
                            userName == null
                                    ? ""
                                    : userName.toString()
                    ) + "\","
                    + "\"email\":\"" + esc(
                            email == null
                                    ? ""
                                    : email.toString()
                    ) + "\","
                    + "\"role\":\"" + esc(
                            role == null
                                    ? ""
                                    : role.toString()
                    ) + "\""
                    + "}"
                    + "}";

            json(resp, response, 200);

            return;
        }

        json(
                resp,
                "{\"ok\":false,\"message\":\"Unknown auth endpoint\"}",
                404
        );
    }

    // ============================================================
    // LOGIN
    // ============================================================

    void login(
            HttpServletRequest request,
            HttpServletResponse response,
            Connection connection
    ) throws Exception {

        String email = p(request, "email");
        String password = p(request, "password");
        String role = p(request, "role");

        if (email.isBlank() || password.isBlank()) {

            json(
                    response,
                    "{\"ok\":false,\"message\":\"Email and password are required.\"}",
                    400
            );

            return;
        }

        String sql =
                "SELECT id, name, email, password, role "
                + "FROM users "
                + "WHERE lower(email) = lower(?)";

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {

                    json(
                            response,
                            "{\"ok\":false,\"message\":\"Invalid email or password.\"}",
                            401
                    );

                    return;
                }

                String storedPassword = rs.getString("password");

                if (
                        storedPassword == null
                        || storedPassword.isBlank()
                        || !BCrypt.checkpw(password, storedPassword)
                ) {

                    json(
                            response,
                            "{\"ok\":false,\"message\":\"Invalid email or password.\"}",
                            401
                    );

                    return;
                }

                String dbRole = rs.getString("role");

                // ------------------------------------------------
                // ROLE CHECK
                // ------------------------------------------------

                if (
                        !role.isBlank()
                        && !role.equalsIgnoreCase(dbRole)
                ) {

                    json(
                            response,
                            "{\"ok\":false,\"message\":\"Selected role does not match this account.\"}",
                            403
                    );

                    return;
                }

                // ------------------------------------------------
                // CREATE SESSION
                // ------------------------------------------------

                HttpSession session = request.getSession(true);

                session.setMaxInactiveInterval(30 * 60);

                session.setAttribute(
                        "userId",
                        rs.getInt("id")
                );

                session.setAttribute(
                        "userName",
                        rs.getString("name")
                );

                session.setAttribute(
                        "email",
                        rs.getString("email")
                );

                session.setAttribute(
                        "role",
                        dbRole
                );

                // ------------------------------------------------
                // REMEMBER EMAIL
                // ------------------------------------------------

                if (
                        "true".equalsIgnoreCase(
                                p(request, "remember")
                        )
                ) {

                    Cookie cookie =
                            new Cookie(
                                    "fw_remember_email",
                                    URLEncoder.encode(
                                            email,
                                            StandardCharsets.UTF_8
                                    )
                            );

                    cookie.setMaxAge(
                            60 * 60 * 24 * 30
                    );

                    cookie.setHttpOnly(false);

                    response.addCookie(cookie);
                }

                // ------------------------------------------------
                // SUCCESS
                // ------------------------------------------------

                json(
                        response,
                        "{"
                        + "\"ok\":true,"
                        + "\"authenticated\":true,"
                        + "\"message\":\"Login successful.\","
                        + "\"role\":\""
                        + esc(dbRole)
                        + "\""
                        + "}",
                        200
                );
            }
        }
    }

    // ============================================================
    // REGISTER
    // ============================================================

    void register(
            HttpServletRequest request,
            HttpServletResponse response,
            Connection connection
    ) throws Exception {

        String name = p(request, "name");
        String email = p(request, "email");
        String password = p(request, "password");

        String role =
                p(request, "role").toUpperCase();

        String phone = p(request, "phone");
        String location = p(request, "location");
        String organization = p(request, "organization");

        // --------------------------------------------------------
        // VALIDATION
        // --------------------------------------------------------

        if (
                name.isBlank()
                || email.isBlank()
                || password.length() < 6
        ) {

            json(
                    response,
                    "{\"ok\":false,\"message\":\"Name, email and a 6+ character password are required.\"}",
                    400
            );

            return;
        }

        // --------------------------------------------------------
        // VALID ROLE
        // --------------------------------------------------------

        if (
                !role.matches(
                        "DONOR|RECIPIENT|VOLUNTEER|ADMIN"
                )
        ) {

            role = "RECIPIENT";
        }

        // --------------------------------------------------------
        // CHECK EXISTING EMAIL
        // --------------------------------------------------------

        String checkSql =
                "SELECT id FROM users "
                + "WHERE lower(email) = lower(?)";

        try (
                PreparedStatement check =
                        connection.prepareStatement(checkSql)
        ) {

            check.setString(1, email);

            try (ResultSet rs = check.executeQuery()) {

                if (rs.next()) {

                    json(
                            response,
                            "{\"ok\":false,\"message\":\"An account with this email already exists.\"}",
                            409
                    );

                    return;
                }
            }
        }

        // --------------------------------------------------------
        // PASSWORD HASH
        // --------------------------------------------------------

        String hashedPassword =
                BCrypt.hashpw(
                        password,
                        BCrypt.gensalt(10)
                );

        // --------------------------------------------------------
        // INSERT USER
        // --------------------------------------------------------

        String sql =
                "INSERT INTO users "
                + "(name,email,password,role,phone,location,organization,availability,vehicle) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";

        try (
                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashedPassword);
            ps.setString(4, role);
            ps.setString(5, phone);
            ps.setString(6, location);
            ps.setString(7, organization);

            ps.setString(
                    8,
                    role.equals("VOLUNTEER")
                            ? "AVAILABLE"
                            : null
            );

            ps.setString(9, "");

            ps.executeUpdate();
        }

        // --------------------------------------------------------
        // SUCCESS
        // --------------------------------------------------------

        json(
                response,
                "{\"ok\":true,\"message\":\"Account created successfully.\"}",
                200
        );
    }
}