package buildcraft.lib.script;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.common.Loader;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;

import buildcraft.lib.BCLibProxy;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.script.ScriptAliasFunction.AliasBuilder;

public class SimpleScript {

    public static final boolean DEBUG_ALL = BCDebugging.shouldDebugLog("lib.script");
    private static final FunctionContext CONTEXT = DefaultContexts.createWithAll("scripts");

    static final Gson GSON = new Gson();
    static final Map<String, ScriptActionLoader> functions = new HashMap<>();
    static BufferedWriter logWriter;
    public static boolean debugAll = false;

    static {
        CONTEXT.put_s_b("is_mod_loaded", Loader::isModLoaded);
        // Functions:

        // Debug: turns on script debugging

        // Add: adds a single recipe

        // Remove: removes a recipe (likely from a different datapack)

        // Overwrite: alias for:
        // - remove <old>
        // - add <recipe>

        // Replace: alias for:
        // - if recipe_exists(<old>)
        // - - remove <old>
        // - - add <new>
        // - endif

        // Modify: alias for:
        // - replace <old> <new>
        // except that <new> inherits tags from <old>

        functions.put("debug", script -> {
            String arg = script.nextSimpleArg();
            if ("all".equals(arg)) {
                debugAll = true;
                script.log("Enabling debugging for all scripts. (And all reloads)");
            } else if ("on".equals(arg)) {
                script.isDebugEnabled = true;
                script.log("Enabling debugging for *this* script only");
            } else {
                script.log("Debug must be followed by either 'on' or 'all'");
            }
            return null;
        });
        functions.put("add", script -> {
            String name = script.nextQuotedArg();
            if (name == null) {
                script.log("Missing name!");
                return null;
            }
            JsonObject json = script.nextJson();
            if (json == null) {
                json = script.loadJson(name);
            }
            ResourceLocation id = new ResourceLocation(script.domain, name);
            return ImmutableList.of(new ScriptActionAdd(id, json));
        });
        functions.put("remove", script -> {
            String name = script.nextQuotedArg();
            if (name == null) {
                script.log("Missing name!");
                return null;
            }
            return ImmutableList.of(new ScriptActionRemove(name));
        });
        functions.put("replace", script -> {
            String toRemove = script.nextQuotedArg();
            String toAdd = script.nextQuotedArg();
            if (toRemove == null) {
                script.log("Missing to_remove!");
                return null;
            }
            if (toAdd == null) {
                script.log("Missing to_add!");
                return null;
            }
            JsonObject json = script.nextJson();
            if (json == null) {
                json = script.loadJson(toAdd);
            }
            ResourceLocation id = new ResourceLocation(script.domain, toAdd);
            return ImmutableList.of(new ScriptActionReplace(toRemove, id, json, false));
        });
        functions.put("overwrite", script -> {
            String toRemove = script.nextQuotedArg();
            String toAdd = script.nextQuotedArg();
            if (toRemove == null) {
                script.log("Missing to_remove!");
                return null;
            }
            if (toAdd == null) {
                script.log("Missing to_add!");
                return null;
            }
            JsonObject json = script.nextJson();
            if (json == null) {
                json = script.loadJson(toAdd);
            }
            ResourceLocation id = new ResourceLocation(script.domain, toAdd);
            return ImmutableList.of(new ScriptActionRemove(toRemove), new ScriptActionAdd(id, json));
        });
        functions.put("modify", script -> {
            String toRemove = script.nextQuotedArg();
            String toAdd = script.nextQuotedArg();
            if (toRemove == null) {
                script.log("Missing to_remove!");
                return null;
            }
            if (toAdd == null) {
                script.log("Missing to_add!");
                return null;
            }
            JsonObject json = script.nextJson();
            if (json == null) {
                json = script.loadJson(toAdd);
            }
            ResourceLocation id = new ResourceLocation(script.domain, toAdd);
            return ImmutableList.of(new ScriptActionReplace(toRemove, id, json, true));
        });
    }

    public final String domain;
    public final Path scriptDirRoot;
    public final Path scriptFolder;
    public final String scriptName;
    public final List<ScriptAction> actions = new ArrayList<>();
    public final Map<String, ScriptAliasFunction> customFunctions = new HashMap<>();

