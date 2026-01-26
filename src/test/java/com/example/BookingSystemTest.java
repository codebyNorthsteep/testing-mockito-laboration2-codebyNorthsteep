package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//todo: dokumentera testfallen med javadocs


/**
 * Unit tests for {@link BookingSystem}.
 * The test suite verifies the core behaviors of the booking system:

 *   Successful room bookings when a valid time window and existing room are provided.
 *   Graceful handling of notification failures without affecting the booking outcome.
 *   Proper validation of start/end times, ensuring that the end is after start,
 *       both times are non‑null, and bookings cannot be made in the past.
 *   Detection of attempts to book a non‑existent room.
 *   Retrieval of available rooms for a requested time interval, excluding
 *       rooms that already have conflicting bookings.
 *   Validation logic for querying available rooms, mirroring the rules used in booking.
 *   Cancellation of bookings and related side‑effects such as notification.

 * Mock objects are configured with {@link MockitoSettings} and lenient stubbing
 * to avoid unnecessary exception failures when a test case terminates early.
 */
@MockitoSettings
class BookingSystemTest {

    @Mock
    private TimeProvider timeProvider;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private NotificationService notificationService;

    //SUT, System Under Test
    private BookingSystem bookingSystem;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 20, 10, 0);


    /**
     * Configures the test environment before each test case.
     * Puts the mocked in-parameters for class BookingSystems constructor.
     * Uses lenient() for the timeProvider because some test cases (e.g., with invalid input)
     * are aborted before the clock needs to be invoked, which would otherwise throw an
     * UnnecessaryStubbingException.
     */
    @BeforeEach
    void setUp() {
        bookingSystem = new BookingSystem(timeProvider, roomRepository, notificationService);
        //Nu är alltid den fixerade tiden now, lenient() för att undvika tidiga exceptions från Mockito
        lenient().when(timeProvider.getCurrentTime()).thenReturn(now);
    }


// --- Tests for bookRoom ---

    /**
     * Validates that a room can be successfully booked when all input parameters are correct.

     * The test sets up a mock of RoomRepository to return an existing Room
     * for the supplied roomId. It then calls BookingSystems method bookRoom(String, LocalDateTime, LocalDateTime)
     * with a start time one day in the future and an end time two days in the future.

     * Assertions verify that:
     * The method returns true, indicating a successful booking.
     * The repository's {}save method is invoked with the updated room instance.
     * A confirmation notification is sent via the NotificationService method sendBookingConfirmation (Booking).

     * This test confirms that under normal circumstances the booking workflow
     * persists the new booking and triggers a notification without throwing exceptions.
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
        verify(notificationService).sendBookingConfirmation(any(Booking.class));


    }
    /**
     * Verifies that the BookingSystem method bookRoom(String, LocalDateTime, LocalDateTime)
     * persists a booking even when the notification service throws an exception.

     * The test sets up a mock room repository to return a specific Room instance,
     * configures the notification service to throw NotificationException,
     * and then calls bookRoom. It asserts that the method returns true
     * and that the repository's save method is invoked, confirming that
     * the booking operation continues despite notification failures. */
    // Fortsätt även om notifieringen misslyckas
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
     * Verifies that BookingSystem method bookRoom(String, LocalDateTime, LocalDateTime)
     * throws an IllegalArgumentException when the supplied end time is before
     * the start time. The test supplies a valid room identifier and two code
     * LocalDateTime instances where the second one precedes the first. It asserts
     * that the exception message contains the phrase "Sluttid måste vara efter starttid".

     * Additionally, the test confirms that no booking confirmation is sent and
     * that the room repository’s method save is never invoked, ensuring that
     * the invalid input prevents any persistence or notification side effects.
     */
    //"Sluttid måste vara efter starttid"
    @Test
    void book_a_room_with_invalid_dates_end_before_start_should_throw_exception() throws NotificationException {
        //Arrange - överflödig men bra för att se hela strukturen av denna mock
        String roomId = "room1";
        LocalDateTime startDate = now.plusDays(2);
        LocalDateTime endDate = now.plusDays(1);
        //Room room = new Room(roomId, "Ocean suite");

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
     * Verifies that attempting to book a room with an invalid start and/or end time results in an
     * {@link IllegalArgumentException}. The test supplies a {@code null} end time (while the start time is
     * valid) and expects the booking system to reject the request before any side effects occur.
     *
     * <p>The method under test, {@link BookingSystem#bookRoom(String, LocalDateTime,
     * LocalDateTime)}, should throw an exception with a message containing
     * <em>Bokning kräver giltiga start- och sluttider samt rum-id</em>. The test also confirms that
     * no notification is sent and that the room repository's {@code save} method is never invoked.
     *
     * @throws NotificationException if sending a booking confirmation fails (not relevant for this test)
     */
    //"Bokning kräver giltiga start- och sluttider samt rum-id"
    @Test
    void book_a_room_with_invalid_start_and_or_end_time_should_throw_exception() throws NotificationException {
        //arrange - överflödig men bra för att se hela strukturen av denna mock
        String roomId = "room1";
        LocalDateTime startDate = now.plusDays(2);
        LocalDateTime endDate = null;
        //Room room = new Room(roomId, "Beach house");

        //Act + Assert
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

    //"Kan inte boka tid i dåtid"
    @Test
    void book_a_room_with_start_date_before_today_should_throw_exception() throws NotificationException {
        //Arrange, överflödig men bra för att se hela strukturen av denna mock
        String roomId = "room1";
        LocalDateTime startDate = now.minusDays(1);
        LocalDateTime endDate = now.plusDays(1);
        //Room room = new Room(roomId, "Family room");

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

    //"Rummet existerar inte"
    @Test
    void book_a_room_when_room_not_existing_should_throw_exception() throws NotificationException {
        //arrange - överflödig men bra för att se hela strukturen av denna mock
        String roomId = "room1";
        LocalDateTime startDate = now.plusDays(2);
        LocalDateTime endDate = now.plusDays(3);
        //Room room = new Room(roomId, "King suite");

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

    //!room.isAvailable(startTime, endTime)
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

    // --- Tests for getAvailableRooms ---


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
            assertThat(result).doesNotContain(room2);

    }

    //test för att se om tillgång finns för att kunna se rum

    //"Måste ange både start- och sluttid"
    @Test
    void get_available_rooms_with_invalid_credentials_should_throw_exception() {
        //Arrange
        LocalDateTime startDate = null;
        LocalDateTime endDate = now.plusDays(2);

        //Act + Assert
        assertThatThrownBy(()->
                bookingSystem.getAvailableRooms(startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Måste ange både start- och sluttid");
    }

    //"Sluttid måste vara efter starttid"
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

    // --- Tests for cancelBooking ---


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

    //"Boknings-id kan inte vara null"
    @Test
    void cancel_booking_with_invalid_id_should_throw_exception() {
        //Arrange
        String bookingId = null;

        //Act + Assert
        assertThatThrownBy(()->
                bookingSystem.cancelBooking(bookingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Boknings-id kan inte vara null");

    }

    //"Kan inte avboka påbörjad eller avslutad bokning"
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

    }
    // Fortsätt även om notifieringen misslyckas
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

    //(roomWithBooking.isEmpty())
    @Test
    void cancel_room_if_bookingId_not_exists_should_return_false() {
        //Arrange
        String bookingId = "B2884";
        Room room = new Room("Room 1", "Ocean Suite");
        when(roomRepository.findAll()).thenReturn(List.of(room));

        //Act
        boolean result = bookingSystem.cancelBooking(bookingId);

        //Assert
        assertThat(result).isFalse();

    }

}