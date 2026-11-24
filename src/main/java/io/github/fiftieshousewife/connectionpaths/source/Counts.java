package io.github.fiftieshousewife.connectionpaths.source;

/** How much was read: files, files that couldn't be parsed, and calls resolved. */
@lombok.Getter
public final class Counts {
    int files;
    int filesNotParsed;
    int callsResolved;
}
