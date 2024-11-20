package syspro.utils;

import syspro.tm.lexer.Token;
import syspro.tm.parser.AnySyntaxKind;
import syspro.tm.parser.SyntaxKind;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


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
        String msg = String.format("--%s --%s   %s", level, stage, message);
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
        String format = "%1$-20s %2$-5s %3$-10s %4$-10s\n";
        String tokenInfo = String.format(format,
                token.toString(), token.leadingTriviaLength,
                tokenLength,
                token.trailingTriviaLength);
        log(LogLevel.INFO, this.stage, message + tokenInfo);
    }

    public void info(AnySyntaxKind node, String message) {

        String tokenInfo = String.format("Node: %s\n",
                node.toString());
        log(LogLevel.INFO, this.stage, message.trim().indent(1) + tokenInfo);

    }

    public void info(SyntaxKind kind, String message) {
        log(LogLevel.INFO, this.stage, message);

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
}
