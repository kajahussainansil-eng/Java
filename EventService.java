package service;

import model.CampusEvent;
import model.User;
import security.AccessControl;
import storage.WebDataManager;
import util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventService {

    private final WebDataManager dataManager;

    public EventService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<CampusEvent> getAllEvents() {
        return dataManager.getEvents().values().stream()
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .collect(Collectors.toList());
    }

    public List<CampusEvent> getUpcomingEvents(int limit) {
        return dataManager.getEvents().values().stream()
                .filter(CampusEvent::isUpcoming)
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public synchronized CampusEvent createEvent(User actor, String name, String description,
                                                String dateTime, String venue, String coordinator) throws Exception {
        if (!AccessControl.canManageEvents(actor)) {
            throw new SecurityException("Permission denied to create campus events.");
        }
        if (!ValidationUtils.isNotEmpty(name)) throw new IllegalArgumentException("Event name is required.");
        if (!ValidationUtils.isNotEmpty(dateTime)) throw new IllegalArgumentException("Date & time is required.");
        if (!ValidationUtils.isNotEmpty(venue)) throw new IllegalArgumentException("Venue is required.");

        String id = dataManager.generateNextEventId();
        CampusEvent ev = new CampusEvent(id, name.trim(), description != null ? description.trim() : "",
                dateTime.trim(), venue.trim(), coordinator != null ? coordinator.trim() : actor.getFullName(), "UPCOMING");

        dataManager.getEvents().put(id, ev);
        dataManager.saveEvents();
        dataManager.logAudit(actor.getUserId(), "CREATE_EVENT", "Created event " + id + ": " + name);
        return ev;
    }

    public synchronized void cancelEvent(User actor, String eventId) throws Exception {
        if (!AccessControl.canManageEvents(actor)) throw new SecurityException("Permission denied.");
        CampusEvent ev = dataManager.getEvents().get(eventId);
        if (ev == null) throw new IllegalArgumentException("Event not found.");

        ev.setStatus("CANCELLED");
        dataManager.saveEvents();
        dataManager.logAudit(actor.getUserId(), "CANCEL_EVENT", "Cancelled event " + eventId);
    }

    public List<CampusEvent> searchEvents(String query, String statusFilter) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String status = statusFilter != null ? statusFilter.trim() : "All";

        return dataManager.getEvents().values().stream()
                .filter(e -> {
                    boolean matchesQuery = q.isEmpty() ||
                            e.getEventName().toLowerCase().contains(q) ||
                            e.getDescription().toLowerCase().contains(q) ||
                            e.getVenue().toLowerCase().contains(q) ||
                            e.getCoordinator().toLowerCase().contains(q);

                    boolean matchesStatus = status.equalsIgnoreCase("All") ||
                            e.getStatus().equalsIgnoreCase(status);

                    return matchesQuery && matchesStatus;
                })
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .collect(Collectors.toList());
    }
}
