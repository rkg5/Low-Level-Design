# Airline Management System LLD — Complete Interview Guide

---

## Current Score: 9/10

What's covered and what could push it to 10/10:

| Feature | Status | Notes |
|---|---|---|
| Core entities (Flight, Seat, User, Booking, Aircraft) | Done | Clean separation |
| Enums (6 enums) | Done | Type-safe state management |
| Observer pattern (Notifications) | Done | Email + SMS |
| Search flights | Done | By source + destination |
| Book with validation | Done | Double-booking prevented |
| Cancel with seat release | Done | Seat returns to AVAILABLE |
| Encapsulation | Done | Private fields + getters |
| Working `main()` demo | Done | End-to-end flow proved |
| **Concurrency** | Not in code | Covered in doc below |
| **Strategy pattern (Payments)** | Not in code | Covered in doc below |
| **Thread-safe booking** | Not in code | Covered in doc below |

---

## 1. Your Original Mistakes (Repeat Tracker)

| Mistake | Problem #1 | Problem #2 | Problem #3 | Total |
|---|---|---|---|---|
| `Class` (uppercase) | 2x | 5x | 3x | **10** |
| No constructors | Yes | Yes | Yes | **3 problems** |
| No imports | Yes | Yes | Yes | **3 problems** |
| `String` for everything | Yes | — | Yes | **2 problems** |
| `Void` uppercase | — | — | Yes | **1** |
| Duplicate class names | — | — | Yes | **1** |
| C++ syntax in Java | — | — | Yes | **1** |

---

## 2. Concurrency — The #1 FAANG Follow-Up

### The Problem
```
Thread 1: Alice checks seat 1A → available
Thread 2: Bob checks seat 1A   → available
Thread 1: Alice books seat 1A  → success
Thread 2: Bob books seat 1A    → success ← BUG: double booking!
```
This is a **race condition**. The check-then-act (`isAvailable()` then `book()`) is not atomic.

### Solution 1: `synchronized` (Simplest)
```java
class AirlineSystem {
    // Lock on the seat object — only one thread can book a specific seat at a time
    Booking bookFlight(User user, Flight flight, Seat seat) {
        synchronized (seat) {
            if (!seat.isAvailable()) return null;
            seat.book();
            // ... create booking
        }
    }
}
```
**When to mention:** Always. This is the minimum expected answer.

### Solution 2: `ReentrantLock` (More Control)
```java
class Seat {
    private final ReentrantLock lock = new ReentrantLock();

    boolean tryBook() {
        if (lock.tryLock()) {
            try {
                if (status == SeatStatus.AVAILABLE) {
                    status = SeatStatus.BOOKED;
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
```
**When to mention:** If interviewer asks *"what if you don't want to block?"* — `tryLock()` returns immediately instead of waiting.

### Solution 3: `AtomicReference` + CAS (Lock-Free)
```java
class Seat {
    private AtomicReference<SeatStatus> status = new AtomicReference<>(SeatStatus.AVAILABLE);

    boolean tryBook() {
        return status.compareAndSet(SeatStatus.AVAILABLE, SeatStatus.BOOKED);
    }

    void release() {
        status.set(SeatStatus.AVAILABLE);
    }
}
```
**When to mention:** If interviewer asks about **lock-free** or **high-throughput** solutions. CAS = Compare And Swap, hardware-level atomic operation.

### Solution 4: Database-Level (Production)
In a real system, you'd use:
```sql
UPDATE seats SET status = 'BOOKED'
WHERE seat_id = ? AND status = 'AVAILABLE';
-- Returns rows_affected = 1 → success, 0 → someone else got it
```
**When to mention:** If the interviewer shifts from LLD to system design.

### Concurrency Cheat Sheet
| Approach | Blocking? | Complexity | Use When |
|---|---|---|---|
| `synchronized` | Yes | Low | Default LLD answer |
| `ReentrantLock` | Optional | Medium | Need tryLock/timeout |
| `AtomicReference` | No | Medium | High throughput, single field |
| DB optimistic lock | No | High | Production systems |

---

## 3. Strategy Pattern — Payments

```java
interface PaymentStrategy {
    boolean pay(double amount, User user);
}

class CreditCardPayment implements PaymentStrategy {
    public boolean pay(double amount, User user) {
        System.out.println("[CC] Charged $" + amount + " to " + user.getName());
        return true;
    }
}

class UPIPayment implements PaymentStrategy {
    public boolean pay(double amount, User user) {
        System.out.println("[UPI] Charged $" + amount + " to " + user.getName());
        return true;
    }
}

// Usage in AirlineSystem:
Booking bookFlight(User user, Flight flight, Seat seat, PaymentStrategy payment) {
    if (!payment.pay(seat.getPrice(), user)) {
        return null; // Payment failed
    }
    seat.book();
    // ...
}
```
**Why interviewers love this:** Shows Open/Closed principle — add new payment methods without modifying existing code.

---

## 4. SOLID Applied to This Problem

