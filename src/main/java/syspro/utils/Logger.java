package syspro.utils;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static syspro.tm.lexer.Keyword.NULL;


public class Logger {

    String FILENAME = "src/main/java/syspro/utils/log.txt";


    private Stage stage;
    private File logFile;
    private FileWriter writer;


    public enum LogLevel {
        ALL,
        INFO,
        WARNING,
        ERROR,
        DEBUG
    }

    public enum Stage {
        LEXICAL,
        SYNTAX,
        SEMANTIC
    }

    public Logger(Stage stage) {
        this.stage = stage;
        this.logFile = new File(FILENAME);
        try {
            this.writer = new FileWriter(logFile, false);
            writer.write(String.format("\n-------------%s----------\n", stage));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void log(LogLevel level, Stage stage, String message) {
        String msg = String.format("--%s --%s   %s\n", level, stage, message);
        try {
            writer.write(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void info(String message) {
        log(LogLevel.INFO, this.stage, message);
    }

    public void info(Token token, String message) {
        String tokenLength = String.format("<%1$s %2$s>",
                token.start + token.leadingTriviaLength,
                token.end - token.trailingTriviaLength);
        String format = "%1$-20s %2$-5s %3$-10s %4$-10s";
        String tokenInfo = String.format(format,
                token, token.leadingTriviaLength,
                tokenLength,
                token.trailingTriviaLength);
        log(LogLevel.INFO, this.stage, message + tokenInfo);
    }

    public void info(AnySyntaxKind kind, String message) {

        String tokenInfo = String.format("  %s",
                kind.toString());
        log(LogLevel.INFO, this.stage, message.trim() + tokenInfo);

    }

    public void info(SyntaxNode node, String message) {
        List<AnySyntaxKind> slots = new ArrayList<>();

        for (int i = 0; i < node.slotCount(); i++) {
            if (node.slot(i) == null) slots.add(NULL);
            else slots.add(node.slot(i).kind());
        }

        String tokenInfo = String.format("  %s %s",
                node.kind(), slots);
        log(LogLevel.INFO, this.stage, message.trim() + tokenInfo);


    }

    public void warn(String message) {
        log(LogLevel.WARNING, this.stage, message);

    }

    public void error(String message) {
        log(LogLevel.ERROR, this.stage, message);

    }

    public void debug(String message) {
        log(LogLevel.DEBUG, this.stage, message);

    }

    public void updateStage(Stage stage) {
        try {
            writer.write(String.format("\n-------------%s----------\n", stage));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.stage = stage;
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printTree(SyntaxNode head, boolean includeSelf) {
        try {
            writer.write(String.format("\n-------------AST----------\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        printTreeRecursive(head, 0, includeSelf);
    }

    // Helper recursive method to print the tree
    private void printTreeRecursive(SyntaxNode node, int depth, boolean includeSelf) {
        String indent = " ".repeat(depth * 2);
        try {
            AnySyntaxKind kind = NULL;
            String info = " ";
            if (node != null) kind = node.kind();
            if (node != null && node.token() != null) info += node.token().toString();
            writer.write(indent + kind + info + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (node == null) return;

        if (includeSelf) {
            for (int i = 0; i < node.slotCount(); i++) {
                var child = node.slot(i);
                printTreeRecursive(child, depth + 1, includeSelf);

            }
        } else {
            for (int i = node.slotCount() - 1; i >= 0; i--) {
                var child = node.slot(i);
                printTreeRecursive(child, depth + 1, includeSelf);

            }
        }
    }
}
