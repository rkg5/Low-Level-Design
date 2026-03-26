import java.util.*;

enum State {
    START, PLAYING, WON
}

class Snake {
    private final int head;
    private final int tail;

    Snake(int head, int tail) {
        if (head <= tail) throw new IllegalArgumentException("Snake head must be > tail");
        this.head = head;
        this.tail = tail;
    }

    int getHead() { return head; }
    int getTail() { return tail; }

    @Override
    public String toString() {
        return "Snake[" + head + "->" + tail + "]";
    }
}

class Ladder {
    private final int bottom;
    private final int top;

    Ladder(int bottom, int top) {
        if (top <= bottom) throw new IllegalArgumentException("Ladder top must be > bottom");
        this.bottom = bottom;
        this.top = top;
    }

    int getBottom() { return bottom; }
    int getTop()    { return top; }

    @Override
    public String toString() {
        return "Ladder[" + bottom + "->" + top + "]";
    }
}

class Player {
    private final String name;
    private int pos;
    private State state;

    Player(String name) {
        this.name = name;
        this.pos = 0;
        this.state = State.START;
    }

    String getName()    { return name; }
    int getPos()        { return pos; }
    State getState()    { return state; }
    void setPos(int pos)       { this.pos = pos; }
    void setState(State state) { this.state = state; }

    @Override
    public String toString() {
        return name + "(pos=" + pos + ", state=" + state + ")";
    }
}

class Dice {
    private final Random random = new Random();
    private final int faces;

    Dice(int faces) {
        this.faces = faces;
    }

    int roll() {
        return random.nextInt(faces) + 1;
    }
}

class Board {
    private final int size;
    private final Map<Integer, Integer> snakeMap;
    private final Map<Integer, Integer> ladderMap;

    Board(int size) {
        this.size = size;
        this.snakeMap = new HashMap<>();
        this.ladderMap = new HashMap<>();
    }

    int getSize() { return size; }

    void addSnake(Snake snake) {
        if (snake.getHead() >= size || snake.getTail() < 1)
            throw new IllegalArgumentException("Snake " + snake + " is out of board bounds [1," + (size-1) + "]");
        if (ladderMap.containsKey(snake.getHead()))
            throw new IllegalArgumentException("Cell " + snake.getHead() + " already has a ladder!");
        if (snakeMap.containsKey(snake.getHead()))
            throw new IllegalArgumentException("Cell " + snake.getHead() + " already has a snake!");
        snakeMap.put(snake.getHead(), snake.getTail());
    }

    void addLadder(Ladder ladder) {
        if (ladder.getTop() >= size || ladder.getBottom() < 1)
            throw new IllegalArgumentException("Ladder " + ladder + " is out of board bounds [1," + (size-1) + "]");
        if (snakeMap.containsKey(ladder.getBottom()))
            throw new IllegalArgumentException("Cell " + ladder.getBottom() + " already has a snake!");
        if (ladderMap.containsKey(ladder.getBottom()))
            throw new IllegalArgumentException("Cell " + ladder.getBottom() + " already has a ladder!");
        ladderMap.put(ladder.getBottom(), ladder.getTop());
    }

    int getFinalPosition(int pos) {
        boolean moved = true;
        while (moved) {
            moved = false;
            if (snakeMap.containsKey(pos)) {
                System.out.println("   [SNAKE] Snake at " + pos + "! Sliding down to " + snakeMap.get(pos));
                pos = snakeMap.get(pos);
                moved = true;
            } else if (ladderMap.containsKey(pos)) {
                System.out.println("   [LADDER] Ladder at " + pos + "! Climbing up to " + ladderMap.get(pos));
                pos = ladderMap.get(pos);
                moved = true;
            }
        }
        return pos;
    }
}

class Game {
    private final Board board;
    private final Dice dice;
    private final Queue<Player> playerQueue;

    Game(Board board, Dice dice) {
        this.board = board;
        this.dice = dice;
        this.playerQueue = new LinkedList<>();
    }

    void addPlayer(Player player) {
        playerQueue.offer(player);
    }

    void play() {
        if (playerQueue.size() < 2)
            throw new IllegalStateException("Need at least 2 players to start!");

        System.out.println("=== Snake and Ladder Game Started! Board size: " + board.getSize() + " ===");
        System.out.println("Players: " + playerQueue + "\n");

        for (Player p : playerQueue) {
            p.setState(State.PLAYING);
        }

        int round = 0;

        while (playerQueue.size() > 1 || playerQueue.peek().getState() != State.WON) {
            round++;
            System.out.println("--- Round " + round + " ---");

            int playersThisRound = playerQueue.size();
            boolean someoneWon = false;

            for (int i = 0; i < playersThisRound && !someoneWon; i++) {
                Player current = playerQueue.poll();

                int diceValue = dice.roll();
                int oldPos = current.getPos();
                int newPos = oldPos + diceValue;

                System.out.println(current.getName() + " rolled " + diceValue
                        + " | pos: " + oldPos + " -> " + newPos);

                if (newPos > board.getSize()) {
                    System.out.println("   [X] Overshoot! Stays at " + oldPos);
                    playerQueue.offer(current);
                    continue;
                }

                if (newPos == board.getSize()) {
                    current.setPos(newPos);
                    current.setState(State.WON);
                    System.out.println("   [WIN] " + current.getName() + " reached " + board.getSize() + " and WINS!");
                    someoneWon = true;
                    continue;
                }

                newPos = board.getFinalPosition(newPos);
                current.setPos(newPos);
                System.out.println("   [OK] " + current.getName() + " -> position " + current.getPos());
                playerQueue.offer(current);
            }
            System.out.println();

            if (someoneWon) break;
        }

        System.out.println("=== Game Over! ===");
    }
}

public class SnakeAndLadder {
    public static void main(String[] args) {
        Board board = new Board(100);

        board.addLadder(new Ladder(2, 38));
        board.addLadder(new Ladder(7, 14));
        board.addLadder(new Ladder(8, 31));
        board.addLadder(new Ladder(15, 26));
        board.addLadder(new Ladder(28, 84));
        board.addLadder(new Ladder(51, 67));
        board.addLadder(new Ladder(71, 91));
        board.addLadder(new Ladder(78, 98));

        board.addSnake(new Snake(16, 6));
        board.addSnake(new Snake(46, 25));
        board.addSnake(new Snake(49, 11));
        board.addSnake(new Snake(62, 19));
        board.addSnake(new Snake(64, 60));
        board.addSnake(new Snake(87, 24));
        board.addSnake(new Snake(93, 73));
        board.addSnake(new Snake(95, 75));
        board.addSnake(new Snake(99, 78));

        Dice dice = new Dice(6);

        Game game = new Game(board, dice);
        game.addPlayer(new Player("Alice"));
        game.addPlayer(new Player("Bob"));

        game.play();
    }
}
