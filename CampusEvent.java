package model;

import java.io.Serializable;

public class CampusEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventName;
    private String description;
    private String dateTime;
    private String venue;
    private String coordinator;
    private String status; // UPCOMING, ONGOING, COMPLETED, CANCELLED

    public CampusEvent() {
        this.status = "UPCOMING";
    }

    public CampusEvent(String eventId, String eventName, String description,
                       String dateTime, String venue, String coordinator, String status) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.description = description;
        this.dateTime = dateTime;
        this.venue = venue;
        this.coordinator = coordinator;
        this.status = (status != null && !status.isEmpty()) ? status : "UPCOMING";
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCoordinator() { return coordinator; }
    public void setCoordinator(String coordinator) { this.coordinator = coordinator; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isUpcoming() {
        return "UPCOMING".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return eventName + " (" + dateTime + " @ " + venue + ")";
    }
}
