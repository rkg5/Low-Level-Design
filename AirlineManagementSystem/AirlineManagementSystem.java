import java.util.*;

enum SeatStatus { AVAILABLE, BOOKED }

enum SeatType { ECONOMY, BUSINESS, FIRST_CLASS }

enum FlightStatus { SCHEDULED, DELAYED, CANCELLED, COMPLETED }

enum BookingStatus { CONFIRMED, CANCELLED, PENDING }

enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }

class Aircraft {
    private String model;
    private int totalSeats;

    Aircraft(String model, int totalSeats) {
        this.model = model;
        this.totalSeats = totalSeats;
    }

    String getModel() { return model; }
    int getTotalSeats() { return totalSeats; }

    @Override
    public String toString() { return model + " (" + totalSeats + " seats)"; }
}

class Seat {
    private String seatNumber;
    private SeatType type;
    private SeatStatus status;
    private double price;

    Seat(String seatNumber, SeatType type, double price) {
        this.seatNumber = seatNumber;
        this.type = type;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    boolean isAvailable() { return status == SeatStatus.AVAILABLE; }
    void book() { this.status = SeatStatus.BOOKED; }
    void release() { this.status = SeatStatus.AVAILABLE; }

    String getSeatNumber() { return seatNumber; }
    SeatType getType() { return type; }
    SeatStatus getStatus() { return status; }
    double getPrice() { return price; }

    @Override
    public String toString() {
        return seatNumber + " (" + type + ") " + status + " $" + price;
    }
}

class Flight {
    private String flightNumber;
    private String source;
    private String destination;
    private Date departureTime;
    private Date arrivalTime;
    private Aircraft aircraft;
    private List<Seat> seats;
    private FlightStatus status;

    Flight(String flightNumber, String source, String destination,
           Date departureTime, Date arrivalTime, Aircraft aircraft) {
        this.flightNumber = flightNumber;
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.aircraft = aircraft;
        this.seats = new ArrayList<>();
        this.status = FlightStatus.SCHEDULED;
    }

    void addSeat(Seat seat) { seats.add(seat); }

    List<Seat> getAvailableSeats() {
        List<Seat> available = new ArrayList<>();
        for (Seat s : seats) {
            if (s.isAvailable()) available.add(s);
        }
        return available;
    }

    String getFlightNumber() { return flightNumber; }
    String getSource() { return source; }
    String getDestination() { return destination; }
    Date getDepartureTime() { return departureTime; }
    FlightStatus getStatus() { return status; }
    void setStatus(FlightStatus status) { this.status = status; }

    @Override
    public String toString() {
        return flightNumber + " | " + source + " -> " + destination
            + " | Available seats: " + getAvailableSeats().size();
    }
}

class User {
    private String name;
    private String email;
    private List<Booking> bookings;

    User(String name, String email) {
        this.name = name;
        this.email = email;
        this.bookings = new ArrayList<>();
    }

    void addBooking(Booking booking) { bookings.add(booking); }

    String getName() { return name; }
    String getEmail() { return email; }
    List<Booking> getBookings() { return bookings; }

    @Override
    public String toString() { return name + " (" + email + ")"; }
}

class Booking {
    private String bookingId;
    private User user;
    private Flight flight;
    private Seat seat;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private Date bookedAt;

    Booking(String bookingId, User user, Flight flight, Seat seat) {
        this.bookingId = bookingId;
        this.user = user;
        this.flight = flight;
        this.seat = seat;
        this.status = BookingStatus.CONFIRMED;
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.bookedAt = new Date();
    }

    void cancel() {
        this.status = BookingStatus.CANCELLED;
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.seat.release();
    }

    String getBookingId() { return bookingId; }
    User getUser() { return user; }
    Flight getFlight() { return flight; }
    Seat getSeat() { return seat; }
    BookingStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "Booking[" + bookingId + "] " + user.getName()
            + " | " + flight.getFlightNumber()
            + " | Seat: " + seat.getSeatNumber()
            + " | " + status;
    }
}

interface NotificationObserver {
    void notify(String message);
}

class EmailNotification implements NotificationObserver {
    @Override
    public void notify(String message) {
        System.out.println("  [EMAIL] " + message);
    }
}

class SMSNotification implements NotificationObserver {
    @Override
    public void notify(String message) {
        System.out.println("  [SMS] " + message);
    }
}

class NotificationService {
    private List<NotificationObserver> observers = new ArrayList<>();

    void addObserver(NotificationObserver observer) { observers.add(observer); }

    void notifyAll(String message) {
        for (NotificationObserver o : observers) {
            o.notify(message);
        }
    }
}

class AirlineSystem {
    private List<Flight> flights;
    private List<User> users;
    private List<Booking> bookings;
    private NotificationService notificationService;
    private int bookingCounter;
    private Map<String, List<Flight>> flightsByRoute;

