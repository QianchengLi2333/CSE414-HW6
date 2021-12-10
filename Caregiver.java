package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class Caregiver {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    private Caregiver(CaregiverBuilder builder) {
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Caregiver(CaregiverGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addCaregiver = "INSERT INTO Caregivers VALUES (? , ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addCaregiver);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void uploadAvailability(Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchAvailability = "SELECT * FROM Availabilities WHERE Time = ? AND Username = ?";
        String searchAppointment = "SELECT * FROM Appointments WHERE CaregiverName = ? AND Date = ?";
        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        try {
            PreparedStatement searchStatement = con.prepareStatement(searchAvailability);
            searchStatement.setDate(1, d);
            searchStatement.setString(2, this.username);
            ResultSet resultSet = searchStatement.executeQuery();
            if (resultSet.isBeforeFirst()) {
                System.out.println("You are already available at this time!");
                System.out.println("Failed to upload availability");
                return;
            }
            PreparedStatement searchStatement2 = con.prepareStatement(searchAppointment);
            searchStatement2.setString(1, this.username);
            searchStatement2.setDate(2, d);
            ResultSet resultSet2 = searchStatement2.executeQuery();
            if (resultSet2.isBeforeFirst()) {
                System.out.println("You have an appointment at that time!");
                System.out.println("Failed to upload availability");
                return;
            }
            PreparedStatement addStatement = con.prepareStatement(addAvailability);
            addStatement.setDate(1, d);
            addStatement.setString(2, this.username);
            addStatement.executeUpdate();
            System.out.println("Availability uploaded!");
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public List<String> searchAvailability(Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchAvailability = "SELECT Username FROM Availabilities WHERE Time = ?";
        List<String> availableCaregiver = new ArrayList<String>();
        try {
            PreparedStatement statement = con.prepareStatement(searchAvailability);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String caregiver = resultSet.getString("Username");
                availableCaregiver.add(caregiver);
            }
            return availableCaregiver;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public String cancelAppointment(String appointmentID) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchAppointment = "SELECT CaregiverName, Date, VaccineName FROM Appointments WHERE AppointmentID = ?";
        String cancelAppointment = "DELETE FROM Appointments WHERE AppointmentID = ?";
        String recoverAvailability = "INSERT INTO Availabilities VALUES (?, ?)";
        String vaccine = "";
        try {
            PreparedStatement searchStatement = con.prepareStatement(searchAppointment);
            searchStatement.setString(1, appointmentID);
            ResultSet resultSet = searchStatement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("No such appointment Please re-enter the appointment ID!");
                return vaccine;
            }
            resultSet.next();
            String caregiver = resultSet.getString("CaregiverName");
            if (!this.username.equals(caregiver)) {
                System.out.println("Don't cancel other people's appointment!");
                return vaccine;
            }
//            String caregiver = resultSet.getString("CaregiverName");
            Date d = resultSet.getDate("Date");
            vaccine = resultSet.getString("VaccineName");
            PreparedStatement cancelStatement = con.prepareStatement(cancelAppointment);
            cancelStatement.setString(1, appointmentID);
            cancelStatement.executeUpdate();
            PreparedStatement recoverStatement = con.prepareStatement(recoverAvailability);
            recoverStatement.setDate(1, d);
            recoverStatement.setString(2, this.username);
            recoverStatement.executeUpdate();
            return vaccine;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void showAvailableVaccine() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String showAvailableVaccine = "SELECT * FROM Vaccines";
        try {
            PreparedStatement statement = con.prepareStatement(showAvailableVaccine);
            ResultSet resultSet = statement.executeQuery();
            int availableVaccines = 0;
            while (resultSet.next()) {
                String vaccineName = resultSet.getString("Name");
                int availableDoses = resultSet.getInt("Doses");
                if (availableDoses > 0) {
                    Vaccine vaccine = new Vaccine.VaccineBuilder(vaccineName, availableDoses).build();
                    System.out.println(vaccine);
                }
            }
            if (availableVaccines == 0) {
                System.out.println("No vaccines available!");
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void showAppointment() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String showAppointment = "SELECT * FROM Appointments WHERE CaregiverName = ?";
        try {
            PreparedStatement statement = con.prepareStatement(showAppointment);
            statement.setString(1, this.username);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("You have no appointment");
                return;
            }
            while (resultSet.next()) {
                String appointmentID = resultSet.getString("AppointmentID");
                String vaccine = resultSet.getString("VaccineName");
                java.util.Date date = resultSet.getDate("Date");
                String caregiver = resultSet.getString("PatientName");
                System.out.println("appointmentID : " + appointmentID + ", vaccine : " + vaccine +
                        ", date : " + date + ", patient : " + caregiver);
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class CaregiverBuilder {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        public CaregiverBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Caregiver build() {
            return new Caregiver(this);
        }
    }

    public static class CaregiverGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        public CaregiverGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Caregiver get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getCaregiver = "SELECT Salt, Hash FROM Caregivers WHERE Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getCaregiver);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    // we need to call Util.trim() to get rid of the paddings,
                    // try to remove the use of Util.trim() and you'll see :)
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    // check if the password matches
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Caregiver(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
