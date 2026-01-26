package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//todo: Skapa tester för BookingSystem.java - både tester för lyckade bokningar samt misslyckade

//todo: avboka en bokning (cancelBooking)
//todo: dokumentera testfallen med javadocs

@ExtendWith(MockitoExtension.class)
class BookingSystemTest {

    @Mock
    private TimeProvider timeProvider;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private NotificationService notificationService;

    private BookingSystem bookingSystem;

    private final LocalDateTime now = LocalDateTime.of(2026, 1, 20, 10, 0);


    /**
     * Konfigurerar testmiljön före varje testfall.
     * Använder lenient() för timeProvider eftersom vissa testfall (t.ex. vid ogiltig indata)
     * avbryts innan klockan behöver anropas, vilket annars skulle kasta UnnecessaryStubbingException.
     */
    @BeforeEach
    void setUp() {
        bookingSystem = new BookingSystem(timeProvider, roomRepository, notificationService);
        //Nu är alltid den fixerade tiden av now, lenient()
        lenient().when(timeProvider.getCurrentTime()).thenReturn(now);
    }

    /**
     * Tests that {@link BookingSystem#bookRoom(String, LocalDateTime, LocalDateTime)} succeeds
     * when a room is available and the requested time window is valid.
     *
     * <p>The test arranges a mock {@code RoomRepository} to return an existing {@code Room}
     * and verifies that:
     * <ul>
     *   <li>the method returns {@code true},
     *   <li>{@link RoomRepository#save(Room)} is called with the updated room,
     *   <li>{@link NotificationService#sendBookingConfirmation(Booking)} is invoked
     *       for the created booking.
     * </ul>
     *
     * <p>It also ensures that no exception is thrown and that the booking system behaves
     * as expected under normal successful conditions.*/
    @Test
    void bookARoom_With_Valid_Credentials() throws NotificationException{
        //Arrange
        String roomId = "room1";
        LocalDateTime startTime = now.plusDays(1);
        LocalDateTime endTime = now.plusDays(1).plusHours(2);
        Room room = new Room(roomId, "Presidential suite");

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        //Act
        boolean result = bookingSystem.bookRoom(roomId, startTime, endTime);

        //Assert
        assertThat(result).isTrue();
        verify(roomRepository).save(room);
        verify(notificationService).sendBookingConfirmation(any(Booking.class));


    }