    final MutableLineList lines;
    boolean isDebugEnabled;
    Set<String> printedFunctions = null;

    ScriptAliasDocumentation currentDocumentation = null;

    private static File logDir;

    public SimpleScript(ScriptableRegistry<?> registry, Path scriptDirRoot, String scriptDomain, Path scriptFolder,
        Path scriptFile, List<Path> roots, List<String> scriptContents) {
        this.scriptDirRoot = scriptDirRoot;
        this.domain = scriptDomain;
        this.scriptFolder = scriptFolder;
        this.scriptName = scriptFile.getFileName().toString();

        logPure("Found script: ", scriptFile);

        this.lines = new MutableLineList(new SourceFile(scriptName, scriptContents.size()), scriptContents);

        int conditionalLevel = 0;
        int skipLevel = 0;

        LineToken token;
        while ((token = lines.nextToken(true)) != null) {
            if (!token.isValid) {
                continue;
            }
            switch (token.type) {
                case COMMENT:
                    continue;
                case FUNC_DOCS: {
                    currentDocumentation = ScriptAliasDocumentation.parse(token.lines);
                    continue;
                }
                case BACKTICK_STRING:
                case QUOTED_STRING: {
                    log("Found unrelated quoted string!");
                    continue;
                }
                case SEPARATE: {
                    // The rest of the loop handles this
                    break;
                }
                default: {
                    throw new IllegalStateException("Unknown/new enum value: " + token.type);
                }
            }
            if (token.type == TokenType.COMMENT) {
                continue;
            }
            assert token.lines.length == 1 : "The parser shouldn't return tokens with a different length for TokenType.SEPARATE!";
            String function = token.lines[0];
            assert !function.isEmpty() : "The parser shouldn't return empty tokens for TokenType.SEPARATE!";

            if (skipLevel > 0) {
                if ("endif".equals(function)) {
                    skipLevel--;
                    conditionalLevel--;
                    log("endif -- skipped block");
                }
                continue;
            }

            switch (function) {
                case "if": {
                    conditionalLevel++;
                    LineToken conditional = lines.nextToken(false);
                    if (conditional == null) {
                        log("Expected a conditional expression in a quote, but found nothing!");
                        continue;
                    }
                    if (!conditional.isValid || !conditional.type.isString) {
                        log("Found a token that wasn't a string! (or was invalid) '"
                            + Arrays.toString(conditional.lines));
                        continue;
                    }
                    String func = conditional.joinLines(false);
                    boolean shouldCall = false;
                    try {
                        shouldCall = GenericExpressionCompiler.compileExpressionBoolean(func, CONTEXT).evaluate();
                        log("(" + func + ") = " + shouldCall);
                    } catch (InvalidExpressionException e) {
                        log("Invalid " + e.getMessage());
                        e.printStackTrace();
                    }
                    if (!shouldCall) {
                        skipLevel++;
                    }
                    break;
                }
                case "endif": {
                    if (conditionalLevel <= 0) {
                        log("cannot end if without starting one!");
                    }
                    conditionalLevel--;
                    log("endif -- executed block");
                    break;
                }
                case "import": {
                    LineToken srcToken = lines.nextToken(false);
                    if (srcToken == null || !srcToken.isValid
                    // Don't allow multi-line strings explicitly - this is just a file name
                        || srcToken.type != TokenType.QUOTED_STRING) {
                        log("Unknown/invalid import statement!");
                        break;
                    }
                    String source = srcToken.joinLines(false);
                    List<String> replements = loadLinesFromLib(source, registry, roots);
                    if (replements == null) {
                        // Already logged by loadLinesFromLib
                        break;
                    }
                    LineData[] rdata = new LineData[replements.size()];
                    SourceFile file = new SourceFile(source, replements.size());
                    for (int i = 0; i < replements.size(); i++) {
                        rdata[i] = new LineData(replements.get(i), file, i);
                    }
                    if (!lines.replace(token.datas[0], rdata, s -> s)) {
                        log("Recursive import!");
                    }
                    break;
                }
                case "alias": {
                    function = nextSimpleArg();
                    BCLog.logger.info("ALIAS " + function);
                    int startLine = lines.lineIterator.previousIndex();
                    String argCount = nextSimpleArg();

                    if (argCount == null) {
                        log("Expected a number, but got nothing!");
                        break;
                    }

                    int argCountNumber;
                    try {
                        argCountNumber = Integer.parseInt(argCount);
                        if (argCountNumber < 0 || argCountNumber > 50) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException nfe) {
                        log("Expected a number between 0 and 50, but got " + argCount);
                        break;
                    }

                    LineToken next = lines.nextToken(false);
                    if (next == null || !next.isValid) {
                        log("Didn't find a backtick (`) or quote (\") to start the alias function!");
                        break;
                    }

                    LineData[] datas = new LineData[next.lines.length];
                    for (int i = 0; i < datas.length; i++) {
                        String text = next.lines[i];
                        LineData data = next.datas[i];
                        datas[i] = new LineData(data, text);
                    }

                    AliasBuilder builder = new AliasBuilder();
                    builder.name = function;
                    builder.argCount = argCountNumber;
                    builder.rawOutputs = datas;
                    builder.startLine = startLine;
                    builder.docs = currentDocumentation;
                    customFunctions.put(function, new ScriptAliasFunction(builder));
                    currentDocumentation = null;
                    break;
                }
                default: {
                    ScriptActionLoader loader = functions.get(function);
                    if (loader != null) {
                        List<ScriptAction> loadedActions = loader.load(this);
                        if (loadedActions != null) {
                            this.actions.addAll(loadedActions);
                        }
                        break;
                    }
                    BCLog.logger.info("INVOKE " + function);
                    LineData start = token.datas[0];
                    ScriptAliasFunction alias = customFunctions.get(function);
                    if (alias != null) {
                        String[] values = parseArgValues(alias.argCount);
                        if (values != null && !lines.replace(start, alias.rawOutput, createAliasTransform(values))) {
                            log("Overlapped alias functions!");
                        }
                        break;
                    }

                    log("Unknown function " + function);
                    break;
                }
            }
        }

        logPure("");
    }

