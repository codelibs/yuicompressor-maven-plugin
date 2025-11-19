/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released May 6, 1998.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.codelibs.maven.yuicompressor;

import org.mozilla.javascript.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Basic Rhino Shell for executing JavaScript.
 * <p>
 * Can execute scripts interactively or in batch mode at the command line.
 * Based on Mozilla Rhino's BasicRhinoShell example.
 *
 * @author Norris Boyd
 * @author David Bernard
 * @author CodeLibs Project
 */
@SuppressWarnings("serial")
public class BasicRhinoShell extends ScriptableObject {

    private boolean quitting;

    @Override
    public String getClassName() {
        return "global";
    }

    /**
     * Main entry point for executing JavaScript.
     * <p>
     * Process arguments and set up the execution environment.
     *
     * @param args     command line arguments
     * @param reporter error reporter for JavaScript errors
     */
    public static void exec(final String[] args, final ErrorReporter reporter) {
        final Context cx = Context.enter();
        cx.setErrorReporter(reporter);
        try {
            final BasicRhinoShell shell = new BasicRhinoShell();
            cx.initStandardObjects(shell);

            // Define global functions
            final String[] names = {"print", "quit", "version", "load", "help", "readFile", "warn"};
            shell.defineFunctionProperties(names, BasicRhinoShell.class, ScriptableObject.DONTENUM);

            final String[] processedArgs = processOptions(cx, args);

            // Set up "arguments" in the global scope
            final Object[] array;
            if (processedArgs.length == 0) {
                array = new Object[0];
            } else {
                final int length = processedArgs.length - 1;
                array = new Object[length];
                System.arraycopy(processedArgs, 1, array, 0, length);
            }
            final Scriptable argsObj = cx.newArray(shell, array);
            shell.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);

            shell.processSource(cx, processedArgs.length == 0 ? null : processedArgs[0]);
        } finally {
            Context.exit();
        }
    }

    /**
     * Parse and process command line options.
     *
     * @param cx   the JavaScript context
     * @param args command line arguments
     * @return remaining arguments after options are processed
     */
    public static String[] processOptions(final Context cx, final String[] args) {
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (!arg.startsWith("-")) {
                final String[] result = new String[args.length - i];
                System.arraycopy(args, i, result, 0, result.length);
                return result;
            }
            if ("-version".equals(arg)) {
                if (++i == args.length) {
                    usage(arg);
                }
                final double d = Context.toNumber(args[i]);
                if (Double.isNaN(d)) {
                    usage(arg);
                }
                cx.setLanguageVersion((int) d);
                continue;
            }
            usage(arg);
        }
        return new String[0];
    }

    /**
     * Print usage message and throw exception.
     *
     * @param arg the problematic argument
     * @throws IllegalArgumentException always thrown with usage information
     */
    private static void usage(final String arg) {
        final StringBuilder msg = new StringBuilder();
        msg.append("Invalid argument: \"").append(arg).append("\"\n");
        msg.append("Valid arguments are:\n");
        msg.append("-version <n> (100=JS 1.0, 110=JS 1.1, 120=JS 1.2, 130=JS 1.3, ");
        msg.append("140=JS 1.4, 150=JS 1.5, 160=JS 1.6, 170=JS 1.7)");
        throw new IllegalArgumentException(msg.toString());
    }

    /**
     * Display help message.
     * <p>
     * This method is defined as a JavaScript function.
     */
    public void help() {
        p("");
        p("Command                Description");
        p("=======                ===========");
        p("help()                 Display usage and help messages.");
        p("defineClass(className) Define an extension using the Java class");
        p("                       named with the string argument.");
        p("                       Uses ScriptableObject.defineClass().");
        p("load(['foo.js', ...])  Load JavaScript source files named by");
        p("                       string arguments.");
        p("loadClass(className)   Load a class named by a string argument.");
        p("                       The class must be a script compiled to a");
        p("                       class file.");
        p("print([expr ...])      Evaluate and print expressions.");
        p("quit()                 Quit the shell.");
        p("version([number])      Get or set the JavaScript version number.");
        p("");
    }

    /**
     * Print the string values of arguments.
     * <p>
     * This method is defined as a JavaScript function.
     */
    public static void print(final Context cx, final Scriptable thisObj, final Object[] args, final Function funObj) {
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                System.out.print(" ");
            }
            System.out.print(Context.toString(args[i]));
        }
        System.out.println();
    }

    /**
     * Quit the shell (only affects interactive mode).
     * <p>
     * This method is defined as a JavaScript function.
     */
    public void quit() {
        quitting = true;
    }

    /**
     * Report a warning.
     * <p>
     * This method is defined as a JavaScript function.
     */
    public static void warn(final Context cx, final Scriptable thisObj, final Object[] args, final Function funObj) {
        final String message = Context.toString(args[0]);
        final int line = (int) Context.toNumber(args[1]);
        final String source = Context.toString(args[2]);
        final int column = (int) Context.toNumber(args[3]);
        cx.getErrorReporter().warning(message, null, line, source, column);
    }

    /**
     * Read a file and return its contents as a string.
     * <p>
     * This method is defined as a JavaScript function.
     *
     * @param path the file path
     * @return file contents
     * @throws RuntimeException if file cannot be read
     */
    public String readFile(final String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException exc) {
            throw new RuntimeException("Failed to read file: " + path, exc);
        }
    }

    /**
     * Get and set the JavaScript language version.
     * <p>
     * This method is defined as a JavaScript function.
     */
    public static double version(final Context cx, final Scriptable thisObj, final Object[] args, final Function funObj) {
        final double result = cx.getLanguageVersion();
        if (args.length > 0) {
            final double d = Context.toNumber(args[0]);
            cx.setLanguageVersion((int) d);
        }
        return result;
    }

    /**
     * Load and execute JavaScript source files.
     * <p>
     * This method is defined as a JavaScript function.
     */
    public static void load(final Context cx, final Scriptable thisObj, final Object[] args, final Function funObj) {
        final BasicRhinoShell shell = (BasicRhinoShell) getTopLevelScope(thisObj);
        for (final Object element : args) {
            shell.processSource(cx, Context.toString(element));
        }
    }

    /**
     * Evaluate JavaScript source from file or interactive mode.
     *
     * @param cx       the JavaScript context
     * @param filename the file to compile, or null for interactive mode
     */
    private void processSource(final Context cx, final String filename) {
        if (filename == null) {
            processInteractive(cx);
        } else {
            processFile(cx, filename);
        }
    }

    private void processInteractive(final Context cx) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            final String sourceName = "<stdin>";
            int lineno = 1;
            boolean hitEOF = false;

            do {
                final int startline = lineno;
                System.err.print("js> ");
                System.err.flush();

                try {
                    final StringBuilder sourceBuilder = new StringBuilder();
                    // Collect lines of source to compile
                    while (true) {
                        final String newline = in.readLine();
                        if (newline == null) {
                            hitEOF = true;
                            break;
                        }
                        sourceBuilder.append(newline).append("\n");
                        lineno++;

                        if (cx.stringIsCompilableUnit(sourceBuilder.toString())) {
                            break;
                        }
                    }

                    final Object result = cx.evaluateString(this, sourceBuilder.toString(), sourceName, startline, null);
                    if (result != Context.getUndefinedValue()) {
                        System.err.println(Context.toString(result));
                    }
                } catch (WrappedException we) {
                    System.err.println(we.getWrappedException().toString());
                } catch (EvaluatorException | JavaScriptException ee) {
                    System.err.println("js: " + ee.getMessage());
                } catch (IOException ioe) {
                    System.err.println(ioe.toString());
                }

                if (quitting) {
                    break;
                }
            } while (!hitEOF);

            System.err.println();
        } catch (IOException ioe) {
            System.err.println("Error opening stdin: " + ioe.toString());
        }
    }

    private void processFile(final Context cx, final String filename) {
        try (FileReader in = new FileReader(filename, StandardCharsets.UTF_8)) {
            cx.evaluateReader(this, in, filename, 1, null);
        } catch (FileNotFoundException ex) {
            Context.reportError("Couldn't open file \"" + filename + "\".");
        } catch (WrappedException we) {
            System.err.println(we.getWrappedException().toString());
        } catch (EvaluatorException | JavaScriptException ee) {
            System.err.println("js: " + ee.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.toString());
        }
    }

    private static void p(final String s) {
        System.out.println(s);
    }
}
