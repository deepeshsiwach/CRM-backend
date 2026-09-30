package ISFT.CRM.controller;

import ISFT.CRM.entity.Course;
import ISFT.CRM.service.CourseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id)
                .orElseThrow(() ->
                        new RuntimeException("Course not found"));
    }

    @PostMapping
    public Course createCourse(@RequestBody Course course) {
        return courseService.createCourse(course);
    }

    @PutMapping("/{id}")
    public Course updateCourse(
            @PathVariable Long id,
            @RequestBody Course courseDetails) {

        return courseService.updateCourse(id, courseDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(
            @PathVariable Long id) {

        try {

            courseService.deleteCourse(id);

            return ResponseEntity
                    .noContent()
                    .build();

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(409)
                    .body(Map.of(
                            "message",
                            e.getMessage()
                    ));
        }
    }
}