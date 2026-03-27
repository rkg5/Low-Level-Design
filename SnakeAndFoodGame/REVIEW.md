# Snake & Food Game LLD — Failures & Lessons Learned

---

## 1. Repeated Mistakes From StackOverflow

| Mistake | StackOverflow | Snake & Food | Total |
|---|---|---|---|
| `Class` (uppercase) | 2 times | 5 times | **7** |
| No imports | 1 time | 1 time | **2** |
| Missing `equals()`/`hashCode()` | — | Cell class | **1** |

**Action:** Before writing ANY Java class, your fingers type `class` (lowercase). This is muscle memory, not knowledge. Drill it.

---

## 2. Your Bugs — Side-by-Side Fixes

### Direction: Class vs Enum
```diff
- Class Direction { int dx, dy; }
+ enum Direction {
+     UP(0,-1), DOWN(0,1), LEFT(-1,0), RIGHT(1,0);
+     final int dx, dy;
+ }
```
**Rule:** Fixed set of values → always `enum`.

### Scope Leak — `width`/`height` in Snake
```diff
  class Snake {
      void move() {
-         if (newHead.x >= width)  // ❌ width doesn't exist in Snake
+         // Snake doesn't check bounds — Game does that
      }
  }
```
**Rule:** Ask *"who OWNS this data?"* — that's where the logic goes.

### `grow()` Bug — Accessing Local Variable Cross-Method
```diff
  void grow() {
-     snake.addFirst(newHead);  // ❌ newHead is local to move()
+     growPending++;  // Just flag it — move() handles the rest
  }
```
**Lesson:** Growth = don't remove the tail on next move. Not "add an extra head."

### Missing `equals()` + `hashCode()` on Cell
Without these, `snake.contains(new Cell(3,4))` returns `false` even if the snake IS at (3,4). Java compares memory addresses by default.
```java
@Override
public boolean equals(Object o) {
    if (!(o instanceof Cell)) return false;
    Cell c = (Cell) o;
    return x == c.x && y == c.y;
}
@Override
public int hashCode() { return 31 * x + y; }
```
**Rule:** Using `.contains()`, `.equals()`, `HashMap`, or `HashSet`? → Override both. Always.

### 60+ Duplicate `gameResume()` Methods
Won't compile. Never add a method unless you can answer: *"What state does this change?"*

---

## 3. SOLID Principles Applied to This Problem

| Principle | How It Applies | Example in Code |
|---|---|---|
| **S** - Single Responsibility | Each class has ONE job | `Board` = bounds, `Snake` = movement, `Food` = spawning, `Game` = orchestration |
| **O** - Open/Closed | Add new food types without changing existing classes | Create `BonusFood extends Food` — `Game` works unchanged |
| **L** - Liskov Substitution | Any `Food` subtype works where `Food` is expected | `BonusFood` can replace `Food` in `Game` seamlessly |
| **I** - Interface Segregation | Small, focused interfaces | Could add `Renderable` interface for anything that draws on board |
| **D** - Dependency Inversion | `Game` depends on abstractions, not concretions | `Food` takes `Random` via constructor (injectable/testable) |

---

## 4. Performance — What Meta Interviewers Will Ask

### "What's the time complexity of self-collision check?"

**Current code:** `body.contains(position)` on a `LinkedList` → **O(n)** where n = snake length.

**Better approach:** Maintain a `HashSet<Cell>` alongside the `Deque<Cell>`:
```java
class Snake {
    private Deque<Cell> body;
    private Set<Cell> bodySet;  // O(1) lookup

    void move(Cell newHead) {
        body.addFirst(newHead);
        bodySet.add(newHead);
        if (growPending > 0) { growPending--; }
        else {
            Cell removed = body.removeLast();
            bodySet.remove(removed);  // Keep set in sync
        }
    }

    boolean collidesWithSelf(Cell pos) {
        return bodySet.contains(pos);  // O(1) instead of O(n)
    }
}
```

| | `Deque.contains()` | `HashSet.contains()` |
|---|---|---|
| Time | O(n) | O(1) |
| Space | No extra | +O(n) |
| When it matters | Snake length > 100 | Always fast |

**Say this in the interview:** *"For a basic implementation, Deque.contains is fine. But if we optimize for large boards or long snakes, I'd add a HashSet alongside the Deque for O(1) collision checks — classic time-space tradeoff."*

### "What about `printBoard()` performance?"

Current: O(W × H × N) — for each cell, we check `body.contains()` which is O(N).

**Better:** Use a `Set<Cell>` for the body → O(W × H).

