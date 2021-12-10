package scheduler.model;

import java.util.Date;

public class Appointment {
    private final String caregiverName;
    private final String patientName;
    private final String vaccineName;
    private final Date date;
    private final String appointmentID;

    public static class AppointmentBuilder {
        private final String caregiverName;
        private final String patientName;
        private final String vaccineName;
        private final Date date;
        private final String appointmentID;

        public AppointmentBuilder(String caregiverName, String patientName, String vaccineName,
                                  Date date, String appointmentID) {
            this.caregiverName = caregiverName;
            this.patientName = patientName;
            this.vaccineName = vaccineName;
            this.date = date;
            this.appointmentID = appointmentID;
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }

    private Appointment(AppointmentBuilder builder) {
        this.caregiverName = builder.caregiverName;
        this.patientName = builder.caregiverName;
        this.vaccineName = builder.vaccineName;
        this.date = builder.date;
        this.appointmentID = builder.appointmentID;
    }

    public String getCaregiverName() {
        return caregiverName;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public Date getDate() {
        return date;
    }

    public String getAppointmentID() {
        return appointmentID;
    }
}