    private static Function<String, String> createAliasTransform(String[] values) {
        switch (values.length) {
            case 0:
                return Function.identity();
            case 1:
                return s -> s.replace("%0", values[0]);
            case 2:
                return s -> s.replace("%0", values[0]).replace("%1", values[1]);
            case 3:
                return s -> s.replace("%0", values[0]).replace("%1", values[1]).replace("%2", values[2]);
            default: {
                return s -> {
                    for (int i = values.length - 1; i >= 0; i--) {
                        s = s.replace("%" + i, values[i]);
                    }
                    return s;
                };
            }
        }
    }

    @Nullable
    private List<String> loadLinesFromLib(String from, ScriptableRegistry<?> registry, List<Path> roots) {
        int colonIndex = from.indexOf(':');
        if (colonIndex <= 0 || colonIndex + 1 == from.length()) {
            log("Expected a separated string (like buildcraftcore:util), but didn't find a colon in '" + from + "'");
            return null;
        }
        String libDomain = from.substring(0, colonIndex);
        String path = from.substring(colonIndex + 1);
        path_loop: for (Path root : roots) {
            Path full = root.resolve(libDomain + "/compat/" + registry.getEntryType() + "/" + path + ".txt");
            if (!Files.exists(full)) {
                continue;
            }
            try {
                List<String> list = new ArrayList<>(Files.readAllLines(full));
                if (list.isEmpty()) {
                    log("Found a library without any lines! We can't load from this! (" + root + ")");
                    continue;
                }
                if (!"~{buildcraft/json/lib}".equals(list.get(0))) {
                    log("Found a library that isn't declared as '~{buildcraft/json/lib}'! We can't load from this! ("
                        + root + ")");
                    continue;
                }
                list.set(0, "// Valid library declaration was here");

                int i = 1;
                String next = list.get(i);
                if ("/**".equals(next)) {
                    do {
                        i++;
                        if (i >= list.size()) {
                            log("Found endless comment in " + root);
                            break path_loop;
                        }
                        next = list.get(i).trim();
                        if (next.endsWith("*/")) {
                            i++;
                            break;
                        }
                    } while (next.startsWith("*"));
                }
                next = list.get(i);
                String[] argValues = null;
                if (next.startsWith("~args")) {
                    String countStr = next.substring("~args".length()).trim();
                    int count;
                    try {
                        count = Integer.parseInt(countStr);
                        if (count < 0 || count > 50) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException nfe) {
                        log("Expected a number between 0 and 50, but got " + countStr);
                        break;
                    }
                    list.set(i, "// valid args: " + next);
                    argValues = parseArgValues(count);
                }
                if (argValues != null) {
                    for (int a = 0; a < argValues.length; a++) {
                        for (i = 0; i < list.size(); i++) {
                            String str = list.get(i);
                            str = str.replace("${" + a + "}", argValues[a]);
                            list.set(i, str);
                        }
                    }
                }

                for (i = 0; i < list.size(); i++) {
                    String str = list.get(i);
                    str = str.replace("${domain}", domain);
                    list.set(i, str);
                }

                return list;
            } catch (IOException e) {
                log("" + e.getMessage());
            }
        }
        return null;
    }