    //"Sluttid måste vara efter starttid"
    @Test
    void bookARoom_With_Invalid_Dates_EndBeforeStart_Should_Throw_Exception() throws NotificationException {
        //arrange - överflödig men bra för att se hela strukturen av mocken
        String roomId = "room1";
        LocalDateTime startTime = now.plusDays(2);
        LocalDateTime endTime = now.plusDays(1);
        //Room room = new Room(roomId, "Ocean suite");

        //act + assert
        assertThatThrownBy(()->
                bookingSystem.bookRoom(roomId, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Sluttid måste vara efter starttid");
       //verify
        verify(notificationService, never())
                .sendBookingConfirmation(any());
        verify(roomRepository, never())
                .save(any());
    }

    //"Bokning kräver giltiga start- och sluttider samt rum-id"
    @Test
    void bookARoom_with_invalid_startAndOrEnd_time_Should_Throw_Exception() throws NotificationException {
        //arrange - överflödig men bra för att se hela strukturen av mocken
        String roomId = "room1";
        LocalDateTime startTime = now.plusDays(2);
        LocalDateTime endTime = null;
        //Room room = new Room(roomId, "Beach house");

        //act + assert
        assertThatThrownBy(()->
                bookingSystem.bookRoom(roomId, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bokning kräver giltiga start- och sluttider samt rum-id");
        //verify
        verify(notificationService, never())
                .sendBookingConfirmation(any());
        verify(roomRepository, never())
                .save(any());
    }

    //"Kan inte boka tid i dåtid"
    @Test
    void bookARoom_with_start_date_before_today_Should_Throw_Exception() throws NotificationException {
        //arrange - - överflödig men bra för att se hela strukturen av mocken
        String roomId = "room1";
        LocalDateTime startTime = now.minusDays(1);
        LocalDateTime endTime = now.plusDays(1);
        //Room room = new Room(roomId, "Family room");

        //act + assert
        assertThatThrownBy(()->
                bookingSystem.bookRoom(roomId, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kan inte boka tid i dåtid");
        //verify
        verify(notificationService, never())
                .sendBookingConfirmation(any());
        verify(roomRepository, never())
                .save(any());
    }

    //"Rummet existerar inte"
    @Test
    void bookARoom_with_room_not_existing_Should_Throw_Exception() throws NotificationException {
        //arrange - överflödig men bra för att se hela strukturen av mocken
        String roomId = "room1";
        LocalDateTime startTime = now.plusDays(2);
        LocalDateTime endTime = now.plusDays(3);
        //Room room = new Room(roomId, "King suite");

        //act + assert
        assertThatThrownBy(()->
                bookingSystem.bookRoom(roomId, startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rummet existerar inte");
        //verify
        verify(notificationService, never())
                .sendBookingConfirmation(any());
        verify(roomRepository, never())
                .save(any());
    }

    @Test
    void getAvailableRoomsWithValidCredentials() {

            // Arrange
            LocalDateTime start = now.plusDays(1);
            LocalDateTime end = now.plusDays(1).plusHours(1);

            Room room1 = new Room("room1", "Ledigt rum");
            Room room2 = new Room("room2", "Upptaget rum");

            // Vi lägger till en bokning i room2 som krockar med vår sökning
            room2.addBooking(new Booking("b1", "room2", start, end));

            when(roomRepository.findAll()).thenReturn(List.of(room1, room2));

            // Act
            List<Room> result = bookingSystem.getAvailableRooms(start, end);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(room1);
            assertThat(result).doesNotContain(room2);

    }

    //test för att se om tillgång finns för att kunna se rum

    //"Måste ange både start- och sluttid"
    @Test
    void getAvailableRooms_with_invalid_credentials_Should_Throw_Exception() {
        LocalDateTime startTime = null;
        LocalDateTime endTime = now.plusDays(2);

        assertThatThrownBy(()->
                bookingSystem.getAvailableRooms(startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Måste ange både start- och sluttid");
    }

    //"Sluttid måste vara efter starttid"
    @Test
    void getAvailableRooms_With_end_before_start_date_Should_Throw_Exception() {
        LocalDateTime startTime = now.plusDays(2);
        LocalDateTime endTime = now.plusDays(1);

        assertThatThrownBy(()->
                bookingSystem.getAvailableRooms(startTime, endTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sluttid måste vara efter starttid");
    }




    @Test
    void cancel_Booking_With_Valid_Id() throws NotificationException {
        String bookingId = "B4567";
        String roomId = "Room1";
        LocalDateTime startTime = now.plusDays(1);
        LocalDateTime endTime = now.plusDays(2);

        Booking booking = new Booking(bookingId, roomId, startTime, endTime);
        Room room = new Room(roomId, "Ocean Suite");
        room.addBooking(booking);

        when(roomRepository.findAll()).thenReturn(List.of(room));

        boolean result = bookingSystem.cancelBooking(bookingId);

        //Assert
        assertThat(result).isTrue();
        verify(roomRepository).save(room);
        verify(notificationService).sendCancellationConfirmation(booking);



    }

    //"Boknings-id kan inte vara null"
    @Test
    void cancel_Booking_With_Invalid_Id_Should_Throw_Exception() {
        String bookingId = null;
        assertThatThrownBy(()->
                bookingSystem.cancelBooking(bookingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Boknings-id kan inte vara null");

    }

    //"Kan inte avboka påbörjad eller avslutad bokning"
    @Test
    void cancel_Booking_During_Or_After_Stay_Should_Throw_Exception(){


    }

}