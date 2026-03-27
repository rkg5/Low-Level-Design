import java.util.*;

enum QuestionStatus { OPEN, CLOSED, DUPLICATE, DELETED }

enum VoteType { UP, DOWN }

interface Voteable {
    void vote(User user, VoteType type);
    int getScore();
}

class Tag {
    private String name;

    Tag(String name) { this.name = name; }

    String getName() { return name; }

    @Override
    public String toString() { return name; }
}

class Vote {
    private User user;
    private VoteType type;

    Vote(User user, VoteType type) {
        this.user = user;
        this.type = type;
    }

    User getUser() { return user; }
    VoteType getType() { return type; }
    void setType(VoteType type) { this.type = type; }
}

class Comment {
    private User user;
    private String content;
    private Date createdAt;

    Comment(User user, String content) {
        this.user = user;
        this.content = content;
        this.createdAt = new Date();
    }

    @Override
    public String toString() { return user.getName() + ": " + content; }
}

class Question implements Voteable {
    private User author;
    private String title;
    private String content;
    private QuestionStatus status;
    private Date createdAt;
    private List<Answer> answers;
    private List<Comment> comments;
    private List<Tag> tags;
    private List<Vote> votes;
    private Answer acceptedAnswer;

    Question(User author, String title, String content, List<Tag> tags) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.status = QuestionStatus.OPEN;
        this.createdAt = new Date();
        this.answers = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.votes = new ArrayList<>();
    }

    @Override
    public void vote(User user, VoteType type) {
        for (Vote v : votes) {
            if (v.getUser() == user) {
                v.setType(type);
                System.out.println("  " + user.getName() + " changed vote to " + type + " on question: " + title);
                return;
            }
        }
        votes.add(new Vote(user, type));
        author.addReputation(type == VoteType.UP ? 5 : -2);
        System.out.println("  " + user.getName() + " voted " + type + " on question: " + title);
    }

    @Override
    public int getScore() {
        int score = 0;
        for (Vote v : votes) {
            score += (v.getType() == VoteType.UP) ? 1 : -1;
        }
        return score;
    }

    void addComment(User user, String content) {
        Comment c = new Comment(user, content);
        comments.add(c);
        user.getComments().add(c);
    }

    void addAnswer(Answer answer) {
        answers.add(answer);
    }

    void acceptAnswer(User user, Answer answer) {
        if (user != this.author) {
            System.out.println("  [DENIED] Only the question author can accept an answer!");
            return;
        }
        if (!answers.contains(answer)) {
            System.out.println("  [DENIED] This answer doesn't belong to this question!");
            return;
        }
        this.acceptedAnswer = answer;
        answer.markAsSolution();
        System.out.println("  [ACCEPTED] Answer by " + answer.getAuthor().getName() + " accepted for: " + title);
    }

    User getAuthor() { return author; }
    String getTitle() { return title; }
    List<Tag> getTags() { return tags; }
    List<Answer> getAnswers() { return answers; }
    QuestionStatus getStatus() { return status; }
    void setStatus(QuestionStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "[Q] " + title + " (score: " + getScore() + ", answers: " + answers.size() + ")";
    }
}

class Answer implements Voteable {
    private User author;
    private String content;
    private Date createdAt;
    private Question question;
    private boolean isSolution;
    private List<Comment> comments;
    private List<Vote> votes;

    Answer(User author, String content, Question question) {
        this.author = author;
        this.content = content;
        this.question = question;
        this.isSolution = false;
        this.createdAt = new Date();
        this.comments = new ArrayList<>();
        this.votes = new ArrayList<>();
    }

    @Override
    public void vote(User user, VoteType type) {
        for (Vote v : votes) {
            if (v.getUser() == user) {
                v.setType(type);
                System.out.println("  " + user.getName() + " changed vote to " + type + " on answer by " + author.getName());
                return;
            }
        }
        votes.add(new Vote(user, type));
        author.addReputation(type == VoteType.UP ? 10 : -2);
        System.out.println("  " + user.getName() + " voted " + type + " on answer by " + author.getName());
    }

    @Override
    public int getScore() {
        int score = 0;
        for (Vote v : votes) {
            score += (v.getType() == VoteType.UP) ? 1 : -1;
        }
        return score;
    }

    void addComment(User user, String content) {
        comments.add(new Comment(user, content));
    }

