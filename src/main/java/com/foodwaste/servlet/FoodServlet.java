package com.foodwaste.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

@WebServlet("/FoodServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10 * 1024 * 1024,
        maxRequestSize = 20 * 1024 * 1024
)
public class FoodServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB =
            "jdbc:sqlite:foodloop.db";


    // =========================================================
    // DATABASE CONNECTION
    // =========================================================

    private static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(DB);
    }


    // =========================================================
    // DATABASE INITIALIZATION
    // =========================================================

    public static void initDatabase()
            throws SQLException {

        try (Connection c = getConnection();
             Statement s = c.createStatement()) {

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT," +
                            "email TEXT UNIQUE," +
                            "password TEXT," +
                            "role TEXT," +
                            "phone TEXT," +
                            "location TEXT," +
                            "organization TEXT," +
                            "availability TEXT," +
                            "vehicle TEXT," +
                            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS food (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "donor_id INTEGER," +
                            "food_name TEXT," +
                            "category TEXT," +
                            "quantity TEXT," +
                            "source_type TEXT," +
                            "description TEXT," +
                            "location TEXT," +
                            "expiry_date TEXT," +
                            "image TEXT," +
                            "status TEXT DEFAULT 'AVAILABLE'," +
                            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS claims (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "food_id INTEGER," +
                            "recipient_id INTEGER," +
                            "status TEXT DEFAULT 'PENDING'," +
                            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS deliveries (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "food_id INTEGER," +
                            "claim_id INTEGER," +
                            "recipient_id INTEGER," +
                            "volunteer_id INTEGER," +
                            "pickup_location TEXT," +
                            "drop_location TEXT," +
                            "distance_km REAL DEFAULT 0," +
                            "rate_per_km REAL DEFAULT 0," +
                            "volunteer_fee REAL DEFAULT 0," +
                            "status TEXT DEFAULT 'ASSIGNED'," +
                            "payment_status TEXT DEFAULT 'PENDING'," +
                            "assigned_at TEXT," +
                            "picked_up_at TEXT," +
                            "delivered_at TEXT," +
                            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );
        }

        // =====================================================
        // MIGRATION
        // =====================================================

        addColumnIfMissing(
                "users",
                "phone",
                "TEXT"
        );

        addColumnIfMissing(
                "users",
                "location",
                "TEXT"
        );

        addColumnIfMissing(
                "users",
                "organization",
                "TEXT"
        );

        addColumnIfMissing(
                "users",
                "availability",
                "TEXT"
        );

        addColumnIfMissing(
                "users",
                "vehicle",
                "TEXT"
        );

        addColumnIfMissing(
                "users",
                "created_at",
                "TEXT"
        );

        addColumnIfMissing(
                "food",
                "source_type",
                "TEXT"
        );

        addColumnIfMissing(
                "food",
                "description",
                "TEXT"
        );

        addColumnIfMissing(
                "food",
                "expiry_date",
                "TEXT"
        );

        addColumnIfMissing(
                "food",
                "image",
                "TEXT"
        );

        addColumnIfMissing(
                "food",
                "status",
                "TEXT"
        );

        addColumnIfMissing(
                "food",
                "created_at",
                "TEXT"
        );

        addColumnIfMissing(
                "claims",
                "status",
                "TEXT"
        );

        addColumnIfMissing(
                "claims",
                "created_at",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "claim_id",
                "INTEGER"
        );

        addColumnIfMissing(
                "deliveries",
                "recipient_id",
                "INTEGER"
        );

        addColumnIfMissing(
                "deliveries",
                "volunteer_id",
                "INTEGER"
        );

        addColumnIfMissing(
                "deliveries",
                "pickup_location",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "drop_location",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "distance_km",
                "REAL"
        );

        addColumnIfMissing(
                "deliveries",
                "rate_per_km",
                "REAL"
        );

        addColumnIfMissing(
                "deliveries",
                "volunteer_fee",
                "REAL"
        );

        addColumnIfMissing(
                "deliveries",
                "status",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "payment_status",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "assigned_at",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "picked_up_at",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "delivered_at",
                "TEXT"
        );

        addColumnIfMissing(
                "deliveries",
                "created_at",
                "TEXT"
        );

        // =====================================================
        // FILL NULL VALUES FOR OLD RECORDS
        // =====================================================

        try (Connection c = getConnection();
             Statement s = c.createStatement()) {

            s.executeUpdate(
                    "UPDATE food SET status='AVAILABLE' " +
                            "WHERE status IS NULL OR status=''"
            );

            s.executeUpdate(
                    "UPDATE claims SET status='PENDING' " +
                            "WHERE status IS NULL OR status=''"
            );

            s.executeUpdate(
                    "UPDATE users SET availability='AVAILABLE' " +
                            "WHERE role='VOLUNTEER' " +
                            "AND (availability IS NULL OR availability='')"
            );
        }
    }


    // =========================================================
    // ADD COLUMN IF MISSING
    // =========================================================

    private static void addColumnIfMissing(
            String table,
            String column,
            String definition
    ) throws SQLException {

        try (Connection c = getConnection();
             Statement s = c.createStatement();
             ResultSet rs =
                     s.executeQuery(
                             "PRAGMA table_info(" + table + ")"
                     )) {

            while (rs.next()) {

                String existing =
                        rs.getString("name");

                if (
                        existing != null &&
                        existing.equalsIgnoreCase(column)
                ) {
                    return;
                }
            }
        }

        try (Connection c = getConnection();
             Statement s = c.createStatement()) {

            s.executeUpdate(
                    "ALTER TABLE " +
                            table +
                            " ADD COLUMN " +
                            column +
                            " " +
                            definition
            );
        }
    }


    // =========================================================
    // INIT
    // =========================================================

    @Override
    public void init()
            throws ServletException {

        try {

            initDatabase();

        } catch (SQLException e) {

            throw new ServletException(
                    "FoodLoop database initialization failed.",
                    e
            );
        }
    }


    // =========================================================
    // JSON RESPONSE
    // =========================================================

    private void json(
            HttpServletResponse response,
            JSONObject object
    ) throws IOException {

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response.getWriter().print(
                object.toString()
        );
    }


    private void json(
            HttpServletResponse response,
            JSONArray array
    ) throws IOException {

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response.getWriter().print(
                array.toString()
        );
    }


    // =========================================================
    // SESSION
    // =========================================================

    private int userId(
            HttpServletRequest request
    ) {

        /*
         * First check the authentication values captured
         * by doPost() before multipart processing.
         */
        Object requestUserId =
                request.getAttribute("foodloopUserId");

        if (requestUserId != null) {

            try {

                if (requestUserId instanceof Number) {

                    return ((Number) requestUserId).intValue();

                }

                return Integer.parseInt(
                        requestUserId.toString()
                );

            } catch (NumberFormatException ignored) {
            }
        }

        /*
         * Normal session lookup.
         */
        HttpSession session =
                request.getSession(false);

        if (
                session == null ||
                session.getAttribute("userId") == null
        ) {
            return -1;
        }

        Object value =
                session.getAttribute("userId");

        try {

            if (value instanceof Number) {

                return ((Number) value).intValue();
            }

            return Integer.parseInt(
                    value.toString()
            );

        } catch (NumberFormatException e) {

            return -1;
        }
    }


    private String userRole(
            HttpServletRequest request
    ) {

        /*
         * First check role captured by doPost().
         */
        Object requestRole =
                request.getAttribute("foodloopRole");

        if (requestRole != null) {

            return requestRole.toString();
        }

        /*
         * Normal session lookup.
         */
        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return "";
        }

        Object role =
                session.getAttribute("role");

        return role == null
                ? ""
                : role.toString();
    }


    // =========================================================
    // GET
    // =========================================================

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String mode =
                request.getParameter("mode");

        try {

            if ("available".equalsIgnoreCase(mode)) {

                getAvailable(response);

            } else if ("mine".equalsIgnoreCase(mode)) {

                getMine(
                        request,
                        response
                );

            } else if (
                    "claim-requests".equalsIgnoreCase(mode)
            ) {

                getClaimRequests(
                        request,
                        response
                );

            } else if (
                    "my-claims".equalsIgnoreCase(mode)
            ) {

                getMyClaims(
                        request,
                        response
                );

            } else if ("stats".equalsIgnoreCase(mode)) {

                getStats(
                        request,
                        response
                );

            } else if (
                    "volunteers".equalsIgnoreCase(mode)
            ) {

                getVolunteers(response);

            } else if ("users".equalsIgnoreCase(mode)) {

                getUsers(response);

            } else {

                JSONObject result =
                        new JSONObject();

                result.put(
                        "ok",
                        false
                );

                result.put(
                        "message",
                        "Unknown food mode."
                );

                json(
                        response,
                        result
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    e.getMessage() == null
                            ? "Server error."
                            : e.getMessage()
            );

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            json(
                    response,
                    result
            );
        }
    }


    // =========================================================
    // AVAILABLE FOOD
    // =========================================================

    private void getAvailable(
            HttpServletResponse response
    ) throws Exception {

        JSONArray foods =
                new JSONArray();

        String sql =
                "SELECT " +
                        "f.id, " +
                        "f.food_name, " +
                        "f.category, " +
                        "f.quantity, " +
                        "f.source_type, " +
                        "f.description, " +
                        "f.location, " +
                        "f.expiry_date, " +
                        "f.image, " +
                        "f.status, " +
                        "f.created_at, " +
                        "u.name AS donor_name " +
                        "FROM food f " +
                        "LEFT JOIN users u " +
                        "ON f.donor_id=u.id " +
                        "WHERE f.status='AVAILABLE' " +
                        "ORDER BY f.id DESC";

        try (Connection c = getConnection();
             PreparedStatement p =
                     c.prepareStatement(sql);
             ResultSet rs =
                     p.executeQuery()) {

            while (rs.next()) {

                JSONObject item =
                        new JSONObject();

                item.put(
                        "id",
                        rs.getInt("id")
                );

                item.put(
                        "foodName",
                        safeString(
                                rs.getString("food_name")
                        )
                );

                item.put(
                        "category",
                        safeString(
                                rs.getString("category")
                        )
                );

                item.put(
                        "quantity",
                        safeString(
                                rs.getString("quantity")
                        )
                );

                item.put(
                        "sourceType",
                        safeString(
                                rs.getString("source_type")
                        )
                );

                item.put(
                        "description",
                        safeString(
                                rs.getString("description")
                        )
                );

                item.put(
                        "location",
                        safeString(
                                rs.getString("location")
                        )
                );

                item.put(
                        "expiryDate",
                        safeString(
                                rs.getString("expiry_date")
                        )
                );

                item.put(
                        "image",
                        safeString(
                                rs.getString("image")
                        )
                );

                item.put(
                        "status",
                        safeString(
                                rs.getString("status")
                        )
                );

                item.put(
                        "donorName",
                        safeString(
                                rs.getString("donor_name")
                        )
                );

                foods.put(item);
            }
        }

        json(
                response,
                foods
        );
    }


    // =========================================================
    // DONOR FOOD
    // =========================================================

    private void getMine(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        int donorId =
                userId(request);

        JSONArray foods =
                new JSONArray();

        String sql =
                "SELECT " +
                        "f.id, " +
                        "f.food_name, " +
                        "f.category, " +
                        "f.quantity, " +
                        "f.source_type, " +
                        "f.description, " +
                        "f.location, " +
                        "f.expiry_date, " +
                        "f.image, " +
                        "f.status, " +
                        "f.created_at, " +
                        "(SELECT COUNT(*) " +
                        " FROM claims c " +
                        " WHERE c.food_id=f.id) " +
                        "AS claim_count " +
                        "FROM food f " +
                        "WHERE f.donor_id=? " +
                        "ORDER BY f.id DESC";

        try (Connection c = getConnection();
             PreparedStatement p =
                     c.prepareStatement(sql)) {

            p.setInt(
                    1,
                    donorId
            );

            try (ResultSet rs =
                         p.executeQuery()) {

                while (rs.next()) {

                    JSONObject item =
                            new JSONObject();

                    item.put(
                            "id",
                            rs.getInt("id")
                    );

                    item.put(
                            "foodName",
                            safeString(
                                    rs.getString("food_name")
                            )
                    );

                    item.put(
                            "category",
                            safeString(
                                    rs.getString("category")
                            )
                    );

                    item.put(
                            "quantity",
                            safeString(
                                    rs.getString("quantity")
                            )
                    );

                    item.put(
                            "sourceType",
                            safeString(
                                    rs.getString("source_type")
                            )
                    );

                    item.put(
                            "description",
                            safeString(
                                    rs.getString("description")
                            )
                    );

                    item.put(
                            "location",
                            safeString(
                                    rs.getString("location")
                            )
                    );

                    item.put(
                            "expiryDate",
                            safeString(
                                    rs.getString("expiry_date")
                            )
                    );

                    item.put(
                            "image",
                            safeString(
                                    rs.getString("image")
                            )
                    );

                    item.put(
                            "status",
                            safeString(
                                    rs.getString("status")
                            )
                    );

                    item.put(
                            "claimCount",
                            rs.getInt("claim_count")
                    );

                    foods.put(item);
                }
            }
        }

        json(
                response,
                foods
        );
    }


    // =========================================================
    // CLAIM REQUESTS
    // =========================================================

    private void getClaimRequests(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        int donorId =
                userId(request);

        JSONArray claims =
                new JSONArray();

        String sql =
                "SELECT " +
                        "c.id AS claim_id, " +
                        "c.food_id, " +
                        "c.recipient_id, " +
                        "c.status, " +
                        "f.food_name, " +
                        "f.quantity, " +
                        "f.category, " +
                        "f.location, " +
                        "u.name AS recipient_name, " +
                        "u.email AS recipient_email " +
                        "FROM claims c " +
                        "JOIN food f " +
                        "ON c.food_id=f.id " +
                        "JOIN users u " +
                        "ON c.recipient_id=u.id " +
                        "WHERE f.donor_id=? " +
                        "ORDER BY c.id DESC";

        try (Connection c = getConnection();
             PreparedStatement p =
                     c.prepareStatement(sql)) {

            p.setInt(
                    1,
                    donorId
            );

            try (ResultSet rs =
                         p.executeQuery()) {

                while (rs.next()) {

                    JSONObject item =
                            new JSONObject();

                    item.put(
                            "claimId",
                            rs.getInt("claim_id")
                    );

                    item.put(
                            "foodId",
                            rs.getInt("food_id")
                    );

                    item.put(
                            "recipientId",
                            rs.getInt("recipient_id")
                    );

                    item.put(
                            "status",
                            safeString(
                                    rs.getString("status")
                            )
                    );

                    item.put(
                            "foodName",
                            safeString(
                                    rs.getString("food_name")
                            )
                    );

                    item.put(
                            "quantity",
                            safeString(
                                    rs.getString("quantity")
                            )
                    );

                    item.put(
                            "category",
                            safeString(
                                    rs.getString("category")
                            )
                    );

                    item.put(
                            "location",
                            safeString(
                                    rs.getString("location")
                            )
                    );

                    item.put(
                            "recipientName",
                            safeString(
                                    rs.getString("recipient_name")
                            )
                    );

                    item.put(
                            "recipientEmail",
                            safeString(
                                    rs.getString("recipient_email")
                            )
                    );

                    claims.put(item);
                }
            }
        }

        json(
                response,
                claims
        );
    }


    // =========================================================
    // RECIPIENT CLAIMS
    // =========================================================

    private void getMyClaims(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        int recipientId =
                userId(request);

        JSONArray claims =
                new JSONArray();

        String sql =
                "SELECT " +
                        "c.id AS claim_id, " +
                        "c.status, " +
                        "f.id AS food_id, " +
                        "f.food_name, " +
                        "f.quantity, " +
                        "f.category, " +
                        "f.location, " +
                        "f.image, " +
                        "u.name AS donor_name, " +
                        "u.email AS donor_email " +
                        "FROM claims c " +
                        "JOIN food f " +
                        "ON c.food_id=f.id " +
                        "JOIN users u " +
                        "ON f.donor_id=u.id " +
                        "WHERE c.recipient_id=? " +
                        "ORDER BY c.id DESC";

        try (Connection c = getConnection();
             PreparedStatement p =
                     c.prepareStatement(sql)) {

            p.setInt(
                    1,
                    recipientId
            );

            try (ResultSet rs =
                         p.executeQuery()) {

                while (rs.next()) {

                    JSONObject item =
                            new JSONObject();

                    item.put(
                            "claimId",
                            rs.getInt("claim_id")
                    );

                    item.put(
                            "foodId",
                            rs.getInt("food_id")
                    );

                    item.put(
                            "status",
                            safeString(
                                    rs.getString("status")
                            )
                    );

                    item.put(
                            "foodName",
                            safeString(
                                    rs.getString("food_name")
                            )
                    );

                    item.put(
                            "quantity",
                            safeString(
                                    rs.getString("quantity")
                            )
                    );

                    item.put(
                            "category",
                            safeString(
                                    rs.getString("category")
                            )
                    );

                    item.put(
                            "location",
                            safeString(
                                    rs.getString("location")
                            )
                    );

                    item.put(
                            "image",
                            safeString(
                                    rs.getString("image")
                            )
                    );

                    item.put(
                            "donorName",
                            safeString(
                                    rs.getString("donor_name")
                            )
                    );

                    item.put(
                            "donorEmail",
                            safeString(
                                    rs.getString("donor_email")
                            )
                    );

                    claims.put(item);
                }
            }
        }

        json(
                response,
                claims
        );
    }


    // =========================================================
    // STATS
    // =========================================================

    private void getStats(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        int currentUser =
                userId(request);

        String role =
                userRole(request);

        JSONObject result =
                new JSONObject();

        int foodCount = 0;
        int claimCount = 0;
        int deliveryCount = 0;

        try (Connection c = getConnection()) {

            if ("DONOR".equalsIgnoreCase(role)) {

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT COUNT(*) " +
                                             "FROM food " +
                                             "WHERE donor_id=?"
                             )) {

                    p.setInt(
                            1,
                            currentUser
                    );

                    try (ResultSet rs =
                                 p.executeQuery()) {

                        if (rs.next()) {
                            foodCount =
                                    rs.getInt(1);
                        }
                    }
                }

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT COUNT(*) " +
                                             "FROM claims c " +
                                             "JOIN food f " +
                                             "ON c.food_id=f.id " +
                                             "WHERE f.donor_id=?"
                             )) {

                    p.setInt(
                            1,
                            currentUser
                    );

                    try (ResultSet rs =
                                 p.executeQuery()) {

                        if (rs.next()) {
                            claimCount =
                                    rs.getInt(1);
                        }
                    }
                }

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT COUNT(*) " +
                                             "FROM deliveries d " +
                                             "JOIN food f " +
                                             "ON d.food_id=f.id " +
                                             "WHERE f.donor_id=?"
                             )) {

                    p.setInt(
                            1,
                            currentUser
                    );

                    try (ResultSet rs =
                                 p.executeQuery()) {

                        if (rs.next()) {
                            deliveryCount =
                                    rs.getInt(1);
                        }
                    }
                }

            } else if (
                    "RECIPIENT".equalsIgnoreCase(role)
            ) {

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT COUNT(*) " +
                                             "FROM food " +
                                             "WHERE status='AVAILABLE'"
                             );
                     ResultSet rs =
                             p.executeQuery()) {

                    if (rs.next()) {
                        foodCount =
                                rs.getInt(1);
                    }
                }

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT COUNT(*) " +
                                             "FROM claims " +
                                             "WHERE recipient_id=?"
                             )) {

                    p.setInt(
                            1,
                            currentUser
                    );

                    try (ResultSet rs =
                                 p.executeQuery()) {

                        if (rs.next()) {
                            claimCount =
                                    rs.getInt(1);
                        }
                    }
                }

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT COUNT(*) " +
                                             "FROM deliveries " +
                                             "WHERE recipient_id=?"
                             )) {

                    p.setInt(
                            1,
                            currentUser
                    );

                    try (ResultSet rs =
                                 p.executeQuery()) {

                        if (rs.next()) {
                            deliveryCount =
                                    rs.getInt(1);
                        }
                    }
                }

            } else if (
                    "VOLUNTEER".equalsIgnoreCase(role)
            ) {

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT COUNT(*) " +
                                             "FROM deliveries " +
                                             "WHERE volunteer_id=?"
                             )) {

                    p.setInt(
                            1,
                            currentUser
                    );

                    try (ResultSet rs =
                                 p.executeQuery()) {

                        if (rs.next()) {
                            deliveryCount =
                                    rs.getInt(1);
                        }
                    }
                }

            } else {

                try (Statement s =
                             c.createStatement()) {

                    try (ResultSet rs =
                                 s.executeQuery(
                                         "SELECT COUNT(*) FROM food"
                                 )) {

                        if (rs.next()) {
                            foodCount =
                                    rs.getInt(1);
                        }
                    }

                    try (ResultSet rs =
                                 s.executeQuery(
                                         "SELECT COUNT(*) FROM claims"
                                 )) {

                        if (rs.next()) {
                            claimCount =
                                    rs.getInt(1);
                        }
                    }

                    try (ResultSet rs =
                                 s.executeQuery(
                                         "SELECT COUNT(*) FROM deliveries"
                                 )) {

                        if (rs.next()) {
                            deliveryCount =
                                    rs.getInt(1);
                        }
                    }
                }
            }
        }

        result.put(
                "foodCount",
                foodCount
        );

        result.put(
                "claimCount",
                claimCount
        );

        result.put(
                "deliveryCount",
                deliveryCount
        );

        json(
                response,
                result
        );
    }


    // =========================================================
    // VOLUNTEERS
    // =========================================================

    private void getVolunteers(
            HttpServletResponse response
    ) throws Exception {

        JSONArray volunteers =
                new JSONArray();

        String sql =
                "SELECT " +
                        "id, name, email, phone, " +
                        "location, availability, vehicle " +
                        "FROM users " +
                        "WHERE role='VOLUNTEER' " +
                        "ORDER BY " +
                        "CASE " +
                        "WHEN availability='AVAILABLE' THEN 0 " +
                        "ELSE 1 " +
                        "END, " +
                        "name";

        try (Connection c = getConnection();
             PreparedStatement p =
                     c.prepareStatement(sql);
             ResultSet rs =
                     p.executeQuery()) {

            while (rs.next()) {

                JSONObject item =
                        new JSONObject();

                item.put(
                        "id",
                        rs.getInt("id")
                );

                item.put(
                        "name",
                        safeString(
                                rs.getString("name")
                        )
                );

                item.put(
                        "email",
                        safeString(
                                rs.getString("email")
                        )
                );

                item.put(
                        "phone",
                        safeString(
                                rs.getString("phone")
                        )
                );

                item.put(
                        "location",
                        safeString(
                                rs.getString("location")
                        )
                );

                item.put(
                        "availability",
                        safeString(
                                rs.getString("availability")
                        )
                );

                item.put(
                        "vehicle",
                        safeString(
                                rs.getString("vehicle")
                        )
                );

                volunteers.put(item);
            }
        }

        json(
                response,
                volunteers
        );
    }


    // =========================================================
    // USERS
    // =========================================================

    private void getUsers(
            HttpServletResponse response
    ) throws Exception {

        JSONArray users =
                new JSONArray();

        String sql =
                "SELECT id,name,email,role," +
                        "phone,location,organization," +
                        "availability,vehicle " +
                        "FROM users " +
                        "ORDER BY id DESC";

        try (Connection c = getConnection();
             PreparedStatement p =
                     c.prepareStatement(sql);
             ResultSet rs =
                     p.executeQuery()) {

            while (rs.next()) {

                JSONObject item =
                        new JSONObject();

                item.put(
                        "id",
                        rs.getInt("id")
                );

                item.put(
                        "name",
                        safeString(
                                rs.getString("name")
                        )
                );

                item.put(
                        "email",
                        safeString(
                                rs.getString("email")
                        )
                );

                item.put(
                        "role",
                        safeString(
                                rs.getString("role")
                        )
                );

                item.put(
                        "phone",
                        safeString(
                                rs.getString("phone")
                        )
                );

                item.put(
                        "location",
                        safeString(
                                rs.getString("location")
                        )
                );

                item.put(
                        "organization",
                        safeString(
                                rs.getString("organization")
                        )
                );

                item.put(
                        "availability",
                        safeString(
                                rs.getString("availability")
                        )
                );

                item.put(
                        "vehicle",
                        safeString(
                                rs.getString("vehicle")
                        )
                );

                users.put(item);
            }
        }

        json(
                response,
                users
        );
    }


    // =========================================================
    // POST
    // =========================================================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        /*
         * IMPORTANT:
         * Capture authentication BEFORE multipart processing.
         *
         * The donation request uses multipart/form-data.
         * We save the session user ID and role as request
         * attributes before reading multipart parameters.
         */

        HttpSession session =
                request.getSession(false);

        int authenticatedUserId = -1;
        String authenticatedRole = "";

        if (session != null) {

            Object storedUserId =
                    session.getAttribute("userId");

            if (storedUserId != null) {

                try {

                    if (storedUserId instanceof Number) {

                        authenticatedUserId =
                                ((Number) storedUserId).intValue();

                    } else {

                        authenticatedUserId =
                                Integer.parseInt(
                                        storedUserId.toString()
                                );
                    }

                } catch (NumberFormatException e) {

                    authenticatedUserId = -1;
                }
            }

            Object storedRole =
                    session.getAttribute("role");

            if (storedRole != null) {

                authenticatedRole =
                        storedRole.toString();
            }
        }

        request.setAttribute(
                "foodloopUserId",
                authenticatedUserId
        );

        request.setAttribute(
                "foodloopRole",
                authenticatedRole
        );

        try {

            String action =
                    request.getParameter("action");

            if ("donate".equalsIgnoreCase(action)) {

                donate(
                        request,
                        response
                );

            } else if (
                    "claim".equalsIgnoreCase(action)
            ) {

                claim(
                        request,
                        response
                );

            } else if (
                    "claim-decision".equalsIgnoreCase(action)
            ) {

                claimDecision(
                        request,
                        response
                );

            } else if (
                    "delete".equalsIgnoreCase(action)
            ) {

                deleteFood(
                        request,
                        response
                );

            } else {

                JSONObject result =
                        new JSONObject();

                result.put(
                        "ok",
                        false
                );

                result.put(
                        "message",
                        "Unknown food action: " +
                                (action == null
                                        ? ""
                                        : action)
                );

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                json(
                        response,
                        result
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    e.getMessage() == null
                            ? "Server error."
                            : e.getMessage()
            );

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            json(
                    response,
                    result
            );
        }
    }


    // =========================================================
    // DONATE
    // =========================================================

    private void donate(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        /*
         * Get authentication captured in doPost().
         */
        Object storedUserId =
                request.getAttribute("foodloopUserId");

        int donorId = -1;

        if (storedUserId instanceof Number) {

            donorId =
                    ((Number) storedUserId).intValue();

        } else if (storedUserId != null) {

            try {

                donorId =
                        Integer.parseInt(
                                storedUserId.toString()
                        );

            } catch (NumberFormatException e) {

                donorId = -1;
            }
        }

        Object storedRole =
                request.getAttribute("foodloopRole");

        String donorRole =
                storedRole == null
                        ? ""
                        : storedRole.toString();

        /*
         * Donation is allowed only for logged-in donors.
         */
        if (
                donorId < 0 ||
                !"DONOR".equalsIgnoreCase(donorRole)
        ) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    "Donor login required."
            );

            json(
                    response,
                    result
            );

            return;
        }

        String foodName =
                param(
                        request,
                        "foodName"
                );

        String category =
                param(
                        request,
                        "category"
                );

        String quantity =
                param(
                        request,
                        "quantity"
                );

        String sourceType =
                param(
                        request,
                        "sourceType"
                );

        String description =
                param(
                        request,
                        "description"
                );

        String location =
                param(
                        request,
                        "location"
                );

        String expiryDate =
                param(
                        request,
                        "expiryDate"
                );

        if (
                foodName.isBlank() ||
                category.isBlank() ||
                quantity.isBlank() ||
                location.isBlank()
        ) {

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    "Food name, category, quantity and location are required."
            );

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            json(
                    response,
                    result
            );

            return;
        }

        // =====================================================
        // IMAGE UPLOAD
        // =====================================================

        String imageName = "";

        Part imagePart = null;

        try {

            imagePart =
                    request.getPart("image");

        } catch (Exception e) {

            e.printStackTrace();
        }

        if (
                imagePart != null &&
                imagePart.getSize() > 0
        ) {

            String submittedFileName =
                    imagePart.getSubmittedFileName();

            String original =
                    submittedFileName == null
                            ? ""
                            : Paths.get(
                                    submittedFileName
                              )
                              .getFileName()
                              .toString();

            String extension = "";

            int dot =
                    original.lastIndexOf('.');

            if (dot >= 0) {

                extension =
                        original.substring(dot)
                                .toLowerCase();
            }

            imageName =
                    UUID.randomUUID() +
                            extension;

            String uploadPath =
                    getServletContext()
                            .getRealPath(
                                    "/uploads"
                            );

            /*
             * When running using the embedded Tomcat setup,
             * getRealPath() may be null.
             */
            if (uploadPath == null) {

                uploadPath =
                        new File(
                                "src/main/webapp/uploads"
                        ).getAbsolutePath();
            }

            File uploadDirectory =
                    new File(uploadPath);

            if (!uploadDirectory.exists()) {

                uploadDirectory.mkdirs();
            }

            File target =
                    new File(
                            uploadDirectory,
                            imageName
                    );

            imagePart.write(
                    target.getAbsolutePath()
            );
        }

        // =====================================================
        // INSERT FOOD
        // =====================================================

        String sql =
                "INSERT INTO food (" +
                        "donor_id," +
                        "food_name," +
                        "category," +
                        "quantity," +
                        "source_type," +
                        "description," +
                        "location," +
                        "expiry_date," +
                        "image," +
                        "status," +
                        "created_at" +
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection c = getConnection();
             PreparedStatement p =
                     c.prepareStatement(sql)) {

            p.setInt(
                    1,
                    donorId
            );

            p.setString(
                    2,
                    foodName
            );

            p.setString(
                    3,
                    category
            );

            p.setString(
                    4,
                    quantity
            );

            p.setString(
                    5,
                    sourceType
            );

            p.setString(
                    6,
                    description
            );

            p.setString(
                    7,
                    location
            );

            p.setString(
                    8,
                    expiryDate
            );

            p.setString(
                    9,
                    imageName
            );

            p.setString(
                    10,
                    "AVAILABLE"
            );

            p.setString(
                    11,
                    LocalDateTime.now().toString()
            );

            p.executeUpdate();
        }

        JSONObject result =
                new JSONObject();

        result.put(
                "ok",
                true
        );

        result.put(
                "message",
                "Food donated successfully."
        );

        result.put(
                "image",
                imageName
        );

        json(
                response,
                result
        );
    }


    // =========================================================
    // CLAIM
    // =========================================================

    private void claim(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        int recipientId =
                userId(request);

        if (recipientId < 0) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    "Please login first."
            );

            json(
                    response,
                    result
            );

            return;
        }

        int foodId =
                Integer.parseInt(
                        request.getParameter(
                                "foodId"
                        )
                );

        try (Connection c =
                     getConnection()) {

            c.setAutoCommit(false);

            try {

                // Check food availability
                try (PreparedStatement check =
                             c.prepareStatement(
                                     "SELECT status " +
                                             "FROM food " +
                                             "WHERE id=?"
                             )) {

                    check.setInt(
                            1,
                            foodId
                    );

                    try (ResultSet rs =
                                 check.executeQuery()) {

                        if (
                                !rs.next() ||
                                !"AVAILABLE".equalsIgnoreCase(
                                        rs.getString("status")
                                )
                        ) {

                            JSONObject result =
                                    new JSONObject();

                            result.put(
                                    "ok",
                                    false
                            );

                            result.put(
                                    "message",
                                    "This food is no longer available."
                            );

                            json(
                                    response,
                                    result
                            );

                            c.rollback();

                            return;
                        }
                    }
                }

                // Check duplicate claim
                try (PreparedStatement check =
                             c.prepareStatement(
                                     "SELECT id " +
                                             "FROM claims " +
                                             "WHERE food_id=? " +
                                             "AND recipient_id=? " +
                                             "AND status IN " +
                                             "('PENDING','APPROVED','ACCEPTED')"
                             )) {

                    check.setInt(
                            1,
                            foodId
                    );

                    check.setInt(
                            2,
                            recipientId
                    );

                    try (ResultSet rs =
                                 check.executeQuery()) {

                        if (rs.next()) {

                            JSONObject result =
                                    new JSONObject();

                            result.put(
                                    "ok",
                                    false
                            );

                            result.put(
                                    "message",
                                    "You have already claimed this food."
                            );

                            json(
                                    response,
                                    result
                            );

                            c.rollback();

                            return;
                        }
                    }
                }

                // Insert claim
                try (PreparedStatement p =
                             c.prepareStatement(
                                     "INSERT INTO claims (" +
                                             "food_id," +
                                             "recipient_id," +
                                             "status," +
                                             "created_at" +
                                             ") VALUES (?,?,?,?)"
                             )) {

                    p.setInt(
                            1,
                            foodId
                    );

                    p.setInt(
                            2,
                            recipientId
                    );

                    p.setString(
                            3,
                            "PENDING"
                    );

                    p.setString(
                            4,
                            LocalDateTime.now().toString()
                    );

                    p.executeUpdate();
                }

                c.commit();

            } catch (Exception e) {

                c.rollback();

                throw e;

            } finally {

                c.setAutoCommit(true);
            }
        }

        JSONObject result =
                new JSONObject();

        result.put(
                "ok",
                true
        );

        result.put(
                "message",
                "Food claim submitted successfully."
        );

        json(
                response,
                result
        );
    }


    // =========================================================
    // CLAIM DECISION
    // =========================================================

    private void claimDecision(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        int donorId =
                userId(request);

        if (
                donorId < 0 ||
                !"DONOR".equalsIgnoreCase(
                        userRole(request)
                )
        ) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    "Donor login required."
            );

            json(
                    response,
                    result
            );

            return;
        }

        int claimId =
                Integer.parseInt(
                        request.getParameter(
                                "claimId"
                        )
                );

        String decision =
                param(
                        request,
                        "decision"
                ).toUpperCase();

        if (
                !"ACCEPT".equals(decision) &&
                !"REJECT".equals(decision)
        ) {

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    "Invalid claim decision."
            );

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            json(
                    response,
                    result
            );

            return;
        }

        try (Connection c =
                     getConnection()) {

            c.setAutoCommit(false);

            try {

                int foodId;
                String currentStatus;

                try (PreparedStatement p =
                             c.prepareStatement(
                                     "SELECT " +
                                             "c.food_id," +
                                             "c.status " +
                                             "FROM claims c " +
                                             "JOIN food f " +
                                             "ON c.food_id=f.id " +
                                             "WHERE c.id=? " +
                                             "AND f.donor_id=?"
                             )) {

                    p.setInt(
                            1,
                            claimId
                    );

                    p.setInt(
                            2,
                            donorId
                    );

                    try (ResultSet rs =
                                 p.executeQuery()) {

                        if (!rs.next()) {

                            JSONObject result =
                                    new JSONObject();

                            result.put(
                                    "ok",
                                    false
                            );

                            result.put(
                                    "message",
                                    "Claim request not found."
                            );

                            json(
                                    response,
                                    result
                            );

                            c.rollback();

                            return;
                        }

                        foodId =
                                rs.getInt(
                                        "food_id"
                                );

                        currentStatus =
                                rs.getString(
                                        "status"
                                );
                    }
                }

                if (
                        !"PENDING".equalsIgnoreCase(
                                currentStatus
                        )
                ) {

                    JSONObject result =
                            new JSONObject();

                    result.put(
                            "ok",
                            false
                    );

                    result.put(
                            "message",
                            "This claim has already been processed."
                    );

                    json(
                            response,
                            result
                    );

                    c.rollback();

                    return;
                }

                if ("ACCEPT".equals(decision)) {

                    // Keep APPROVED because this is the
                    // existing FoodLoop claim workflow.

                    try (PreparedStatement p =
                                 c.prepareStatement(
                                         "UPDATE claims " +
                                                 "SET status='APPROVED' " +
                                                 "WHERE id=?"
                                 )) {

                        p.setInt(
                                1,
                                claimId
                        );

                        p.executeUpdate();
                    }

                    try (PreparedStatement p =
                                 c.prepareStatement(
                                         "UPDATE food " +
                                                 "SET status='CLAIMED' " +
                                                 "WHERE id=?"
                                 )) {

                        p.setInt(
                                1,
                                foodId
                        );

                        p.executeUpdate();
                    }

                    // Reject other pending claims
                    // for the same food.

                    try (PreparedStatement p =
                                 c.prepareStatement(
                                         "UPDATE claims " +
                                                 "SET status='REJECTED' " +
                                                 "WHERE food_id=? " +
                                                 "AND id<>? " +
                                                 "AND status='PENDING'"
                                 )) {

                        p.setInt(
                                1,
                                foodId
                        );

                        p.setInt(
                                2,
                                claimId
                        );

                        p.executeUpdate();
                    }

                } else {

                    try (PreparedStatement p =
                                 c.prepareStatement(
                                         "UPDATE claims " +
                                                 "SET status='REJECTED' " +
                                                 "WHERE id=?"
                                 )) {

                        p.setInt(
                                1,
                                claimId
                        );

                        p.executeUpdate();
                    }
                }

                c.commit();

            } catch (Exception e) {

                c.rollback();

                throw e;

            } finally {

                c.setAutoCommit(true);
            }
        }

        JSONObject result =
                new JSONObject();

        result.put(
                "ok",
                true
        );

        if ("ACCEPT".equals(decision)) {

            result.put(
                    "message",
                    "Claim accepted. Please choose a volunteer."
            );

            result.put(
                    "claimId",
                    claimId
            );

        } else {

            result.put(
                    "message",
                    "Claim rejected successfully."
            );
        }

        json(
                response,
                result
        );
    }


    // =========================================================
    // DELETE FOOD
    // =========================================================

    private void deleteFood(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        int donorId =
                userId(request);

        if (
                donorId < 0 ||
                !"DONOR".equalsIgnoreCase(
                        userRole(request)
                )
        ) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            JSONObject result =
                    new JSONObject();

            result.put(
                    "ok",
                    false
            );

            result.put(
                    "message",
                    "Donor login required."
            );

            json(
                    response,
                    result
            );

            return;
        }

        int foodId =
                Integer.parseInt(
                        request.getParameter(
                                "foodId"
                        )
                );

        try (Connection c =
                     getConnection();
             PreparedStatement p =
                     c.prepareStatement(
                             "DELETE FROM food " +
                                     "WHERE id=? " +
                                     "AND donor_id=? " +
                                     "AND status='AVAILABLE'"
                     )) {

            p.setInt(
                    1,
                    foodId
            );

            p.setInt(
                    2,
                    donorId
            );

            int rows =
                    p.executeUpdate();

            JSONObject result =
                    new JSONObject();

            if (rows == 0) {

                result.put(
                        "ok",
                        false
                );

                result.put(
                        "message",
                        "Food could not be deleted."
                );

            } else {

                result.put(
                        "ok",
                        true
                );

                result.put(
                        "message",
                        "Food deleted successfully."
                );
            }

            json(
                    response,
                    result
            );
        }
    }


    // =========================================================
    // PARAMETER
    // =========================================================

    private String param(
            HttpServletRequest request,
            String name
    ) {

        String value =
                request.getParameter(name);

        return value == null
                ? ""
                : value.trim();
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safeString(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}