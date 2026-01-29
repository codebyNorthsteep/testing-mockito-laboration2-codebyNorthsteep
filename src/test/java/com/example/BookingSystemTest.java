package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;



/**
 * Unit test suite for {@link BookingSystem}.
 * * Verifies the core business logic including room reservations, availability searches,
 * and cancellation procedures. Dependencies are mocked to ensure isolated testing
 * of the booking logic.
 */
//I de test som fungerar så gör jag en stub av ett objekt i testet, de som ej har det blir röda och mockito kastar en stubbingexeption
//Gör två inre klasser @Nestled - Nestled classes
//@MockitoSettings - Innehåller @ExtendWith plus andra inställningar tex lenient()
@ExtendWith(MockitoExtension.class)
class BookingSystemTest {

    @Mock
    private TimeProvider timeProvider;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private NotificationService notificationService;

    //SUT, System Under Test
    @InjectMocks
    private BookingSystem bookingSystem;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 20, 10, 0);

    /**
     * Operational flow tests for the BookingSystem.
     * Focuses on both successful and unsuccessfull execution paths and complex business rules.
     */
@Nested
public class BookingSystemFlowTests{
    @BeforeEach
    void setUp() {
// Fixes the system time to ensure consistent behavior for time-sensitive logic, this due to a StubbingException thrown
        when(timeProvider.getCurrentTime()).thenReturn(now);
    }

// --- Tests for bookRoom ---
        /**
         * Verifies that a room is correctly booked when provided with valid input.
         * <p>
         * Setup: Mock the booking of a room.
         * Expectation: Method returns true, the room is saved to roomRepository,
         * and a confirmation email is triggered(NotificationService).
         */
    @Test
    void book_a_room_with_valid_credentials() throws NotificationException{
        //Arrange
        String roomId = "room1";
        LocalDateTime startDate = now.plusDays(1);
        LocalDateTime endDate = now.plusDays(2);
        Room room = new Room(roomId, "Presidential suite");

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        //Act
        boolean result = bookingSystem.bookRoom(roomId, startDate, endDate);

        //Assert
        assertThat(result).isTrue();
        verify(roomRepository).save(room);
        verify(notificationService).sendBookingConfirmation(any());


    }

        /**
         * Ensures that booking persistence remains successful even if the notification service fails.
         * <p>
         * Logic: The primary booking action should not be rolled back due to secondary notification errors.
         * Assert and verify: That the booking was made and that the room was saved in the RoomRepository.
         */
    @Test
    void book_room_even_if_notification_fails() throws NotificationException {
        //Arrange
        String roomId = "Room1";
        Room room = new Room(roomId, "Presidential Suite");

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        doThrow(new NotificationException("Error sending notification")).when(notificationService).sendBookingConfirmation(any());

        //Act
        boolean result = bookingSystem.bookRoom(roomId, now.plusDays(1),now.plusDays(2));

        //Assert
        assertThat(result).isTrue();
        verify(roomRepository).save(room);

    }

