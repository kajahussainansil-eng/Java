package service;

import model.Complaint;
import model.Role;
import model.User;
import storage.WebDataManager;
import util.DateUtils;
import util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ComplaintService {

    private final WebDataManager dataManager;

    public ComplaintService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<Complaint> getAllComplaints() {
        return dataManager.getComplaints().values().stream()
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .collect(Collectors.toList());
    }

    public List<Complaint> getComplaintsForStudent(String studentId) {
        if (studentId == null) return new ArrayList<>();
        return dataManager.getComplaints().values().stream()
                .filter(c -> studentId.equalsIgnoreCase(c.getStudentId()))
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .collect(Collectors.toList());
    }

    public synchronized Complaint submitComplaint(User studentUser, String subject, String category,
                                                  String description) throws Exception {
        if (studentUser == null || studentUser.getRole() != Role.STUDENT) {
            throw new SecurityException("Only students can lodge complaints.");
        }
        if (!ValidationUtils.isNotEmpty(subject)) throw new IllegalArgumentException("Subject is required.");
        if (!ValidationUtils.isNotEmpty(description)) throw new IllegalArgumentException("Description is required.");

        String id = dataManager.generateNextComplaintId();
        Complaint complaint = new Complaint(id, studentUser.getUserId(), subject.trim(),
                category != null ? category : "OTHER", description.trim(),
                DateUtils.todayString(), "PENDING", "Campus Administration", "");

        dataManager.getComplaints().put(id, complaint);
        dataManager.saveComplaints();
        dataManager.logAudit(studentUser.getUserId(), "SUBMIT_COMPLAINT", "Lodged complaint " + id + ": " + subject);
        return complaint;
    }

    public synchronized void updateComplaintStatus(User staffUser, String complaintId,
                                                   String newStatus, String handler,
                                                   String resolutionNotes) throws Exception {
        if (staffUser == null || staffUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Admin authorization required to resolve complaints.");
        }

        Complaint complaint = dataManager.getComplaints().get(complaintId);
        if (complaint == null) throw new IllegalArgumentException("Complaint not found.");

        complaint.setStatus(newStatus != null ? newStatus : complaint.getStatus());
        if (ValidationUtils.isNotEmpty(handler)) {
            complaint.setAssignedHandler(handler.trim());
        }
        if (resolutionNotes != null) {
            complaint.setResolutionNotes(resolutionNotes.trim());
        }

        dataManager.saveComplaints();
        dataManager.logAudit(staffUser.getUserId(), "UPDATE_COMPLAINT",
                "Updated complaint " + complaintId + " status to " + newStatus);
    }

    public long countPendingComplaints() {
        return dataManager.getComplaints().values().stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(c.getStatus()))
                .count();
    }

    public List<Complaint> searchComplaints(User viewer, String query, String statusFilter, String categoryFilter) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String status = statusFilter != null ? statusFilter.trim() : "All";
        String cat = categoryFilter != null ? categoryFilter.trim() : "All";

        return dataManager.getComplaints().values().stream()
                .filter(c -> {
                    if (viewer.getRole() == Role.STUDENT && !c.getStudentId().equalsIgnoreCase(viewer.getUserId())) {
                        return false;
                    }

                    boolean matchesQuery = q.isEmpty() ||
                            c.getComplaintId().toLowerCase().contains(q) ||
                            c.getSubject().toLowerCase().contains(q) ||
                            c.getDescription().toLowerCase().contains(q) ||
                            c.getStudentId().toLowerCase().contains(q);

                    boolean matchesStatus = status.equalsIgnoreCase("All") ||
                            c.getStatus().equalsIgnoreCase(status);

                    boolean matchesCat = cat.equalsIgnoreCase("All") ||
                            c.getCategory().equalsIgnoreCase(cat);

                    return matchesQuery && matchesStatus && matchesCat;
                })
                .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
                .collect(Collectors.toList());
    }
}
