package simply.simply_study.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import simply.simply_study.model.enums.OfferingStatus;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offering",
    indexes = {
        @Index(name = "idx_offering_teacher_id", columnList = "teacher_id"),
        @Index(name = "idx_offering_course_id", columnList = "course_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "current_enrollment", nullable = false, columnDefinition = "integer default 0")
    private int currentEnrollment = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PUBLISHED'")
    private OfferingStatus status = OfferingStatus.PUBLISHED;

    @Version
    private Long version;

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime ASC")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<Session> sessions = new ArrayList<>();

    public Offering(Course course, User teacher, String title, String timezone, Integer maxCapacity) {
        this.course = course;
        this.teacher = teacher;
        this.title = title;
        this.timezone = timezone;
        this.maxCapacity = maxCapacity;
        this.currentEnrollment = 0;
        this.status = OfferingStatus.PUBLISHED;
    }

    public void addSession(Session session) {
        sessions.add(session);
        session.setOffering(this);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
        session.setOffering(null);
    }
}
