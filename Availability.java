package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Availability {
    private final Date time;
    private final String username;

    public static class AvailabilityBuilder {
        private final Date time;
        private final String username;

        public AvailabilityBuilder(Date time, String username) {
            this.time = time;
            this.username = username;
        }

        public Availability build() {
            return new Availability(this);
        }
    }

    private Availability(AvailabilityBuilder builder) {
        this.time = builder.time;
        this.username = builder.username;
    }

    public Date getTime() {
        return time;
    }

    public String getUsername() {
        return username;
    }
}
