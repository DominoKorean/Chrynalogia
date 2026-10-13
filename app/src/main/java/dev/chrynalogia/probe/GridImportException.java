package dev.chrynalogia.probe;
/** Stable user-facing resource, with the technical cause retained for diagnostics. */
final class GridImportException extends java.io.IOException {
    final int messageRes;
    GridImportException(int messageRes){this(messageRes,null);}
    GridImportException(int messageRes,Throwable cause){super("Grid import error "+messageRes,cause);this.messageRes=messageRes;}
}