**Even better for real games:** Don't redraw the whole board. Only update cells that changed (head added, tail removed, food eaten). This is how real game renders work.

---

## 5. Follow-Up Questions Meta WILL Ask

### Q: "How would you add different food types?"

**Answer:** Use inheritance or an interface:
```java
interface FoodItem {
    Cell getPosition();
    int getPoints();
    void spawn(int w, int h, Collection<Cell> occupied);
}

class NormalFood implements FoodItem { ... }       // +1 point, +1 growth
class BonusFood implements FoodItem { ... }        // +5 points, +3 growth, disappears after 10 ticks
class PoisonFood implements FoodItem { ... }       // -1 length, doesn't kill
```
`Game` holds `List<FoodItem>` instead of a single `Food`.

### Q: "How would you add obstacles/walls?"

**Answer:** Add obstacle cells to `Board`:
```java
class Board {
    private Set<Cell> obstacles;
    boolean isBlocked(Cell c) { return isOutOfBounds(c) || obstacles.contains(c); }
}
```
`Game.tick()` calls `board.isBlocked()` instead of just `isOutOfBounds()`.

### Q: "How would you make it multiplayer?"

**Answer:**
- `Game` holds `List<Snake>` instead of one `Snake`
- Each `tick()` processes all snakes
- Collision: snake hitting another snake's body = death
- Food spawn avoids ALL snake bodies
- Turn-based (each player inputs) or real-time (concurrent input threads)

### Q: "How would you add difficulty levels?"

**Answer:** Extract game config:
```java
class GameConfig {
    int width, height;
    int tickSpeedMs;        // 500ms=easy, 100ms=hard
    boolean wrapAround;     // wall kills vs wraps to other side
    int foodCount;          // how many simultaneous food items
}
```

### Q: "What about wrap-around mode (no wall death)?"

**Answer:** One-line change in `Board`:
```java
Cell wrap(Cell c) {
    return new Cell((c.x + width) % width, (c.y + height) % height);
}
```
`Game` calls `board.wrap(newHead)` instead of `board.isOutOfBounds()`.

### Q: "What if the snake fills the entire board?"

**Answer:** Current `food.spawn()` loops forever if no empty cell exists. Fix:
```java
void spawn(int w, int h, Collection<Cell> occupied) {
    if (occupied.size() >= w * h) throw new IllegalStateException("Board full - you win!");
    Cell candidate;
    do { candidate = new Cell(random.nextInt(w), random.nextInt(h)); }
    while (occupied.contains(candidate));
    this.position = candidate;
}
```
Or collect all empty cells first and pick randomly from those (deterministic time).

---

## 6. Design Patterns Relevant to This Problem

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | Different AI strategies for auto-play snake | `interface MoveStrategy { Direction nextMove(Snake, Board, Food); }` |
| **Observer** | Game events (score changed, game over, food eaten) | `interface GameListener { void onFoodEaten(); void onGameOver(); }` |
| **State** | Game states (Playing, Paused, GameOver) | Already done with `GameState` enum — each state has different `tick()` behavior |
| **Factory** | Creating different food types | `FoodFactory.create(FoodType type)` |
| **Builder** | Complex game configuration | `new GameBuilder().width(20).height(20).wrapAround(true).build()` |

---

## 7. Thread Safety — Real-Time Game

If the interviewer asks *"make this real-time instead of turn-based"*:

```java
class Game {
    private volatile GameState state;        // visible across threads
    private final Object lock = new Object();

    // Game loop thread — ticks every N ms
    void startGameLoop(int tickMs) {
        new Thread(() -> {
            while (state == GameState.PLAYING) {
                synchronized (lock) { tick(); }
                Thread.sleep(tickMs);
            }
        }).start();
    }

    // Input thread — reads user input
    void changeDirection(Direction dir) {
        synchronized (lock) { snake.setDirection(dir); }
    }
}
```
Key concepts: `volatile` for state visibility, `synchronized` for mutual exclusion between game loop and input.

---

## 8. Entity Responsibility Map

```
Cell       → x, y, equals(), hashCode()         [Value Object]
Direction  → enum UP/DOWN/LEFT/RIGHT with dx,dy  [Enum]
GameState  → enum PLAYING/PAUSED/GAME_OVER        [Enum]
Snake      → body, direction, move, grow, self-collision  [Entity]
Food       → position, spawn (avoids snake body)  [Entity]
Board      → dimensions, bounds checking           [Entity]
Game       → orchestrates tick cycle               [Service/Controller]
```

**Each entity has ONE job. If you write `width` inside `Snake`, you're violating SRP.**
