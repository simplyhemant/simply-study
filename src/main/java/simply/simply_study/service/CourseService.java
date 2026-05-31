package simply.simply_study.service;

import org.springframework.data.domain.Pageable;
import simply.simply_study.dto.*;
import java.util.List;

public interface CourseService {
    CourseResponseDto createCourse(String teacherId, CreateCourseRequest request);
    List<CourseResponseDto> getAllCourses(Pageable pageable);
    default List<CourseResponseDto> getAllCourses() {
        return getAllCourses(Pageable.unpaged());
    }
    CourseResponseDto getCourseById(Long id);
    CourseDetailsResponseDto getCourseDetailsById(Long id, String timezone);
}
