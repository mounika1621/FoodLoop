package com.foodwaste;

import com.foodwaste.servlet.AuthServlet;
import com.foodwaste.servlet.FoodServlet;
import com.foodwaste.servlet.DeliveryServlet;

import jakarta.servlet.MultipartConfigElement;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {

        // =========================================================
        // INITIALIZE DATABASE
        // =========================================================

        FoodServlet.initDatabase();

        // =========================================================
        // CREATE TOMCAT SERVER
        // =========================================================

        Tomcat tomcat = new Tomcat();

        tomcat.setPort(8080);

        // Force connector creation
        tomcat.getConnector();

        // =========================================================
        // WEB APPLICATION
        // =========================================================

        File webApp = new File("src/main/webapp");

        Context context = tomcat.addContext(
                "",
                webApp.getAbsolutePath()
        );

        // =========================================================
        // DEFAULT SERVLET
        // =========================================================

        Wrapper defaultServlet = Tomcat.addServlet(
                context,
                "default",
                new org.apache.catalina.servlets.DefaultServlet()
        );

        defaultServlet.setLoadOnStartup(1);

        context.addServletMappingDecoded(
                "/",
                "default"
        );

        context.addWelcomeFile(
                "index.html"
        );

        // =========================================================
        // AUTH SERVLET
        // =========================================================

        Wrapper authServlet = Tomcat.addServlet(
                context,
                "auth",
                new AuthServlet()
        );

        authServlet.setLoadOnStartup(1);

        context.addServletMappingDecoded(
                "/api/auth/*",
                "auth"
        );

        // =========================================================
        // FOOD SERVLET
        // =========================================================

        Wrapper foodServlet = Tomcat.addServlet(
                context,
                "food",
                new FoodServlet()
        );

        foodServlet.setLoadOnStartup(1);

        // ---------------------------------------------------------
        // MULTIPART CONFIGURATION
        // ---------------------------------------------------------

        if (foodServlet instanceof StandardWrapper) {

            StandardWrapper wrapper =
                    (StandardWrapper) foodServlet;

            wrapper.setMultipartConfigElement(
                    new MultipartConfigElement(
                            System.getProperty("java.io.tmpdir"),
                            10 * 1024 * 1024,
                            20 * 1024 * 1024,
                            1024 * 1024
                    )
            );
        }

        // ---------------------------------------------------------
        // FOOD API
        // ---------------------------------------------------------

        context.addServletMappingDecoded(
                "/api/food",
                "food"
        );

        // ---------------------------------------------------------
        // IMPORTANT:
        // Your existing dashboard.js uses FoodServlet
        // ---------------------------------------------------------

        context.addServletMappingDecoded(
                "/FoodServlet",
                "food"
        );

        // =========================================================
        // DELIVERY SERVLET
        // =========================================================

        Wrapper deliveryServlet = Tomcat.addServlet(
                context,
                "delivery",
                new DeliveryServlet()
        );

        deliveryServlet.setLoadOnStartup(1);

        context.addServletMappingDecoded(
                "/api/delivery",
                "delivery"
        );

        // =========================================================
        // START TOMCAT
        // =========================================================

        tomcat.start();

        // =========================================================
        // CONSOLE INFORMATION
        // =========================================================

        System.out.println();
        System.out.println("==========================================");
        System.out.println("       FOODLOOP SERVER STARTED");
        System.out.println("==========================================");

        System.out.println();
        System.out.println("Main URL:");
        System.out.println("http://localhost:8080/");

        System.out.println();
        System.out.println("Food API:");
        System.out.println("http://localhost:8080/FoodServlet");

        System.out.println();
        System.out.println("Food API alternative:");
        System.out.println("http://localhost:8080/api/food");

        System.out.println();
        System.out.println("Delivery API:");
        System.out.println("http://localhost:8080/api/delivery");

        System.out.println();
        System.out.println("Multipart upload: ENABLED");

        System.out.println();
        System.out.println("==========================================");
        System.out.println();

        // Keep server running
        tomcat.getServer().await();
    }
}