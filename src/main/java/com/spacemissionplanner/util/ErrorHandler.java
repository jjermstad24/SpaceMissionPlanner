package com.spacemissionplanner.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ErrorHandler {

    private static final Logger LOG = Logger.getLogger(ErrorHandler.class.getName());
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static Consumer<String> statusConsumer;

    static {
        LOG.setLevel(Level.ALL);
    }

    private ErrorHandler() {}

    public static void setStatusConsumer(Consumer<String> consumer) {
        statusConsumer = consumer;
    }

    public static void info(String message) {
        String line = ts() + " [INFO] " + message;
        LOG.info(message);
        setStatus(message);
    }

    public static void warn(String message, Throwable e) {
        String line = ts() + " [WARN] " + message;
        LOG.warning(message + (e != null ? " — " + e.getMessage() : ""));
        System.err.println(line);
        if (e != null) {
            e.printStackTrace(System.err);
        }
        setStatus("⚠ " + message);
    }

    public static void error(String context, Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        String line = ts() + " [ERROR] " + context + " — " + e.getMessage();
        LOG.log(Level.SEVERE, context, e);
        System.err.println(line);
        System.err.println(trace);
        setStatus("✗ " + context);
    }

    public static void showError(String title, String message) {
        error(title, new RuntimeException(message));
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.show();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, message);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.show();
            });
        }
    }

    public static <T> T parseSafe(Supplier<T> supplier, T fallback, String context) {
        try {
            return supplier.get();
        } catch (Exception e) {
            warn(context + " — using fallback value", e);
            return fallback;
        }
    }

    public static void runSafe(Runnable runnable, String context) {
        try {
            runnable.run();
        } catch (Exception e) {
            error(context, e);
        }
    }

    public static void runSafeSilent(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {}
    }

    private static void setStatus(String message) {
        if (statusConsumer != null) {
            if (Platform.isFxApplicationThread()) {
                statusConsumer.accept(message);
            } else {
                Platform.runLater(() -> statusConsumer.accept(message));
            }
        }
    }

    private static String ts() {
        return LocalDateTime.now().format(TS);
    }
}