        /**
         * Verifies that the system prevents bookings where the end time is before the start time.
         * <p>
         * Expectation: Throws IllegalArgumentException with a message and stops execution before saving or notifying.
         * Verifies that the room is never booked.
         */
    @Test
    void book_a_room_with_invalid_dates_end_before_start_should_throw_exception() throws NotificationException {
        //Arrange - överflödig men bra för att se hela strukturen av denna mock
        String roomId = "room1";
        LocalDateTime startDate = now.plusDays(2);
        LocalDateTime endDate = now.plusDays(1);

        //Act + Assert
        assertThatThrownBy(()->
                bookingSystem.bookRoom(roomId, startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Sluttid måste vara efter starttid");
       //Assert
        verify(notificationService, never())
                .sendBookingConfirmation(any());
        verify(roomRepository, never())
                .save(any());
    }



        /**
         * Verifies that the system rejects booking requests for dates that have already passed.
         * Expectation: Throws IllegalArgumentException with a message and stops execution before saving or notifying.
         * Verifies that the room is never booked.
         */
    @Test
    void book_a_room_with_start_date_before_today_should_throw_exception() throws NotificationException {
        //Arrange, överflödig men bra för att se hela strukturen av denna mock
        String roomId = "room1";
        LocalDateTime startDate = now.minusDays(1);
        LocalDateTime endDate = now.plusDays(1);


        //act + assert
        assertThatThrownBy(()->
                bookingSystem.bookRoom(roomId, startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kan inte boka tid i dåtid");
        //Assert
        verify(notificationService, never())
                .sendBookingConfirmation(any());
        verify(roomRepository, never())
                .save(any());
    }

        /**
         * Verifies that an attempt to book a non-existent room ID results in an appropriate exception.
         * Expectation: Throws IllegalArgumentException with a message and stops execution before saving or notifying.
         * Verifies that the room is never booked.
         */
    @Test
    void book_a_room_when_room_not_existing_should_throw_exception() throws NotificationException {
        //arrange - överflödig men bra för att se hela strukturen av denna mock
        String roomId = "room1";
        LocalDateTime startDate = now.plusDays(2);
        LocalDateTime endDate = now.plusDays(3);

        //act + assert
        assertThatThrownBy(()->
                bookingSystem.bookRoom(roomId, startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rummet existerar inte");
        //Assert
        verify(notificationService, never())
                .sendBookingConfirmation(any());
        verify(roomRepository, never())
                .save(any());
    }

        /**
         * Confirms that a booking request returns false if the room is already booked for the given interval.
         * Verifies that the room is never booked.
         */
    @Test
    void book_room_if_room_occupied_should_return_false() {
        //Arrange
        String roomId = "Room1";
        String bookingId = "B849587";
        LocalDateTime startDate = now.plusDays(1);
        LocalDateTime endDate = now.plusDays(2);
        Room room = new Room(roomId, "Presidential Suite");
        room.addBooking(new Booking(bookingId, roomId, startDate, endDate));

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        //Act
        boolean result = bookingSystem.bookRoom(roomId, startDate, endDate);

        //Assert
        assertThat(result).isFalse();
        verify(roomRepository,never()).save(any());
    }


    // --- Tests for cancelBooking ---


        /**
         * Mocks a booking for the RoomRepository.
         * Verifies the successful cancellation of that existing future booking.
         * <p>
         * Expectation: Booking is removed from the room, room state is persisted, and user is notified.
         */
    @Test
    void cancel_booking_with_valid_id() throws NotificationException {
        //Arrange
        String bookingId = "B4567";
        String roomId = "Room1";
        LocalDateTime startDate = now.plusDays(1);
        LocalDateTime endDate = now.plusDays(2);

        Booking booking = new Booking(bookingId, roomId, startDate, endDate);
        Room room = new Room(roomId, "Ocean Suite");
        room.addBooking(booking);

        when(roomRepository.findAll()).thenReturn(List.of(room));

        //Act
        boolean result = bookingSystem.cancelBooking(bookingId);

        //Assert
        assertThat(result).isTrue();
        verify(roomRepository).save(room);
        verify(notificationService).sendCancellationConfirmation(booking);

    }


        /**
         * Mocks a booking for the RoomRepository.
         * Ensures that bookings currently in progress or already completed cannot be canceled.
         * <p>
         * Expectation: Throws IllegalStateException as cancellation is only permitted for future stays.
         */
    @Test
    void cancel_booking_during_or_after_stay_should_throw_exception(){
        //Arrange
        String bookingId = "B4567";
        String roomId = "Room1";
        LocalDateTime startDate = now.minusDays(1);
        LocalDateTime endDate = now.plusDays(2);

        Booking booking = new Booking(bookingId, roomId, startDate, endDate);
        Room room = new Room(roomId, "Ocean Suite");
        room.addBooking(booking);

        when(roomRepository.findAll()).thenReturn(List.of(room));

        //Act + Assert
        assertThatThrownBy(()->
                bookingSystem.cancelBooking(bookingId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kan inte avboka påbörjad eller avslutad bokning");

        //verify()?
        verify(roomRepository, never()).save(room);

    }
        /**
         * Ensures that cancelling persistence remains successful even if the notification service fails.
         * <p>
         * Logic: The primary cancelling action should not be rolled back due to secondary notification errors.
         * Assert and verify: That the booking was canceled and that the room was deleted in the RoomRepository.
         */
    @Test
    void cancel_booking_even_if_notification_fails() throws NotificationException {
        //Arrange
        String bookingId = "B5342";
        String roomId = "Room1";
        LocalDateTime startDate = now.plusDays(1);
        LocalDateTime endDate = now.plusDays(2);

        Booking booking = new Booking(bookingId, roomId, startDate, endDate);
        Room room = new Room(roomId, "Presidential Suite");
        room.addBooking(booking);

        when(roomRepository.findAll()).thenReturn(List.of(room));
        doThrow(new NotificationException("Error sending notification")).when(notificationService).sendCancellationConfirmation(any());

        //Act
        boolean result = bookingSystem.cancelBooking(bookingId);

        //Assert
        assertThat(result).isTrue();
        verify(roomRepository).save(room);
    }

}
    /**
     * Input validation and availability check tests.
     * Verifies how the system handles null inputs and filtering of available rooms.
     */
    @Nested
    public class BookingSystemTestForValidation {

        //--- Tests for bookRoom ---

        /** Using ParameterizedTest for checking multiple scenarios.
         * Validates that the booking method rejects null values for room ID or timestamps.
         */
        @ParameterizedTest
        @MethodSource("nullCheckArguments")
        void nullCheck(String roomId, LocalDateTime startDate, LocalDateTime endDate) throws NotificationException {
            assertThatThrownBy(()->
                    bookingSystem.bookRoom(roomId, startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bokning kräver giltiga start- och sluttider samt rum-id");
            //Asset
            verify(notificationService, never())
                    .sendBookingConfirmation(any());
            verify(roomRepository, never())
                    .save(any());
        }

        static Stream<Arguments> nullCheckArguments () {
            LocalDateTime now = LocalDateTime.of(2026, 1, 20, 10, 0);
            return Stream.of(
                    Arguments.of("Room1",null,now.plusDays(2)),
                    Arguments.of(null, now.plusDays(1), now.plusDays(2)),
                    Arguments.of("Room1", now.plusDays(1), null)

            );
        }


        //--- Tests for getAvailableRooms ---

        /** Mocks two rooms, one of which is occupied to ensure that the room is
         * not shown during search.
         * Verifies that availability search correctly filters out rooms with conflicting bookings.
         */
        @Test
        void get_available_rooms_with_valid_credentials() {

            // Arrange
            LocalDateTime startDate = now.plusDays(1);
            LocalDateTime endDate = now.plusDays(1).plusHours(1);

            Room room1 = new Room("room1", "Ledigt rum");
            Room room2 = new Room("room2", "Upptaget rum");

            //För att simulera ett redan bokat rum, för verifiering av att det rummet ej dyker upp i listan
            room2.addBooking(new Booking("B2435531", "room2", startDate, endDate));

            when(roomRepository.findAll()).thenReturn(List.of(room1, room2));

            // Act
            List<Room> result = bookingSystem.getAvailableRooms(startDate, endDate);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(room1);

        }

        /**
         * Ensures availability checks require valid chronological time ranges.
         * Expectation: Throws IllegalArgumentException with a message.
         */
        @Test
        void get_available_rooms_with_end_before_start_date_should_throw_exception() {
            //Arrange
            LocalDateTime startDate = now.plusDays(2);
            LocalDateTime endDate = now.plusDays(1);

            //Act + Assert
            assertThatThrownBy(()->
                    bookingSystem.getAvailableRooms(startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sluttid måste vara efter starttid");
        }

        /** Using ParameterizedTest for checking multiple scenarios.
         * Validates that the getAvailableRooms method rejects null values for timestamps.
         * Expectation: Throws IllegalArgumentException with a message.
         */
        @ParameterizedTest
        @MethodSource("nullCheckArgumentsForDates")
        void get_available_rooms_with_invalid_start_or_end_date_should_throw_exception(LocalDateTime startDate, LocalDateTime endDate) {

            //Act + Assert
            assertThatThrownBy(()->
                    bookingSystem.getAvailableRooms(startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Måste ange både start- och sluttid");
        }

        static Stream<Arguments> nullCheckArgumentsForDates () {
            LocalDateTime now = LocalDateTime.of(2026, 1, 20, 10, 0);
            return Stream.of(
                    Arguments.of(null,now.plusDays(2)),
                    Arguments.of(now.plusDays(1), null)

            );
        }


        //--- Tests for cancelBooking ---
        /**
         * Confirms that attempting to cancel a booking with an unknown ID returns false.
         *
         */
        @Test
        void cancel_room_if_booking_id_not_exists_should_return_false() {
            //Arrange
            String bookingId = "B2884";
            Room room = new Room("Room 1", "Ocean Suite");
            when(roomRepository.findAll()).thenReturn(List.of(room));

            //Act
            boolean result = bookingSystem.cancelBooking(bookingId);

            //Assert
            assertThat(result).isFalse();
            //verify()? För att kontrollera att inget rum avbokades
            verify(roomRepository, never()).save(room);

        }

        /**
         * Verifies that cancellation logic rejects null as a booking ID.
         * Expectation: Throws IllegalArgumentException with a message.
         */
        @Test
        void cancel_booking_with_invalid_booking_id_should_throw_exception() {

            //Act + Assert
            assertThatThrownBy(()->
                    bookingSystem.cancelBooking(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Boknings-id kan inte vara null");

        }


    }

}