# Snake and Ladder -- LLD Review Notes

All the fixes applied to your original code, with explanations.

---

## 1. Encapsulation (Private Fields + Getters)

**Your code:**
```java
class Snake {
    int head, tail;  // package-private, anyone can modify
}
```

**Fixed:**
```java
class Snake {
    private final int head;
    private final int tail;
    int getHead() { return head; }
    int getTail() { return tail; }
}
```

**Why:** `private final` prevents external code from changing values after construction. No setters = immutable object = thread-safe = fewer bugs. Interviewers specifically check for this.

---

## 2. Decoupled Players from Board

**Your code:**
```java
class Board {
    List<Player> players;  // Board owns players
    void addPlayer(Player p) { ... }
}
```

**Fixed:**
```java
class Board {
    // NO players here -- Board only knows layout
}
class Game {
    Queue<Player> playerQueue;  // Game owns players
    void addPlayer(Player p) { ... }
}
```

**Why:** Board should only know about its LAYOUT (cells, snakes, ladders). It should NOT know who is playing on it. This is **Separation of Concerns**. Ask yourself: "Does this class NEED to know about this?" If not, move it out.

---

## 3. Queue-Based Turn Management

**Your code:**
```java
// for-loop over List<Player>
for (Player player : board.players) {
    // play turn...
}
// Problem: modifying list while iterating = ConcurrentModificationException
// Problem: harder to remove winners
```

**Fixed:**
```java
Queue<Player> playerQueue = new LinkedList<>();
// In game loop:
Player current = playerQueue.poll();   // take from front
// ... play their turn ...
if (current.getState() != State.WON) {
    playerQueue.offer(current);        // put back at end
}
```

**Why:** Queue is PERFECT for turn-based games:
- `poll()` = take next player (removes from front)
- `offer()` = put back at end (if not won)
- Natural round-robin rotation
- Winners just don't get put back

**Interview Tip:** Using the right data structure (Queue for turns, Map for lookups, Set for uniqueness) shows you think about DESIGN, not just logic.

---

## 4. Map for O(1) Snake/Ladder Lookup

**Your code:**
```java
List<Snake> snakes;
// O(n) iteration on every move
int checkSnake(int pos) {
    for (Snake snake : snakes) {
        if (snake.head == pos) return snake.tail;
    }
    return pos;
}
```

**Fixed:**
```java
Map<Integer, Integer> snakeMap;   // head -> tail
Map<Integer, Integer> ladderMap;  // bottom -> top
// O(1) lookup
if (snakeMap.containsKey(pos)) {
    pos = snakeMap.get(pos);
}
```

**Why:** Every player move checks for snakes/ladders. With a Map, each check is O(1) instead of O(n). For 100 cells this doesn't matter, but in an interview saying "I'd use a Map for O(1)" shows you think about performance.

---

## 5. Board Validation

**Your code:** No validation -- could add overlapping snakes/ladders or out-of-bounds positions.

**Fixed:**
```java
void addSnake(Snake snake) {
    // Check 1: within board bounds [1, size-1]
    // Check 2: no ladder already at this cell
    // Check 3: no duplicate snake head
}
```

**Why:** Fail fast with clear errors. Defensive programming catches bugs at setup time, not during gameplay when they're harder to debug.

---

## 6. Chain Check (Cascading Snakes/Ladders)

**Your code:**
```java
newPos = board.checkLadder(newPos);  // check ladder
newPos = board.checkSnake(newPos);   // then check snake
// Problem: if snake drops you onto ANOTHER ladder, it's missed!
```

**Fixed:**
```java
int getFinalPosition(int pos) {
    boolean moved = true;
    while (moved) {
        moved = false;
        if (snakeMap.containsKey(pos)) {
            pos = snakeMap.get(pos);
            moved = true;  // check again
        } else if (ladderMap.containsKey(pos)) {
            pos = ladderMap.get(pos);
            moved = true;  // check again
        }
    }
    return pos;
}
```

**Why:** A while loop keeps resolving until the position stabilizes. This handles ANY chain depth: ladder -> snake -> ladder -> etc. Verified in testing: snake at 99 dropped to 78, then ladder at 78 climbed to 98.

---

## 7. Minor Fixes

| Issue | Before | After | Why |
|-------|--------|-------|-----|
| Missing comma | `int head tail` | `int head, tail` | Syntax error |
| Nonexistent type | `Position pos` | `int head, int tail` | Position class didn't exist |
| Case sensitivity | `string turn` | `State state` | Java: `String` not `string` |
| Syntax error | `state. = turn` | `this.state = state` | Invalid assignment |
| Missing semicolon | `List<Player> players` | `List<Player> players;` | Won't compile |
| Typo | `PLayer` | `Player` | Case matters in Java |
| Param shadowing | `addPlayers(Player players)` | `addPlayer(Player player)` | `players.add(players)` added list to itself |
| Wrong language | `rand()%6` | `new Random().nextInt(6)+1` | That's C, not Java |
| Missing import | (nothing) | `import java.util.*` | List, ArrayList, Random need importing |

---

## Class Diagram (Final)

```
State (enum)          -- START, PLAYING, WON
Snake                 -- private final int head, tail
Ladder                -- private final int bottom, top
Player                -- private final String name; int pos; State state
Dice                  -- private int faces; int roll()
Board                 -- int size; Map snakeMap, ladderMap; getFinalPosition()
Game                  -- Board, Dice, Queue<Player>; play()
```

**Relationships:**
- Game HAS-A Board (aggregation)
- Game HAS-A Dice (aggregation)
- Game HAS-MANY Players via Queue (aggregation)
- Board HAS-MANY Snakes/Ladders via Maps (composition)
