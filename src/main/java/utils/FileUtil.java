package utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FileUtil {
    private static final String USERS_DIR = "GENIUS/data/";

    // Check and create folder if it does not exist
    private static void ensureDataDirectoryExists() {
        File directory = new File(USERS_DIR);
        boolean dirCreated = directory.mkdirs();  // Create a folder and save the result
        if (dirCreated) {
            System.out.println("Directory created successfully.");
        } else {
            System.out.println("Directory already exists or failed to create.");
        }
    }

    // Checking for duplicate emails
    public static boolean isEmailTaken(String email) {
        File userFile = new File(USERS_DIR + email + ".txt");
        return userFile.exists(); // If the file exists, the email is a duplicate.
    }

    // Save user information to file
    public static void saveUser(String email, String nickname, String password) {
        if (isEmailTaken(email)) {
            return;  // If the email is a duplicate, the data will not be saved.
        }

        ensureDataDirectoryExists();  // Ensure the folder exists

        String userFile = USERS_DIR + email + ".txt"; // File for each user
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFile))) {
            writer.write("Email: " + email + "\n");
            writer.write("Nickname: " + nickname + "\n");
            writer.write("Password: " + password + "\n");
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }

    // Reading user information from file
    public static String[] readUser(String email) {
        String userFile = USERS_DIR + email + ".txt";
        String[] userData = new String[2]; // To save username and email

        try {
            List<String> lines = Files.readAllLines(Paths.get(userFile));

            for (String line : lines) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    if (parts[0].equals("Email")) {
                        userData[0] = parts[1];
                    } else if (parts[0].equals("Nickname")) {
                        userData[1] = parts[1];
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading user data: " + e.getMessage());
        }

        return userData;
    }

}
