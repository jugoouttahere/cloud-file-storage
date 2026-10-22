package ru.rostislav.cloudfilestorage;

import org.junit.jupiter.api.Test;
import ru.rostislav.cloudfilestorage.util.PathValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathValidatorTest {


    @Test
    void shouldRejectParentDirectory() {
        assertFalse(PathValidator.isValidPath("../file.txt"));
    }

    @Test
    void shouldRejectNestedParentDirectory() {
        assertFalse(PathValidator.isValidPath("documents/../file.txt"));
    }

    @Test
    void shouldRejectCurrentDirectory() {
        assertFalse(PathValidator.isValidPath("./file.txt"));
    }

    @Test
    void shouldRejectNestedCurrentDirectory() {
        assertFalse(PathValidator.isValidPath("documents/./file.txt"));
    }

    @Test
    void shouldRejectBackslash() {
        assertFalse(PathValidator.isValidPath("documents\\file.txt"));
    }

    @Test
    void shouldRejectDoubleSlash() {
        assertFalse(PathValidator.isValidPath("documents//file.txt"));
    }

    @Test
    void shouldRejectPathStartingWithSlash() {
        assertFalse(PathValidator.isValidPath("/documents/file.txt"));
    }

    @Test
    void shouldRejectBlankPath() {
        assertFalse(PathValidator.isValidPath("   "));
    }

    @Test
    void shouldRejectNullPath() {
        assertFalse(PathValidator.isValidPath(null));
    }

    @Test
    void shouldAcceptRootPath() {
        assertTrue(PathValidator.isValidPath(""));
    }

    @Test
    void shouldAcceptFilePath() {
        assertTrue(PathValidator.isValidPath("documents/file.txt"));
    }

    @Test
    void shouldAcceptFolderPath() {
        assertTrue(PathValidator.isValidPath("documents/folder/"));
    }

    @Test
    void shouldAcceptNestedPath() {
        assertTrue(PathValidator.isValidPath("documents/projects/java/file.txt"));
    }

    @Test
    void shouldRecognizeFolderPath() {
        assertTrue(PathValidator.isFolderPath("documents/"));
    }

    @Test
    void shouldRecognizeRootPathAsFolder() {
        assertTrue(PathValidator.isFolderPath(""));
    }

    @Test
    void shouldRecognizeFilePathAsNotFolder() {
        assertFalse(PathValidator.isFolderPath("documents/file.txt"));
    }
}