    @Nullable
    private String[] parseArgValues(int count) {
        BCLog.logger.info("Grabbing " + count + " values");
        String[] args = new String[count];
        boolean invalid = false;
        for (int i = 0; i < count; i++) {
            LineToken next = lines.nextToken(false);
            if (next == null) {
                log("Expected a value, got nothing for the " + toIndexStr(i + 1) + " argument!");
                invalid = true;
                args[i] = "";
            } else if (!next.isValid) {
                log("Expected a value, got an invalid token (" + next + ") for the " + toIndexStr(i + 1)
                    + " argument!");
                invalid = true;
                args[i] = "";
            } else {
                // FIXME: I think we need a way to
                args[i] = next.joinLines(true);
            }
        }
        if (invalid) {
            return null;
        }
        return args;
    }

    private static String toIndexStr(int val) {
        int end = val % 10;
        String strEnd = "th";
        if (end == 1) {
            strEnd = "st";
        } else if (end == 2) {
            strEnd = "nd";
        } else if (end == 3) {
            strEnd = "rd";
        }
        return val + strEnd;
    }

    private String getLineNumber() {
        if (!lines.lineIterator.hasPrevious()) {
            return "0";
        }
        lines.lineIterator.previous();
        return lines.lineIterator.next().lineNumbers;
    }

    void log(String line) {
        if (DEBUG_ALL || debugAll || isDebugEnabled) {
            log0(getLineNumber() + ": " + line);
        }
    }

    void log(String line, Path path) {
        if (DEBUG_ALL || debugAll || isDebugEnabled) {
            log0(getLineNumber() + ": " + line + scriptDirRoot.relativize(path));
        }
    }

    void logPure(String line) {
        if (DEBUG_ALL || debugAll || isDebugEnabled) {
            log0(line);
        }
    }

    void logPure(String line, Path path) {
        if (DEBUG_ALL || debugAll || isDebugEnabled) {
            log0(line + scriptDirRoot.relativize(path));
        }
    }

    public static void logForAll(String line) {
        if (DEBUG_ALL || debugAll) {
            log0(line);
        }
    }

    private static void log0(String line) {
        if (logWriter != null) {
            try {
                logWriter.write(line);
                logWriter.newLine();
            } catch (IOException io) {
                BCLog.logger.warn("[lib.script] Failed to write to the log file!", io);
                closeLog();
            }
        }
        BCLog.logger.info(line);
    }

    public static AutoCloseable createLogFile(String path) {
        logDir = new File(BCLibProxy.getProxy().getGameDirectory(), "logs/buildcraft/scripts");
        try {
            logDir.mkdirs();
            File logFile = new File(logDir, path + ".log");
            logFile.getParentFile().mkdirs();
            logWriter = new BufferedWriter(new FileWriter(logFile));
            return SimpleScript::closeLog;
        } catch (IOException io) {
            BCLog.logger.warn("[lib.script] Failed to open the log file! (" + logDir + ")", io);
            closeLog();
            return () -> {};
        }
    }

