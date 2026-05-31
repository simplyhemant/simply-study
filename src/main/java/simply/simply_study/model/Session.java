package simply.simply_study.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "session",
    indexes = {
        @Index(name = "idx_session_offering_id", columnList = "offering_id"),
        @Index(name = "idx_session_time_range", columnList = "start_time, end_time")
    })
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private Offering offering;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant endTime;

    public Session(Offering offering, Instant startTime, Instant endTime) {
        this.offering = offering;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
