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
     * IllegalArgumentException. The test supplies an end time containing null (while the start time is
     * valid) and expects the booking system to reject the request before any side effects occur.

     * The method under test, BookingSystem method bookRoom(String, LocalDateTime,
     * LocalDateTime), should throw an exception with a message containing
     * "Bokning kräver giltiga start-och sluttider samt rum-id". The test also confirms that
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

    /**
     * Validates that BookingSystem method bookRoom(String, LocalDateTime, LocalDateTime) rejects a booking
     * request whose start time is in the past.

     * The test constructs a startDate that is one day before the current time and an endDate
     * that lies in the future. It then verifies that invoking bookRoom throws an
     * IllegalArgumentException with a message containing the Swedish phrase
     * “Kan inte boka tid i dåtid”.

     * The test also ensures that no side effects occur: the notification service is not called and the room
     * repository’s save method is never invoked.
     */
    //"Kan inte boka tid i dåtid"
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
     * Verifies that attempting to book a non‑existent room results in an IllegalArgumentException.

     * The test supplies a roomId that is not present in the mocked RoomRepository.  When
     * BookingSystem method bookRoom(String, LocalDateTime, LocalDateTime) is invoked,
     * it should throw an exception whose message contains "Rummet existerar inte".

     * Additionally, the test asserts that no side effects occur: the notification service must not be
     *  called, and the repository’s RoomRepository method save(Room) method must never be invoked.
     *
     * @throws NotificationException if a booking confirmation could not be sent (not relevant for this test)
     */
    //"Rummet existerar inte"
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
     * Verifies that BookingSystem method bookRoom(String, LocalDateTime, LocalDateTime) correctly
     * refuses to create a booking when the requested time slot is already taken.

     * The test sets up a Room with an existing Booking that overlaps the
     * requested interval.  When bookRoom() is called, it should return false
     * and leave the repository untouched – no call to RoomRepository method save(Room) must
     * occur.
     */
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


    /**
     * Tests that BookingSystem method getAvailableRooms(LocalDateTime, LocalDateTime) returns only rooms that are not booked
     * during the requested time interval.

     * The test sets up two rooms: one free and one already booked for the given period. It verifies that the
     * method correctly filters out the occupied room and returns a list containing solely the available room.
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
            assertThat(result).doesNotContain(room2);

    }

    //test för att se om tillgång finns för att kunna se rum

    /**
     * Verifies that BookingSystem method getAvailableRooms(LocalDateTime, LocalDateTime) throws an
     * IllegalArgumentException when either the start or end time is null.

     * The test deliberately passes a startDate with a value of while supplying a valid future end
     * time. The expected behaviour is that the method validates its input arguments and rejects the
     * request before any interaction with the underlying RoomRepository. Consequently, the
     * exception message must contain the Swedish phrase "Måste ange både start-och sluttid".

     * When this condition occurs, no room data should be queried or processed. The test uses
     * AssertJ’s assertThatThrownBy to confirm that the exception type and message are as
     * specified.
     */
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

    /**
     * Ensures that BookingSystem method getAvailableRooms(LocalDateTime, LocalDateTime) throws an
     * IllegalArgumentException when the supplied end time precedes the start time.

     * The test arranges a future start date and an earlier end date, then verifies that the method
     * rejects the request by throwing an exception whose message contains the Swedish phrase
     * "Sluttid måste vara efter starttid". No interaction with RoomRepository should occur
     * because validation fails before any repository queries.
     */
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


    /**
     * Attempts to cancel an existing booking identified by the given bookingId.

     * The method searches through all rooms retrieved from the repository, locates the booking,
     * removes it from the room’s schedule, persists the updated room state, and notifies the user
     * of the cancellation. If the booking is successfully canceled, the method returns true;
     * otherwise, it returns false.

     * bookingId the unique identifier of the booking to cancel
     * returns true if a booking with the specified ID was found and removed. Returns false
     * otherwise.
     * Throws NotificationException if an error occurs while sending the cancellation confirmation
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
     * Tests that attempting to cancel a booking with an invalid (null) booking ID
     * results in an IllegalArgumentException. The exception message should
     * contain the text "Boknings-id kan inte vara null".
     */
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

    /**
     * Verifies that attempting to cancel a booking either currently in progress
     * or has already finished results in an IllegalStateException.

     * The test sets up a booking whose start date is one day before the current time and
     * ends two days after. It then registers this booking with a room, mocks the repository
     * to return that room, and calls cancelBooking on the system under test.

     * The expected outcome is that an IllegalStateException is thrown,
     * containing the message fragment "Kan inte avboka påbörjad eller avslutad bokning",
     * indicating that bookings which are already underway or completed cannot be canceled.*/
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
    /**
     * Tests that the booking system continues to cancel a booking even when sending a
     * cancellation confirmation through the notification service throws an exception.

     * The test sets up a booking and associates it with a room. It then simulates a failure in
     *  the NotificationService method sendCancellationConfirmation(Booking) by throwing a
     * NotificationException. After invoking cancelBooking on the system, the test verifies
     * that the cancellation succeeds (returns true) and that the updated room state is persisted.

     * This behavior ensures that transient notification failures do not prevent critical booking
     * lifecycle operations from completing.
     */
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

    /**
     * Verifies that attempting to cancel a booking with an ID that does not exist in the system
     * correctly results in false. The test sets up a single room without any bookings,
     * invokes BookingSystem method cancelBooking(String) with a non‑existent booking ID, and
     * asserts that the method returns false, indicating that no cancellation could be performed.
     */
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