    void markAsSolution() { this.isSolution = true; }

    User getAuthor() { return author; }
    Question getQuestion() { return question; }
    boolean isSolution() { return isSolution; }

    @Override
    public String toString() {
        return "[A] by " + author.getName() + " (score: " + getScore() + ")" + (isSolution ? " [ACCEPTED]" : "");
    }
}

class User {
    private String name;
    private int reputation;
    private List<Question> questions;
    private List<Answer> answers;
    private List<Comment> comments;

    User(String name) {
        this.name = name;
        this.reputation = 0;
        this.questions = new ArrayList<>();
        this.answers = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    void addReputation(int points) {
        this.reputation += points;
        if (this.reputation < 0) this.reputation = 0;
    }

    String getName() { return name; }
    int getReputation() { return reputation; }
    List<Question> getQuestions() { return questions; }
    List<Answer> getAnswers() { return answers; }
    List<Comment> getComments() { return comments; }

    @Override
    public String toString() { return name + " (rep: " + reputation + ")"; }
}

class StackOverflowSystem {
    private List<User> users;
    private List<Question> questions;
    private Map<String, List<Question>> tagIndex;

    StackOverflowSystem() {
        this.users = new ArrayList<>();
        this.questions = new ArrayList<>();
        this.tagIndex = new HashMap<>();
    }

    void registerUser(User user) {
        users.add(user);
        System.out.println("User registered: " + user.getName());
    }

    Question askQuestion(User user, String title, String content, List<Tag> tags) {
        Question question = new Question(user, title, content, tags);
        questions.add(question);
        user.getQuestions().add(question);
        for (Tag tag : tags) {
            tagIndex.computeIfAbsent(tag.getName(), k -> new ArrayList<>()).add(question);
        }
        System.out.println(user.getName() + " asked: " + title);
        return question;
    }

    Answer postAnswer(User user, Question question, String content) {
        Answer answer = new Answer(user, content, question);
        question.addAnswer(answer);
        user.getAnswers().add(answer);
        System.out.println(user.getName() + " answered: " + question.getTitle());
        return answer;
    }

    List<Question> searchByTag(String tagName) {
        return tagIndex.getOrDefault(tagName, Collections.emptyList());
    }

    void vote(User user, Voteable item, VoteType type) {
        item.vote(user, type);
    }
}

public class Stackoverflow {
    public static void main(String[] args) {
        StackOverflowSystem platform = new StackOverflowSystem();

        User alice = new User("Alice");
        User bob = new User("Bob");
        User charlie = new User("Charlie");
        platform.registerUser(alice);
        platform.registerUser(bob);
        platform.registerUser(charlie);

        Tag java = new Tag("java");
        Tag design = new Tag("design-patterns");

        System.out.println("\n--- Asking a Question ---");
        Question q1 = platform.askQuestion(alice,
            "What is the Factory Pattern?",
            "Can someone explain the Factory design pattern with an example?",
            Arrays.asList(java, design));

        System.out.println("\n--- Posting Answers ---");
        Answer a1 = platform.postAnswer(bob, q1,
            "Factory Pattern creates objects without exposing creation logic.");
        Answer a2 = platform.postAnswer(charlie, q1,
            "It's a creational pattern that uses a factory method to create objects.");

        System.out.println("\n--- Voting ---");
        platform.vote(charlie, q1, VoteType.UP);
        platform.vote(alice, a1, VoteType.UP);
        platform.vote(charlie, a1, VoteType.UP);
        platform.vote(alice, a2, VoteType.DOWN);

        System.out.println("\n--- Comments ---");
        q1.addComment(bob, "Great question, I had the same doubt!");
        a1.addComment(alice, "Thanks, this is clear!");

        System.out.println("\n--- Accepting Answer ---");
        q1.acceptAnswer(alice, a1);
        q1.acceptAnswer(bob, a2);

        System.out.println("\n--- Search by Tag ---");
        List<Question> javaQuestions = platform.searchByTag("java");
        System.out.println("Questions tagged 'java': " + javaQuestions);

        System.out.println("\n--- Final State ---");
        System.out.println(q1);
        for (Answer a : q1.getAnswers()) {
            System.out.println("  " + a);
        }
        System.out.println("\nReputations:");
        System.out.println("  " + alice);
        System.out.println("  " + bob);
        System.out.println("  " + charlie);
    }
}