| Principle | How It Applies |
|---|---|
| **S** - Single Responsibility | `Seat` manages seat state, `Booking` tracks booking, `NotificationService` handles alerts — each class does ONE thing |
| **O** - Open/Closed | New `NotificationObserver` (Push, WhatsApp) or `PaymentStrategy` (Crypto) can be added without modifying existing classes |
| **L** - Liskov Substitution | `SMSNotification` replaces `EmailNotification` wherever `NotificationObserver` is expected |
| **I** - Interface Segregation | `NotificationObserver` has ONE method (`notify`). Not bloated with unrelated methods |
| **D** - Dependency Inversion | `AirlineSystem` depends on `NotificationObserver` interface, not on `EmailNotification` directly |

---

## 5. Google/Meta/Uber Follow-Up Questions

### Q1: "How do you handle overbooking?" (Airlines do this!)
```java
class Flight {
    private int overbookingLimit; // e.g., 5% extra
    boolean canBook() {
        int bookedCount = seats.size() - getAvailableSeats().size();
        return bookedCount < seats.size() + overbookingLimit;
    }
}
```
If overbooked passengers show up, compensate with `Booking.compensate()`.

### Q2: "How would you handle seat selection with pricing tiers?"
Already done — `Seat` has `SeatType` and `price`. Extend with:
```java
class PricingEngine {
    double getPrice(Seat seat, Date bookingDate, Date departureDate) {
        long daysUntilDeparture = /* calculate */;
        double multiplier = daysUntilDeparture < 7 ? 2.0 : 1.0; // dynamic pricing
        return seat.getBasePrice() * multiplier;
    }
}
```

### Q3: "How would you add a loyalty/frequent flyer program?"
```java
class User {
    private int loyaltyPoints;
    void earnPoints(int points) { loyaltyPoints += points; }
    boolean redeemPoints(int points) {
        if (loyaltyPoints >= points) { loyaltyPoints -= points; return true; }
        return false;
    }
}
```
Booking triggers `user.earnPoints(flight.getDistance() * multiplier)`.

### Q4: "How would you handle connecting flights?"
```java
class Itinerary {
    private List<Flight> legs;  // Delhi → Mumbai → Bangalore
    private User passenger;
    double getTotalPrice() { /* sum all seat prices */ }
}
```
`AirlineSystem.searchConnections(source, dest)` uses BFS/DFS on route graph.

### Q5: "How would you scale this for millions of flights?"
| Concern | Solution |
|---|---|
| Search speed | Index flights by route: `Map<String, List<Flight>>` where key = "DEL-MUM" |
| Concurrent bookings | Per-seat locking (not global lock) |
| Data volume | Partition flights by date, archive old flights |
| Read-heavy traffic | Cache popular routes, invalidate on schedule changes |

### Q6: "How do you ensure idempotent bookings?" (Uber loves this)
```java
Booking bookFlight(User user, Flight flight, Seat seat, String idempotencyKey) {
    if (processedKeys.contains(idempotencyKey)) {
        return existingBookings.get(idempotencyKey); // Return existing, don't re-book
    }
    // ... process booking
    processedKeys.add(idempotencyKey);
}
```
Prevents duplicate bookings from network retries.

---

## 6. Design Patterns Used (Interview Scorecard)

| Pattern | Where | Points |
|---|---|---|
| **Observer** | `NotificationObserver` → Email/SMS | Decoupled notifications |
| **Strategy** | `PaymentStrategy` → CC/UPI | Swappable payment methods |
| **Builder** | Could use for complex `Flight` creation | Optional bonus |
| **Factory** | Could use for `Seat` creation by type | Optional bonus |
| **Singleton** | `AirlineSystem` could be singleton | Mention if asked |

---

## 7. Entity Relationship Diagram

```
Aircraft  1──────N  Flight
Flight    1──────N  Seat
Flight    1──────N  Booking
User      1──────N  Booking
Booking   N──────1  Seat
Booking   N──────1  Flight
Booking   N──────1  User

AirlineSystem (orchestrator)
├── List<Flight>
├── List<User>
├── List<Booking>
└── NotificationService
    └── List<NotificationObserver>
        ├── EmailNotification
        └── SMSNotification
```

---

## 8. What Makes This 10/10 vs 9/10

To push from 9 → 10 in the **actual interview**, you should be able to **verbally explain** (not necessarily code):

| Topic | One-Liner You Should Say |
|---|---|
| Concurrency | "I'd synchronize on the Seat object to prevent double-booking race conditions" |
| Payments | "Strategy pattern — inject PaymentStrategy into bookFlight for CC/UPI/Crypto" |
| Scaling search | "Index flights by route key like DEL-MUM in a HashMap for O(1) lookup" |
| Idempotency | "Use an idempotency key on bookFlight to handle network retries safely" |
| Dynamic pricing | "PricingEngine calculates price based on demand and days until departure" |
| Connecting flights | "Model as Itinerary with List<Flight> legs, search with BFS on route graph" |

**You don't need to code all of these. You need to MENTION them when asked. The code you have is the 9/10 base. These verbal answers push it to 10/10.**
