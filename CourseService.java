package service;

import model.Course;
import model.Faculty;
import storage.WebDataManager;
import util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CourseService {

    private final WebDataManager dataManager;

    public CourseService(WebDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public List<Course> getAllCourses() {
        return new ArrayList<>(dataManager.getCourses().values());
    }

    public Course getCourse(String code) {
        if (code == null) return null;
        return dataManager.getCourses().get(code.toUpperCase().trim());
    }

    public synchronized void createCourse(Course course) throws Exception {
        if (course == null) throw new IllegalArgumentException("Course cannot be null.");
        String code = course.getCourseCode();
        if (!ValidationUtils.isValidCourseCode(code)) {
            throw new IllegalArgumentException("Invalid course code format (e.g. CS101, IT205).");
        }
        if (dataManager.getCourses().containsKey(code.toUpperCase())) {
            throw new IllegalArgumentException("Course code " + code + " already exists.");
        }
        if (!ValidationUtils.isNotEmpty(course.getCourseName())) {
            throw new IllegalArgumentException("Course name is required.");
        }
        if (!ValidationUtils.isNotEmpty(course.getDepartment())) {
            throw new IllegalArgumentException("Department is required.");
        }
        if (course.getCredits() < 1 || course.getCredits() > 6) {
            throw new IllegalArgumentException("Credits must be between 1 and 6.");
        }
        if (course.getSemester() < 1 || course.getSemester() > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }

        String facId = course.getAssignedFacultyId();
        if (ValidationUtils.isNotEmpty(facId)) {
            Faculty fac = dataManager.getFaculties().get(facId.toUpperCase());
            if (fac == null) {
                throw new IllegalArgumentException("Assigned faculty ID " + facId + " does not exist.");
            }
        }

        course.setCourseCode(code.toUpperCase());
        dataManager.getCourses().put(course.getCourseCode(), course);
        dataManager.saveCourses();
        dataManager.logAudit("ADMIN", "CREATE_COURSE", "Created course " + course.getCourseCode() + " - " + course.getCourseName());
    }

    public synchronized void updateCourse(Course course) throws Exception {
        if (course == null || !dataManager.getCourses().containsKey(course.getCourseCode().toUpperCase())) {
            throw new IllegalArgumentException("Course does not exist.");
        }
        if (!ValidationUtils.isNotEmpty(course.getCourseName())) {
            throw new IllegalArgumentException("Course name is required.");
        }
        if (course.getCredits() < 1 || course.getCredits() > 6) {
            throw new IllegalArgumentException("Credits must be between 1 and 6.");
        }

        String facId = course.getAssignedFacultyId();
        if (ValidationUtils.isNotEmpty(facId)) {
            Faculty fac = dataManager.getFaculties().get(facId.toUpperCase());
            if (fac == null) {
                throw new IllegalArgumentException("Assigned faculty ID " + facId + " does not exist.");
            }
        }

        dataManager.getCourses().put(course.getCourseCode().toUpperCase(), course);
        dataManager.saveCourses();
        dataManager.logAudit("ADMIN", "UPDATE_COURSE", "Updated course " + course.getCourseCode());
    }

    public synchronized void archiveCourse(String courseCode) throws Exception {
        Course c = dataManager.getCourses().get(courseCode.toUpperCase());
        if (c == null) throw new IllegalArgumentException("Course not found.");

        c.setStatus("ARCHIVED");
        dataManager.saveCourses();
        dataManager.logAudit("ADMIN", "ARCHIVE_COURSE", "Archived course " + courseCode);
    }

    public List<Course> getCoursesByFaculty(String facultyId) {
        if (facultyId == null) return new ArrayList<>();
        return dataManager.getCourses().values().stream()
                .filter(c -> facultyId.equalsIgnoreCase(c.getAssignedFacultyId()))
                .collect(Collectors.toList());
    }

    public List<Course> searchCourses(String query, String deptFilter) {
        String q = query != null ? query.trim().toLowerCase() : "";
        String dept = deptFilter != null ? deptFilter.trim() : "All";

        return dataManager.getCourses().values().stream()
                .filter(c -> {
                    boolean matchesQuery = q.isEmpty() ||
                            c.getCourseCode().toLowerCase().contains(q) ||
                            c.getCourseName().toLowerCase().contains(q) ||
                            c.getDepartment().toLowerCase().contains(q);

                    boolean matchesDept = dept.equalsIgnoreCase("All") ||
                            c.getDepartment().equalsIgnoreCase(dept);

                    return matchesQuery && matchesDept;
                })
                .sorted((a, b) -> a.getCourseCode().compareTo(b.getCourseCode()))
                .collect(Collectors.toList());
    }
}
