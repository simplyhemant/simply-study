package simply.simply_study.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import simply.simply_study.dto.BookingResponseDto;
import simply.simply_study.dto.CreateBookingRequest;
import simply.simply_study.dto.SessionResponseDto;
import simply.simply_study.exception.ForbiddenException;
import simply.simply_study.exception.OfferingFullException;
import simply.simply_study.exception.ResourceNotFoundException;
import simply.simply_study.exception.ScheduleConflictException;
import simply.simply_study.model.*;
import simply.simply_study.model.enums.Role;
import simply.simply_study.repository.BookingRepository;
import simply.simply_study.repository.BookingSessionRepository;
import simply.simply_study.repository.OfferingRepository;
import simply.simply_study.repository.UserRepository;
import simply.simply_study.service.BookingService;
import simply.simply_study.service.TimezoneService;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSessionRepository bookingSessionRepository;
    private final OfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final TimezoneService timezoneService;

    @Override
    @Transactional
    public BookingResponseDto createBooking(CreateBookingRequest request) {
        log.info("Attempting to create booking for parentId: {}, offeringId: {}",
                request.parentId(), request.offeringId());

        User parent = userRepository.findByIdWithWriteLock(request.parentId())
                .orElseThrow(() -> {
                    log.warn("Booking failed: User not found with id: {}", request.parentId());
                    return new ResourceNotFoundException("Parent not found with id: " + request.parentId());
                });

        if (parent.getRole() != Role.PARENT) {
            log.warn("Booking failed: User {} is not a PARENT", request.parentId());
            throw new ForbiddenException("User is not a PARENT");
        }

        bookingSessionRepository.lockParentBookings(parent.getId());

        Offering offering = offeringRepository.findByIdWithWriteLock(request.offeringId())
                .orElseThrow(() -> {
                    log.warn("Booking failed: Offering not found with id: {}", request.offeringId());
                    return new ResourceNotFoundException("Offering not found with id: " + request.offeringId());
                });

        if (offering.getSessions() == null || offering.getSessions().isEmpty()) {
            log.warn("Booking failed: Offering {} has no sessions", offering.getId());
            throw new ScheduleConflictException("Cannot book an offering that has no sessions.");
        }

        boolean hasFutureSession = offering.getSessions().stream()
                .anyMatch(session -> session.getStartTime().isAfter(Instant.now()));
        if (!hasFutureSession) {
            log.warn("Booking failed: Offering {} has no sessions in the future", offering.getId());
            throw new ScheduleConflictException("Cannot book an offering that has no future sessions.");
        }

        if (bookingRepository.existsByParentIdAndOfferingId(parent.getId(), offering.getId())) {
            log.warn("Booking failed: Parent {} has already booked offering {}", parent.getId(), offering.getId());
            throw new ScheduleConflictException("Parent has already booked this offering.");
        }

        int rowsUpdated = offeringRepository.incrementEnrollment(offering.getId());
        if (rowsUpdated == 0) {
            log.warn("Booking failed: Offering {} has reached max capacity of {}", offering.getId(), offering.getMaxCapacity());
            throw new OfferingFullException("Offering has reached maximum capacity of " + offering.getMaxCapacity());
        }

        long conflictCount = bookingSessionRepository.countOverlappingWithOffering(
                parent.getId(), offering.getId());
        if (conflictCount > 0) {
            log.warn("Booking failed: Time overlap conflict detected for parent {} on offering {}",
                    parent.getId(), offering.getId());
            throw new ScheduleConflictException("Schedule conflict detected: one or more sessions overlap with your existing bookings.");
        }

        Booking booking = new Booking(offering, parent, Instant.now());
        Booking saved = bookingRepository.save(booking);

        List<BookingSession> bookingSessions = offering.getSessions().stream()
                .map(session -> new BookingSession(saved, session))
                .toList();
        bookingSessionRepository.saveAll(bookingSessions);

        log.info("Booking created successfully with id: {} for parentId: {}", saved.getId(), parent.getId());
        return mapToBookingResponse(saved, parent.getTimezone());
    }

    @Override
    public List<BookingResponseDto> getParentBookings(String parentId, String timezone) {
        log.info("Fetching bookings for parentId: {}, timezone: {}", parentId, timezone);

        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> {
                    log.warn("Fetching bookings failed: User not found with id: {}", parentId);
                    return new ResourceNotFoundException("Parent not found with id: " + parentId);
                });

        if (parent.getRole() != Role.PARENT) {
            log.warn("Fetching bookings failed: User {} is not a PARENT", parentId);
            throw new ForbiddenException("User is not a PARENT");
        }

        List<Booking> bookings = bookingRepository.findByParentIdWithDetails(parentId);
        String zone = (timezone != null && !timezone.isBlank()) ? timezone : parent.getTimezone();

        return bookings.stream()
                .map(booking -> mapToBookingResponse(booking, zone))
                .collect(Collectors.toList());
    }

    private BookingResponseDto mapToBookingResponse(Booking booking, String timezone) {
        List<SessionResponseDto> sessionDtos = booking.getOffering().getSessions().stream()
                .map(s -> new SessionResponseDto(
                        s.getId(),
                        booking.getOffering().getId(),
                        booking.getOffering().getTeacher().getId(),
                        timezoneService.formatToLocalString(s.getStartTime(), timezone),
                        timezoneService.formatToLocalString(s.getEndTime(), timezone)
                ))
                .collect(Collectors.toList());

        return new BookingResponseDto(
                booking.getId(),
                booking.getOffering().getId(),
                booking.getOffering().getCourse().getTitle(),
                booking.getOffering().getTeacher().getId(),
                booking.getBookedAt(),
                sessionDtos
        );
    }
}