    private static void closeLog() {
        if (logWriter != null) {
            try {
                try {
                    logWriter.flush();
                } finally {
                    logWriter.close();
                    logWriter = null;
                }
            } catch (IOException io) {
                BCLog.logger.warn(
                    "[lib.script] Failed to close the log file, so it might not be complete! (" + logDir + ")", io);
            }
        }
    }

    // Script loading functionality

    /** Attempts to parse the next stage as a normal argument, or returns the empty string if the end of the line has
     * been reached. */
    String nextSimpleArg() {
        LineToken next = lines.nextToken(false);
        if (next == null || !next.isValid || next.type != TokenType.SEPARATE) {
            return "";
        }
        return next.joinLines(false);
    }

    @Nullable
    String nextQuotedArg() {
        LineToken next = lines.nextToken(false);
        if (next == null || !next.isValid) {
            return null;
        }
        return next.joinLines(false);
    }

    /** Like {@link #nextSimpleArg()}, but is quote-aware when searching. Also removes said quotes. Returns null if no
     * valid argument was found. */
    @Nullable
    String[] nextQuotedArgAsArray() {
        String[] arr = null;
        LineToken next = lines.nextToken(false);
        if (next == null || !next.isValid) {

        } else {
            arr = next.lines;
        }
        return arr;
    }

    /** Attempts to parse the next stage as a JSON object, or returns null if the end of the line has been reached. */
    @Nullable
    JsonObject nextJson() {
        String multiLine = nextQuotedArg();
        try {
            return GSON.fromJson(multiLine, JsonObject.class);
        } catch (JsonSyntaxException jse) {
            log("Invalid JSON: " + jse.getMessage());
            return null;
        }
    }

    @Nullable
    JsonObject loadJson(String path) {
        Path jsonPath = scriptFolder.resolve(path + ".json");
        if (Files.exists(jsonPath)) {
            try (BufferedReader reader = Files.newBufferedReader(jsonPath)) {
                return GSON.fromJson(reader, JsonObject.class);
            } catch (IOException io) {
                log("Unable to read the file! " + io.getMessage());
                return null;
            } catch (JsonSyntaxException jse) {
                log("Invalid JSON: " + jse.getMessage());
                return null;
            }
        } else {
            log("Couldn't find the resource: ", jsonPath);
            return null;
        }
    }

    public enum TokenType {
        /** "string" */
        QUOTED_STRING(true),
        /** `a<br/>
         * multi<br/>
         * line<br/>
         * string` */
        BACKTICK_STRING(true),
        /** functionName */
        SEPARATE(false),
        /** // Comment */
        COMMENT(false),
        /** {@literal /}** Comment<br/>
         * * % argument comment<br/>
         * *{@literal /} */
        FUNC_DOCS(false);

        public final boolean isString;

        private TokenType(boolean isString) {
            this.isString = isString;
        }
    }

    public static final class LineToken {
        public final String[] lines;
        public final LineData[] datas;
        public final TokenType type;
        public final boolean isValid;

        public LineToken(String singleLine, LineData data, TokenType type, boolean isValid) {
            this(new String[] { singleLine }, new LineData[] { data }, type, isValid);
        }

        public String joinLines(boolean separateWithNewLine) {
            switch (lines.length) {
                case 0:
                    return "";
                case 1:
                    return lines[0];
                default:
                    break;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(lines[0]);
            for (int i = 1; i < lines.length; i++) {
                if (separateWithNewLine) {
                    sb.append('\n');
                }
                sb.append(lines[i]);
            }
            return sb.toString();
        }

        public LineToken(String[] lines, LineData[] datas, TokenType type, boolean isValid) {
            if (type == TokenType.BACKTICK_STRING || type == TokenType.QUOTED_STRING) {
                char ctype = type == TokenType.BACKTICK_STRING ? '`' : '"';
                StringBuilder sb = new StringBuilder();
                for (int l = 0; l < lines.length; l++) {
                    String line = lines[l];
                    if (line.length() <= 1) {
                        continue;
                    }
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        char n = i + 1 == line.length() ? '-' : line.charAt(i + 1);
                        if (c == '\\' && n == ctype) {
                            i++;
                            sb.append(n);
                            continue;
                        }
                        sb.append(c);
                    }
                    lines[l] = sb.toString();
                    sb.setLength(0);
                }
            }
            this.lines = lines;
            this.datas = datas;
            this.type = type;
            this.isValid = isValid;
            BCLog.logger.info("nextToken() = " + isValid + type + Arrays.toString(lines));
        }
    }

