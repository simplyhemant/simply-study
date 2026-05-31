package simply.simply_study.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking_session",
    uniqueConstraints = @UniqueConstraint(name = "uq_booking_session", columnNames = {"booking_id", "session_id"}),
    indexes = {
        @Index(name = "idx_bs_booking_id", columnList = "booking_id"),
        @Index(name = "idx_bs_session_id", columnList = "session_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class BookingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    public BookingSession(Booking booking, Session session) {
        this.booking = booking;
        this.session = session;
    }
}
