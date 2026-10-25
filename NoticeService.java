package service;

import model.Notice;
import model.User;
import security.AccessControl;
import storage.WebDataManager;
import util.DateUtils;
import util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NoticeService {

    private final WebDataManager dataManager;

    public NoticeService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<Notice> getAllNotices() {
        return dataManager.getNotices().values().stream()
                .sorted((a, b) -> b.getPublicationDate().compareTo(a.getPublicationDate()))
                .collect(Collectors.toList());
    }

    public List<Notice> getActiveNotices() {
        return dataManager.getNotices().values().stream()
                .filter(Notice::isActive)
                .sorted((a, b) -> b.getPublicationDate().compareTo(a.getPublicationDate()))
                .collect(Collectors.toList());
    }

    public List<Notice> getRecentNotices(int count) {
        return getActiveNotices().stream().limit(count).collect(Collectors.toList());
    }

    public synchronized Notice createNotice(User author, String title, String category,
                                            String description) throws Exception {
        if (!AccessControl.canPublishNotices(author)) {
            throw new SecurityException("Permission denied to publish notices.");
        }
        if (!ValidationUtils.isNotEmpty(title)) throw new IllegalArgumentException("Title is required.");
        if (!ValidationUtils.isNotEmpty(description)) throw new IllegalArgumentException("Description is required.");

        String id = dataManager.generateNextNoticeId();
        Notice notice = new Notice(id, title.trim(), category != null ? category : "GENERAL",
                DateUtils.todayString(), description.trim(), author.getFullName(), "ACTIVE");

        dataManager.getNotices().put(id, notice);
        dataManager.saveNotices();
        dataManager.logAudit(author.getUserId(), "CREATE_NOTICE", "Published notice " + id + ": " + title);
        return notice;
    }

    public synchronized void updateNotice(User editor, Notice notice) throws Exception {
        if (!AccessControl.canPublishNotices(editor)) throw new SecurityException("Permission denied.");
        if (notice == null || !dataManager.getNotices().containsKey(notice.getNoticeId())) {
            throw new IllegalArgumentException("Notice not found.");
        }
        if (!ValidationUtils.isNotEmpty(notice.getTitle())) throw new IllegalArgumentException("Title is required.");

        dataManager.getNotices().put(notice.getNoticeId(), notice);
        dataManager.saveNotices();
        dataManager.logAudit(editor.getUserId(), "UPDATE_NOTICE", "Updated notice " + notice.getNoticeId());
    }

    public synchronized void archiveNotice(User actor, String noticeId) throws Exception {
        if (!AccessControl.canPublishNotices(actor)) throw new SecurityException("Permission denied.");
        Notice n = dataManager.getNotices().get(noticeId);
        if (n == null) throw new IllegalArgumentException("Notice not found.");

        n.setStatus("ARCHIVED");
        dataManager.saveNotices();
        dataManager.logAudit(actor.getUserId(), "ARCHIVE_NOTICE", "Archived notice " + noticeId);
    }

    public List<Notice> searchNotices(String query, String categoryFilter) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String cat = categoryFilter != null ? categoryFilter.trim() : "All";

        return dataManager.getNotices().values().stream()
                .filter(n -> {
                    boolean matchesQuery = q.isEmpty() ||
                            n.getTitle().toLowerCase().contains(q) ||
                            n.getDescription().toLowerCase().contains(q) ||
                            n.getPublishedBy().toLowerCase().contains(q);

                    boolean matchesCat = cat.equalsIgnoreCase("All") ||
                            n.getCategory().equalsIgnoreCase(cat);

                    return matchesQuery && matchesCat;
                })
                .sorted((a, b) -> b.getPublicationDate().compareTo(a.getPublicationDate()))
                .collect(Collectors.toList());
    }
}
