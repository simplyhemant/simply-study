package simply.simply_study.service;

import simply.simply_study.dto.BookingResponseDto;
import simply.simply_study.dto.CreateBookingRequest;
import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(CreateBookingRequest request);
    List<BookingResponseDto> getParentBookings(String parentId, String timezone);
}
