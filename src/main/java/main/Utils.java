package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Utils
{
    private Utils() {}

    public static void fileLineReader(final String fileName,  final ThrowableBiConsumer<Integer, String> consumer) {
        fileLineReader(new File(fileName), consumer);
    }

    public static void fileLineReader(final File file,  final ThrowableBiConsumer<Integer, String> consumer) {
        try {
            fileLineReader(new FileInputStream(file), consumer);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void fileLineReader(final InputStream is,  final ThrowableBiConsumer<Integer, String> consumer) {
        int lineNo = 0;

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            while (reader.ready()) {
                final String row = reader.readLine();
                consumer.accept(++lineNo, row);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage() + " in line: " + lineNo);
        }
    }

    @FunctionalInterface
    public interface ThrowableConsumer<T> extends Consumer<T> {
        @Override
        default void accept(final T e) {
            try {
                acceptWithThrowable(e);
            }
            catch (final Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        void acceptWithThrowable(T e) throws Throwable;
    }

    @FunctionalInterface
    public interface ThrowableBiConsumer<T, U> extends BiConsumer<T, U> {
        @Override
        default void accept(final T v1, final U v2) {
            try {
                acceptWithThrowable(v1, v2);
            }
            catch (final Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        void acceptWithThrowable(final T v1, final U v2) throws Throwable;
    }

    public static void fileWriter(final String fileName, final ThrowableConsumer<BufferedWriter> consumer) {
        fileWriter(fileName,  false, consumer);
    }

    public static void fileWriter(final String fileName, final boolean appendToFile, final ThrowableConsumer<BufferedWriter> consumer) {
        createDirectoriesForFile(fileName);

        try (final BufferedWriter f = new BufferedWriter(new FileWriter(fileName, appendToFile))) {
            consumer.accept(f);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createDirectoriesForFile(final File file) {
        createDirectoriesForFile(file.getAbsolutePath());
    }

    public static void createDirectoriesForFile(final String fileName) {
        try {
            Files.createDirectories(Paths.get(fileName).getParent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Box<T> {
        public T value;

        public Box(T value) { this.value = value; }

        public String toString() {
            return value.toString();
        }
    }

    public static String getCanonicalPath(final String fileName) {
        return getCanonicalPath(new File(fileName));
    }

    public static String getCanonicalPath(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isInteger(final String s) {
        try {
            Integer.parseInt(s);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public static String nonEmpty(final String s) {
        return nonEmpty(s, "string must not be empty");
    }

    public static String nonEmpty(final String s, final String errorMessage) {
        Objects.requireNonNull(s);
        if (s.trim().isEmpty())
            throw new IllegalArgumentException(errorMessage);
        return s;
    }

    public static boolean fileExists(final String fileName) {
        try {
            File f = new File(fileName);
            return f.exists();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
