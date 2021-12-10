package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null ) {
            System.out.println("Already logged-in!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (weakPassword(password)) {
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null ) {
            System.out.println("Already logged-in!");
            return;
        }
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (weakPassword(password)) {
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean weakPassword(String password) {
        Pattern pattern1 = Pattern.compile("^\\d+$");
        Matcher matcher1 = pattern1.matcher(password);
        Pattern patter2 = Pattern.compile("^[a-zA-Z]+$");
        Matcher matcher2 = patter2.matcher(password);
        Pattern pattern3 = Pattern.compile("^.*[!@#?]+.*$");
        Matcher matcher3 = pattern3.matcher(password);
        if (password.length() < 8) {
            System.out.println("A strong password should have at least 8 characters!");
            return true;
        } else if (matcher1.matches() || matcher2.matches()) {
            System.out.println("A strong password should be a mixture of letters and numbers!");
            return true;
        } else if (password.toLowerCase().equals(password)) {
            System.out.println("A strong password should be a mixture of both uppercase and lowercase letters!");
            return true;
        } else if (!matcher3.matches()) {
            System.out.println("A strong password should include at least one special character: !, @, #, and ?");
            return true;
        }
        System.out.println("You created a strong password!");
        return false;
    }


    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null ) {
            System.out.println("Already logged-in!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (tokens.length != 2) {
            System.out.println("Please re-enter the date!");
            return;
        }
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in to perform this operation!");
            return;
        }

        String date = tokens[1];
        List<String> availableCaregivers = new ArrayList<String>();
        try {
            Date d = Date.valueOf(date);
            if (currentCaregiver == null) {
                availableCaregivers = currentPatient.searchAvailability(d);
            } else {
                availableCaregivers = currentCaregiver.searchAvailability(d);
            }
            if (availableCaregivers.size() == 0) {
                System.out.println("No caregivers available at this time!");
            }
            for (int i = 0; i < availableCaregivers.size(); i++) {
                System.out.println("caregiver : " + availableCaregivers.get(i));
            }
            System.out.println("Above is the caregivers and vaccines available at the specified date");
            if (currentCaregiver == null) {
                currentPatient.showAvailableVaccine();
            } else {
                currentCaregiver.showAvailableVaccine();
            }
            System.out.println("Above is the caregivers and vaccines available at the specified date");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching caregivers' schedule");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];
        try {
            Date d = Date.valueOf(date);
            List<String> availableCaregivers = currentPatient.searchAvailability(d);
            if (availableCaregivers.isEmpty()) {
                System.out.println("No caregiver is available this time! Please choose another date!");
                return;
            } else if (checkVaccineAvailability(vaccineName)) {
                return;
            }
            String caregiver = availableCaregivers.get((int)(Math.random() * availableCaregivers.size()));
            String ID = currentPatient.reserveAppointment(caregiver, vaccineName, d);
            if (ID.length() == 0) {
                return;
            }
            System.out.println("Appointment reserved!");
            System.out.println("caregiver : " + caregiver);
            System.out.println("appointmentID : " + ID);
            Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
            vaccine.decreaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Error occurred when reserving");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        }
    }

    private static boolean checkVaccineAvailability(String vaccine) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchVaccineDoses = "SELECT Doses FROM Vaccines WHERE Name = ?";
        try {
            PreparedStatement statement = con.prepareStatement(searchVaccineDoses);
            statement.setString(1, vaccine);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("No such vaccine! Please re-enter the name!");
                return !resultSet.isBeforeFirst();
            } else {
                resultSet.next();
                int doses = resultSet.getInt("Doses");
                if (doses == 0) {
                    System.out.println("This vaccine is not available now!");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking vaccine availability");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return false;
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in to perform this operation!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String appointmentID = tokens[1];
        String vaccineName = "";
        try {
            if (currentCaregiver != null) {
                vaccineName = currentCaregiver.cancelAppointment(appointmentID);
            } else {
                vaccineName = currentPatient.cancelAppointment(appointmentID);
            }
            if (vaccineName.length() == 0) {
                return;
            }
            Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
            vaccine.increaseAvailableDoses(1);
            System.out.println("Appointment canceled");
        } catch (SQLException e) {
            System.out.println("Error occurred when cancelling appointment");
            e.printStackTrace();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in to perform this operation!");
            return;
        }
        try {
            if (currentCaregiver != null) {
                currentCaregiver.showAppointment();
            } else {
                currentPatient.showAppointment();
            }
            System.out.println("Above are your appointment");
        } catch (SQLException e) {
            System.out.println("Error occurred when showing appointments");
            e.printStackTrace();
        }

    }
    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Already logged-out!");
            return;
        }
        if (currentCaregiver != null) {
            currentCaregiver = null;
            System.out.println("Caregiver logged out");
        } else {
            currentPatient = null;
            System.out.println("Patient logged out");
        }
    }
}