    public static class MutableLineList {
        public final SourceFile file;
        private final List<LineData> lines = new LinkedList<>();
        private final ListIterator<LineData> lineIterator =

            lines.listIterator();
        private final Object o =

            new ListIterator<SimpleScript.LineData>() {
                final ListIterator<LineData> real = lines.listIterator();

                @Override
                public boolean hasNext() {
                    return real.hasNext();
                }

                @Override
                public LineData next() {
                    LineData value = real.next();
                    BCLog.logger.info("next() = " + value);
                    return value;
                }

                @Override
                public boolean hasPrevious() {
                    return real.hasPrevious();
                }

                @Override
                public LineData previous() {
                    LineData value = real.previous();
                    BCLog.logger.info("previous() = " + value);
                    return value;
                }

                @Override
                public int nextIndex() {
                    return real.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return real.previousIndex();
                }

                @Override
                public void remove() {
                    BCLog.logger.info("remove()");
                    real.remove();
                }

                @Override
                public void set(LineData e) {
                    BCLog.logger.info("set(" + e + ")");
                    real.set(e);
                }

                @Override
                public void add(LineData e) {
                    BCLog.logger.info("add(" + e + ")");
                    real.add(e);
                }

            };
        private int currentIndexInLine = -1;

        public MutableLineList(SourceFile file, List<String> rawData) {
            this.file = file;
            for (int i = rawData.size() - 1; i >= 0; i--) {
                String line = rawData.get(i);
                lineIterator.add(new LineData(line, file, i));
                lineIterator.previous();
            }
        }

