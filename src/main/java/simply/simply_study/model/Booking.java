package simply.simply_study.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import simply.simply_study.model.enums.BookingStatus;

import java.time.Instant;

@Entity
@Table(name = "booking",
    uniqueConstraints = @UniqueConstraint(name = "uq_parent_offering", columnNames = {"parent_id", "offering_id"}),
    indexes = {
        @Index(name = "idx_booking_parent_id", columnList = "parent_id"),
        @Index(name = "idx_booking_offering_id", columnList = "offering_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private Offering offering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "booked_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant bookedAt;

    public Booking(Offering offering, User parent, Instant bookedAt) {
        this.offering = offering;
        this.parent = parent;
        this.bookedAt = bookedAt;
        this.status = BookingStatus.CONFIRMED;
    }
}
