package ISFT.CRM.service;


import ISFT.CRM.entity.Course;
import ISFT.CRM.repository.CourseRepository;
import ISFT.CRM.repository.CampaignRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CampaignRepository campaignRepository;

    public CourseService(
            CourseRepository courseRepository,
            CampaignRepository campaignRepository) {

        this.courseRepository = courseRepository;
        this.campaignRepository = campaignRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    public Course createCourse(Course course) {
        if (course.getCourseName() == null ||
                course.getCourseName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Course name is required");
        }

        if (course.getDurationMonths() == null ||
                course.getDurationMonths() <= 0) {

            throw new IllegalArgumentException(
                    "Course duration must be greater than 0 months");
        }
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, Course courseDetails) {
        if (courseDetails.getCourseName() == null ||
                courseDetails.getCourseName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Course name is required");
        }

        if (courseDetails.getDurationMonths() == null ||
                courseDetails.getDurationMonths() <= 0) {

            throw new IllegalArgumentException(
                    "Course duration must be greater than 0 months");
        }

        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Course not found"));

        existingCourse.setCourseName(courseDetails.getCourseName());
        existingCourse.setDescription(courseDetails.getDescription());
        existingCourse.setDurationMonths(courseDetails.getDurationMonths());
        existingCourse.setStatus(courseDetails.getStatus());

        return courseRepository.save(existingCourse);
    }

    public void deleteCourse(Long id) {

        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found");
        }

        long campaignCount =
                campaignRepository.countByCourseId(id);

        if (campaignCount > 0) {

            throw new IllegalStateException(
                    "Cannot delete course because it is used by "
                            + campaignCount
                            + " campaign(s)."
            );
        }

        courseRepository.deleteById(id);
    }
}