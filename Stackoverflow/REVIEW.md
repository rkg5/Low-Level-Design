# StackOverflow LLD — Failures & Lessons Learned

Your original code vs what it should have been. Study the **why**, not just the fix.

---

## 1. Compilation Errors You Made (Twice)

These are the errors that will end your interview before it starts.

### `Class` vs `class`
```diff
- Class Tag {
+ class Tag {

- Class StackOverFlow {
+ class StackOverflow {
```
**Why you failed:** Java keywords are lowercase. `Class` is actually a valid type in Java (`java.lang.Class`) — so the compiler thinks you're declaring a variable, not a class. You made this mistake **in both rounds**. Drill it.

### `string` vs `String`
```diff
- string name;
- string content;
+ String name;
+ String content;
```
**Why you failed:** Java has no `string`. It's `String` (a class in `java.lang`). This is the #1 tell that someone comes from Python/JS and hasn't internalized Java.

### Missing semicolons
```diff
- String Education
- User user
+ String education;
+ User user;
```
**Why you failed:** Rushing. In an interview, slow down and proofread every line.

### Wrong constructor name
```diff
- Questions(Author author, String name, String content) {
+ Question(User user, String title, String content, List<Tag> tags) {
```
**Why you failed:** Three errors in one line:
1. Constructor must match class name exactly (`Question`, not `Questions`)
2. `Author` doesn't exist — you renamed it to `User`
3. Missing parameters (no tags)

### Wrong initialization
```diff
- questions = new questions();
- answers = new answers();
+ questions = new ArrayList<>();
+ answers = new ArrayList<>();
```
**Why you failed:** `new questions()` tries to call a constructor on a class called `questions`. Lists are initialized with `new ArrayList<>()`.

---

## 2. Design Mistakes

### Using `int upvotes` instead of `List<Vote>`

**Your version:**
```java
int upvotes;
int downvotes;
```

**Correct version:**
```java
private List<Vote> votes = new ArrayList<>();
```

**Why it matters:**
| | `int` counters | `List<Vote>` |
|---|---|---|
| Can user vote twice? | Yes (bug) | No (check existing votes) |
| Can user change vote? | No | Yes (find and update) |
| Know WHO voted? | No | Yes |
| Undo a vote? | No | Yes (remove from list) |

**Interview killer:** If an interviewer asks *"how do you prevent a user from voting twice?"* and you have `int upvotes`, you have no answer.

### Missing `Voteable` interface

**Your version:** Vote logic doesn't exist.

**Correct version:**
```java
interface Voteable {
    void vote(User user, VoteType type);
    int getScore();
}
class Question implements Voteable { ... }
class Answer   implements Voteable { ... }
```

**Why it matters:** Both `Question` and `Answer` can be voted on. Without the interface, you duplicate vote logic. With it, `StackOverflowSystem.vote()` accepts any `Voteable` — **polymorphism in action**. Interviewers specifically look for this.

### `Author` with `age` and `Education`

**Your version:**
```java
class Author {
    String name;
    Double age;
    String Education;
}
```

**Correct version:**
```java
class User {
    private String name;
    private int reputation;
    private List<Question> questions;
    private List<Answer> answers;
    private List<Comment> comments;
}
```

**Why it matters:**
- `age`, `Education` are irrelevant to StackOverflow — wastes interview time
- Name it `User` not `Author` — matches the domain language
- `reputation` is core to the platform — must be modeled
- Users own questions, answers, comments — must track those relationships

### No relationship between Question and Answer

**Your version:** `Question` has no `List<Answer>`. `Answer` has no reference to `Question`.

**Correct version:**
```java
class Question {
    private List<Answer> answers;   // a question HAS many answers
}
class Answer {
    private Question question;      // an answer BELONGS TO one question
}
```

**Why it matters:** Without this, there's no way to find answers for a question or know which question an answer belongs to. This is basic **entity relationship modeling**.

---

## 3. Patterns to Memorize for ANY LLD Interview

### The Entity Checklist
Before coding, list every entity and ask:
- What **fields** does it have?
- What **relationships** does it have? (has-many, belongs-to)
- What **behaviors** does it have? (methods, not just data)
- Does it share behavior with another entity? → **Interface**

### The SOLID Checklist
| Principle | How it applies here |
|---|---|
| **S** - Single Responsibility | `Question` handles questions, `StackOverflowSystem` orchestrates |
| **O** - Open/Closed | `Voteable` lets you add new voteable entities without changing vote logic |
| **L** - Liskov Substitution | Any `Voteable` can be passed to `vote()` |
| **I** - Interface Segregation | `Voteable` is small and focused — only `vote()` and `getScore()` |
| **D** - Dependency Inversion | `StackOverflowSystem.vote()` depends on `Voteable` interface, not concrete classes |

### The Constructor Rule
**Every class must:**
1. Have a constructor
2. Initialize ALL collection fields (`new ArrayList<>()`, `new HashMap<>()`)
3. Set sensible defaults (`reputation = 0`, `status = OPEN`)

Forgetting to initialize a `List` → `NullPointerException` on first use.

---

## 4. Common LLD Interview Traps

| Trap | How to avoid |
|---|---|
| Jumping straight to code | List entities + relationships on paper first |
| Plural class names (`Questions`) | Always singular — each object is ONE thing |
| Raw strings for states | Use `enum` — `QuestionStatus.OPEN` not `"open"` |
| No validation in methods | Always check: is user authorized? does entity exist? |
| God class (one class does everything) | Split into entity classes + one orchestrator |
| No `main()` or demo | Always write one — proves your code works |
| Exposing all fields as public | Use `private` + getters. Modify state through methods |

---

## 5. Your LLD Interview Template

Use this structure for **every** LLD problem:

```
Step 1: Identify entities           (nouns in the problem)
Step 2: Identify relationships      (who owns what, has-many, belongs-to)
Step 3: Identify behaviors          (verbs — what can users DO?)
Step 4: Identify shared behaviors   (→ interfaces)
Step 5: Define enums for states     (status, types)
Step 6: Code entities               (fields + constructor + init lists)
Step 7: Code orchestrator class     (the "System" that ties it all together)
Step 8: Code main() demo            (prove it works)
```
