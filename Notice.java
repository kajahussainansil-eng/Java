package model;

import java.io.Serializable;

public class Notice implements Serializable {
    private static final long serialVersionUID = 1L;

    private String noticeId;
    private String title;
    private String category; // ACADEMIC, EVENT, GENERAL, URGENT, EXAM
    private String publicationDate; // YYYY-MM-DD
    private String description;
    private String publishedBy;
    private String status; // ACTIVE, ARCHIVED

    public Notice() {
        this.category = "GENERAL";
        this.status = "ACTIVE";
    }

    public Notice(String noticeId, String title, String category,
                  String publicationDate, String description, String publishedBy, String status) {
        this.noticeId = noticeId;
        this.title = title;
        this.category = category != null ? category : "GENERAL";
        this.publicationDate = publicationDate;
        this.description = description;
        this.publishedBy = publishedBy;
        this.status = (status != null && !status.isEmpty()) ? status : "ACTIVE";
    }

    public String getNoticeId() { return noticeId; }
    public void setNoticeId(String noticeId) { this.noticeId = noticeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPublicationDate() { return publicationDate; }
    public void setPublicationDate(String publicationDate) { this.publicationDate = publicationDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "[" + category + "] " + title;
    }
}