    AirlineSystem() {
        this.flights = new ArrayList<>();
        this.users = new ArrayList<>();
        this.bookings = new ArrayList<>();
        //DI principles voilates
        this.notificationService = new NotificationService();
        this.notificationService.addObserver(new EmailNotification());
        this.notificationService.addObserver(new SMSNotification());
        this.bookingCounter = 0;
        this.flightsByRoute = new HashMap<>();
    }

    void addFlight(Flight flight) {
        flights.add(flight);
        String routeKey = flight.getSource().toLowerCase() + "-" + flight.getDestination().toLowerCase();
        flightsByRoute.putIfAbsent(routeKey, new ArrayList<>());
        flightsByRoute.get(routeKey).add(flight);
        System.out.println("Flight added: " + flight);
    }

    void registerUser(User user) {
        users.add(user);
        System.out.println("User registered: " + user);
    }

    List<Flight> searchFlights(String source, String destination) {
        String routeKey = source.toLowerCase() + "-" + destination.toLowerCase();
        List<Flight> results = new ArrayList<>();
        List<Flight> routeFlights = flightsByRoute.getOrDefault(routeKey, new ArrayList<>());
        for (Flight f : routeFlights) {
            if (f.getStatus() == FlightStatus.SCHEDULED && !f.getAvailableSeats().isEmpty()) {
                results.add(f);
            }
        }
        return results;
    }

    Booking bookFlight(User user, Flight flight, Seat seat) {
        if (!seat.isAvailable()) {
            System.out.println("[DENIED] Seat " + seat.getSeatNumber() + " is already booked!");
            return null;
        }
        if (flight.getStatus() != FlightStatus.SCHEDULED) {
            System.out.println("[DENIED] Flight " + flight.getFlightNumber() + " is not available!");
            return null;
        }

        seat.book();
        String bookingId = "BK-" + (++bookingCounter);
        Booking booking = new Booking(bookingId, user, flight, seat);
        bookings.add(booking);
        user.addBooking(booking);

        notificationService.notifyAll(
            "Booking " + bookingId + " confirmed for " + user.getName()
            + " on " + flight.getFlightNumber()
            + " | Seat: " + seat.getSeatNumber());

        return booking;
    }

    boolean cancelBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            System.out.println("[DENIED] Booking already cancelled!");
            return false;
        }

        booking.cancel();

        notificationService.notifyAll(
            "Booking " + booking.getBookingId() + " cancelled for "
            + booking.getUser().getName()
            + ". Refund initiated.");

        return true;
    }
}

public class AirlineManagementSystem {
    public static void main(String[] args) {
        AirlineSystem system = new AirlineSystem();

        // 1. Create aircraft and flights
        Aircraft boeing = new Aircraft("Boeing 737", 180);
        Flight f1 = new Flight("AI-101", "Delhi", "Mumbai",
            new Date(), new Date(), boeing);
        Flight f2 = new Flight("AI-202", "Delhi", "Bangalore",
            new Date(), new Date(), boeing);

        // Add seats to flights
        f1.addSeat(new Seat("1A", SeatType.FIRST_CLASS, 500));
        f1.addSeat(new Seat("1B", SeatType.FIRST_CLASS, 500));
        f1.addSeat(new Seat("10A", SeatType.ECONOMY, 100));
        f1.addSeat(new Seat("10B", SeatType.ECONOMY, 100));

        f2.addSeat(new Seat("1A", SeatType.BUSINESS, 300));
        f2.addSeat(new Seat("5A", SeatType.ECONOMY, 120));

        system.addFlight(f1);
        system.addFlight(f2);

        // 2. Register users
        User alice = new User("Alice", "alice@mail.com");
        User bob = new User("Bob", "bob@mail.com");
        system.registerUser(alice);
        system.registerUser(bob);

        // 3. Search flights
        System.out.println("\n--- Search: Delhi -> Mumbai ---");
        List<Flight> results = system.searchFlights("Delhi", "Mumbai");
        for (Flight f : results) System.out.println("  " + f);

        // 4. Book flights
        System.out.println("\n--- Booking ---");
        Seat seatToBook = f1.getAvailableSeats().get(0);
        Booking b1 = system.bookFlight(alice, f1, seatToBook);

        // 5. Try to book same seat again (should fail)
        System.out.println("\n--- Double Booking (should fail) ---");
        system.bookFlight(bob, f1, seatToBook);

        // 6. Bob books a different seat
        System.out.println("\n--- Bob books economy ---");
        Booking b2 = system.bookFlight(bob, f1, f1.getAvailableSeats().get(0));

        // 7. Cancel booking
        System.out.println("\n--- Cancellation ---");
        system.cancelBooking(b1);

        // 8. Verify seat is available again after cancellation
        System.out.println("\n--- Search after cancellation ---");
        results = system.searchFlights("Delhi", "Mumbai");
        for (Flight f : results) System.out.println("  " + f);

        // 9. Print final state
        System.out.println("\n--- Alice's bookings ---");
        for (Booking b : alice.getBookings()) System.out.println("  " + b);
        System.out.println("\n--- Bob's bookings ---");
        for (Booking b : bob.getBookings()) System.out.println("  " + b);
    }
}
