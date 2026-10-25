package model;

import java.io.Serializable;

public class Complaint implements Serializable {
    private static final long serialVersionUID = 1L;

    private String complaintId;
    private String studentId;
    private String subject;
    private String category; // HOSTEL, ACADEMIC, INFRASTRUCTURE, CANTEEN, LIBRARY, OTHER
    private String description;
    private String submissionDate; // YYYY-MM-DD
    private String status; // PENDING, IN_PROGRESS, RESOLVED, REJECTED
    private String assignedHandler;
    private String resolutionNotes;

    public Complaint() {
        this.status = "PENDING";
        this.category = "INFRASTRUCTURE";
        this.assignedHandler = "Campus Administration";
        this.resolutionNotes = "";
    }

    public Complaint(String complaintId, String studentId, String subject,
                     String category, String description, String submissionDate,
                     String status, String assignedHandler, String resolutionNotes) {
        this.complaintId = complaintId;
        this.studentId = studentId;
        this.subject = subject;
        this.category = category != null ? category : "INFRASTRUCTURE";
        this.description = description;
        this.submissionDate = submissionDate;
        this.status = (status != null && !status.isEmpty()) ? status : "PENDING";
        this.assignedHandler = (assignedHandler != null && !assignedHandler.isEmpty()) ? assignedHandler : "Campus Administration";
        this.resolutionNotes = resolutionNotes != null ? resolutionNotes : "";
    }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(String submissionDate) { this.submissionDate = submissionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedHandler() { return assignedHandler; }
    public void setAssignedHandler(String assignedHandler) { this.assignedHandler = assignedHandler; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "[" + status + "] " + subject + " (" + studentId + ")";
    }
}
