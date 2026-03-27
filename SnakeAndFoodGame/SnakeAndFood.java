import java.util.*;

enum Direction {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    final int dx, dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }
}

enum GameState { PLAYING, PAUSED, GAME_OVER }

class Cell {
    int x, y;

    Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cell)) return false;
        Cell c = (Cell) o;
        return x == c.x && y == c.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() { return "(" + x + "," + y + ")"; }
}

class Snake {
    private Deque<Cell> body;
    private Direction direction;
    private int growPending;

    Snake(Cell start, Direction direction) {
        this.body = new LinkedList<>();
        this.body.addFirst(start);
        this.direction = direction;
        this.growPending = 0;
    }

    Cell getHead() { return body.peekFirst(); }
    Deque<Cell> getBody() { return body; }
    Direction getDirection() { return direction; }

    void setDirection(Direction newDir) {
        // Prevent 180-degree reversal (can't go LEFT if going RIGHT)
        if (this.direction.dx + newDir.dx == 0 && this.direction.dy + newDir.dy == 0) {
            return;
        }
        this.direction = newDir;
    }

    Cell nextHead() {
        Cell head = getHead();
        return new Cell(head.x + direction.dx, head.y + direction.dy);
    }

    void move(Cell newHead) {
        body.addFirst(newHead);
        if (growPending > 0) {
            growPending--;
        } else {
            body.removeLast();
        }
    }

    void grow() {
        growPending++;
    }

    boolean collidesWithSelf(Cell position) {
        return body.contains(position);
    }

    int length() { return body.size(); }
}

class Food {
    private Cell position;
    private Random random;

    Food(Random random) {
        this.random = random;
    }

    Cell getPosition() { return position; }

    void spawn(int width, int height, Deque<Cell> snakeBody) {
        Cell candidate;
        do {
            candidate = new Cell(random.nextInt(width), random.nextInt(height));
        } while (snakeBody.contains(candidate));
        this.position = candidate;
    }
}

class Board {
    private int width, height;

    Board(int width, int height) {
        this.width = width;
        this.height = height;
    }

    boolean isOutOfBounds(Cell cell) {
        return cell.x < 0 || cell.x >= width || cell.y < 0 || cell.y >= height;
    }

    int getWidth() { return width; }
    int getHeight() { return height; }
}

class Game {
    private Board board;
    private Snake snake;
    private Food food;
    private int score;
    private GameState state;
    private Random random;

    Game(int width, int height) {
        this.board = new Board(width, height);
        this.random = new Random();

        Cell startPos = new Cell(width / 2, height / 2);
        this.snake = new Snake(startPos, Direction.RIGHT);
        this.food = new Food(random);
        this.food.spawn(width, height, snake.getBody());
        this.score = 0;
        this.state = GameState.PLAYING;
    }

    void changeDirection(Direction dir) {
        if (state != GameState.PLAYING) return;
        snake.setDirection(dir);
    }

    void tick() {
        if (state != GameState.PLAYING) return;

        Cell newHead = snake.nextHead();

        if (board.isOutOfBounds(newHead)) {
            state = GameState.GAME_OVER;
            return;
        }

        if (snake.collidesWithSelf(newHead)) {
            state = GameState.GAME_OVER;
            return;
        }

        snake.move(newHead);

        if (newHead.equals(food.getPosition())) {
            snake.grow();
            score++;
            food.spawn(board.getWidth(), board.getHeight(), snake.getBody());
        }
    }

    void printBoard() {
        System.out.println("Score: " + score + " | Length: " + snake.length());
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                Cell c = new Cell(x, y);
                if (c.equals(snake.getHead())) {
                    System.out.print("@ ");
                } else if (snake.getBody().contains(c)) {
                    System.out.print("O ");
                } else if (c.equals(food.getPosition())) {
                    System.out.print("* ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    GameState getState() { return state; }
    int getScore() { return score; }
}

public class SnakeAndFood {
    public static void main(String[] args) {
        Game game = new Game(10, 10);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Snake & Food Game ===");
        System.out.println("Controls: W=UP, S=DOWN, A=LEFT, D=RIGHT, Q=QUIT\n");

        game.printBoard();

        while (game.getState() == GameState.PLAYING) {
            System.out.print("Move: ");
            String input = scanner.nextLine().trim().toUpperCase();

            switch (input) {
                case "W": game.changeDirection(Direction.UP); break;
                case "S": game.changeDirection(Direction.DOWN); break;
                case "A": game.changeDirection(Direction.LEFT); break;
                case "D": game.changeDirection(Direction.RIGHT); break;
                case "Q":
                    System.out.println("Quit. Final score: " + game.getScore());
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid input. Use W/A/S/D.");
                    continue;
            }

            game.tick();
            game.printBoard();
        }

        System.out.println("GAME OVER! Final score: " + game.getScore());
        scanner.close();
    }
}