        /** @param jumpToNextLine If true then also search the lines after the current one for the token. Used by
         *            function callers as arguments must all be on the same line (unless '¬' is found)
         * @return The next token (separated into an array as the indervidual lines) or null if there was no token. The
         *         returned token will never have an empty array. */
        @Nullable
        public LineToken nextToken(boolean jumpToNextLine) {
            boolean isComment = false;
            int start = -1;
            LineData data;
            String line;
            boolean foundNextLineSymbol = false;
            start_search_line: do {
                foundNextLineSymbol = false;
                if (!lineIterator.hasNext()) {
                    return null;
                }
                line = (data = lineIterator.next()).text;
                boolean isMultiLine = false;
                char end = ' ';
                start_search: for (int i = Math.max(0, currentIndexInLine); i < line.length(); i++) {
                    char c = line.charAt(i);
                    switch (c) {
                        case ' ': {
                            continue;
                        }
                        case '/': {
                            if (i + 1 == line.length()) {
                                return new LineToken(line.substring(i), data, TokenType.COMMENT, false);
                            }
                            isComment = true;
                            if (!line.startsWith("/**", i)) {
                                // The comment extends for the rest of the line
                                currentIndexInLine = -1;
                                // Don't go to the previous() element
                                return new LineToken(line.substring(i), data, TokenType.COMMENT,
                                    line.startsWith("//", i));
                            }
                            start = i + 3;
                            break start_search;
                        }
                        case '"': {
                            end = '"';
                            start = i + 1;
                            break start_search;
                        }
                        case '`': {
                            end = '`';
                            isMultiLine = true;
                            start = i + 1;
                            break start_search;
                        }
                        case '¬': {
                            boolean isLast = true;
                            // Check to ensure that this is the last char
                            for (int j = i; j < line.length(); j++) {
                                if (!Character.isWhitespace(line.charAt(j))) {
                                    if (!line.startsWith("//", j)) {
                                        isLast = false;
                                    }
                                    break;
                                }
                            }
                            if (isLast) {
                                foundNextLineSymbol = true;
                                currentIndexInLine = -1;
                                continue start_search_line;
                            }
                            // Intentionally treat it as part of a separate token
                        }
                        //$FALL-THROUGH$ (Intentional)
                        default: {
                            if (Character.isWhitespace(c)) {
                                continue;
                            }
                            // Must be the start of a single-word element
                            for (int j = i; j < line.length(); j++) {
                                char d = line.charAt(j);
                                if (Character.isWhitespace(d)) {
                                    currentIndexInLine = j + 1;
                                    // Ensure that the next iteration will take the line
                                    lineIterator.previous();
                                    return new LineToken(line.substring(i, j), data, TokenType.SEPARATE, true);
                                }
                            }
                            currentIndexInLine = line.length();
                            // Ensure that the next iteration will take the line
                            lineIterator.previous();
                            return new LineToken(line.substring(i), data, TokenType.SEPARATE, true);
                        }
                    }
                }
                if (start < 0) {
                    currentIndexInLine = -1;
                    continue;
                }
                if (isComment) {
                    for (int i = start; i < line.length(); i++) {
                        if (line.startsWith("*/", i)) {
                            currentIndexInLine = i + 3;
                            // Ensure that the next iteration will take the line
                            lineIterator.previous();
                            return new LineToken(line.substring(start, i + 3), data, TokenType.FUNC_DOCS, true);
                        }
                    }

                    break start_search_line;
                } else {
                    for (int i = start; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == '\\') {
                            i++;
                            continue;
                        }
                        if (c == end) {
                            currentIndexInLine = i + 1;
                            // Ensure that the next iteration will take the line
                            lineIterator.previous();
                            return new LineToken(line.substring(start, i), data, TokenType.QUOTED_STRING, true);
                        }
                    }
                    if (!isMultiLine) {
                        currentIndexInLine = line.length();
                        // Ensure that the next iteration will take the line
                        lineIterator.previous();

                        // Invalid token - we found the start but not the end
                        // so we'll return the invalid part
                        return new LineToken(line.substring(start + 1), data, TokenType.BACKTICK_STRING, false);
                    }
                    break start_search_line;
                }
            } while (jumpToNextLine || foundNextLineSymbol);
            if (start < 0) {
                return null;
            }
            // Now we have:
            // - a multiline thing
            // - that is either a /* \n* \n* \n*/
            // - or ` data \n d `
            List<String> tokenLines = new ArrayList<>();
            List<LineData> tokenData = new ArrayList<>();
            tokenLines.add(line.substring(start));
            tokenData.add(data);
            line_loop: while (true) {
                if (!lineIterator.hasNext()) {
                    return null;
                }
                line = (data = lineIterator.next()).text;
                if (isComment) {
                    if (!line.trim().startsWith("*")) {
                        // Tiny theoretical safety feature
                        break;
                    }
                    int end = line.indexOf("*/");
                    if (end >= 0) {
                        currentIndexInLine = end + 2;
                        // Ensure that the next iteration will take the line
                        lineIterator.previous();
                        tokenLines.add(line.substring(0, end));
                        tokenData.add(data);
                        break;
                    }
                    line = line.substring(line.indexOf('*') + 1);
                } else {
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == '\\') {
                            i++;
                            continue;
                        }
                        if (c == '`') {
                            currentIndexInLine = i + 1;
                            // Ensure that the next iteration will take the line
                            lineIterator.previous();
                            tokenLines.add(line.substring(0, i));
                            tokenData.add(data);
                            break line_loop;
                        }
                    }
                }
                tokenLines.add(line);
                tokenData.add(data);
            }
            return new LineToken(tokenLines.toArray(new String[0]), tokenData.toArray(new LineData[0]),
                isComment ? TokenType.FUNC_DOCS : TokenType.BACKTICK_STRING, true);
        }

        public boolean replace(LineData start, LineData[] with, Function<String, String> transform) {
            SourceLine srcLine = with[0].original;
            List<LineData> removed = new ArrayList<>();
            lineIterator.next();
            while (true) {// Safe because if we run out of elements it's a bug anyway
                LineData line = lineIterator.previous();
                if (line == start) {
                    lineIterator.remove();
                    // Mark it as valid
                    removed = null;
                    break;
                }
                if (line.firstLineSources.contains(srcLine)) {
                    BCLog.logger.warn("Overlap: " + srcLine + ", " + line);
                    break;
                }
                lineIterator.remove();
                removed.add(line);
            }
            if (removed != null) {
                // We found an overlapping line
                for (LineData line : removed) {
                    lineIterator.add(line);
                }
                return false;
            }
            int line = lineIterator.nextIndex();
            for (LineData other : with) {
                lineIterator.add(other.createReplacement(transform.apply(other.text), srcLine, line++));
            }
            // Go back to the very first line
            for (int i = 0; i < with.length; i++) {
                lineIterator.previous();
            }
            currentIndexInLine = -1;
            return true;
        }

        public int size() {
            return lines.size();
        }
    }

    public static class LineData {
        public final String text;
        public final String lineNumbers;
        public final SourceLine original;
        /** All sources. Doesn't include the original. */
        public final ImmutableList<SourceLine> firstLineSources;

        public LineData(String text, SourceFile file, int line) {
            this.text = text;
            this.original = new SourceLine(file, line);
            this.firstLineSources = ImmutableList.of();
            this.lineNumbers = line + 1 + "";
        }

        public LineData(LineData from, String text) {
            this.text = text;
            this.lineNumbers = from.lineNumbers;
            this.original = from.original;
            this.firstLineSources = from.firstLineSources;
        }

        private LineData(String text, LineData original, int nextLine, ImmutableList<SourceLine> sources) {
            this.text = text;
            this.original = original.original;
            this.firstLineSources = sources;
            this.lineNumbers = original.lineNumbers + "(" + (nextLine + 1) + ")";
        }

        public LineData createReplacement(String newText, SourceLine newSource, int newLine) {
            ImmutableList<SourceLine> list =
                ImmutableList.<SourceLine> builder().addAll(firstLineSources).add(newSource).build();
            return new LineData(newText, this, newLine, list);
        }

        @Override
        public String toString() {
            return original + " " + lineNumbers + ": " + firstLineSources + " " + text;
        }
    }

    public static final class SourceLine {
        public final SourceFile file;
        public final int line;

        public SourceLine(SourceFile file, int line) {
            this.file = file;
            this.line = line;
        }

        public void appendLineNumber(StringBuilder sb) {
            // Actually we don't do this do we?
            sb.append(line);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, line);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            SourceLine other = (SourceLine) obj;
            return line == other.line && file.equals(other.file);
        }

        @Override
        public String toString() {
            return file.name + "." + (line + 1);
        }
    }

    public static final class SourceFile {
        public final String name;
        public final int lineCount;
        private final int lineWidth;

        public SourceFile(String name, int lineCount) {
            this.name = name;
            this.lineCount = lineCount;
            lineWidth = Integer.toString(lineCount).length();
        }
    }

    public interface ScriptActionLoader {
        List<ScriptAction> load(SimpleScript script);
    }

    public static abstract class ScriptAction {
        public JsonObject getJson() {
            throw new UnsupportedOperationException(getClass() + " doesn't support getJson()!");
        }
    }

    public static class ScriptActionRemove extends ScriptAction {
        public final ResourceLocation name;

        public ScriptActionRemove(String name) {
            this.name = new ResourceLocation(name);
        }
    }

    public static class ScriptActionAdd extends ScriptAction {
        public final ResourceLocation name;
        public final JsonObject json;

        public ScriptActionAdd(ResourceLocation name, JsonObject json) {
            this.name = name;
            this.json = json;
        }

        @Override
        public JsonObject getJson() {
            return json;
        }
    }

    public static class ScriptActionReplace extends ScriptAction {
        public final ResourceLocation toReplace, name;
        public final boolean inheritTags;
        public final JsonObject json;

        public ScriptActionReplace(String toReplace, ResourceLocation name, JsonObject json, boolean inheritTags) {
            this.toReplace = new ResourceLocation(toReplace);
            this.name = name;
            this.json = json;
            this.inheritTags = inheritTags;
        }

        public ScriptActionAdd convertToAdder() {
            return new ScriptActionAdd(name, json);
        }

        @Override
        public JsonObject getJson() {
            return json;
        }
    }
}