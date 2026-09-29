package ru.rostislav.cloudfilestorage;

import org.junit.jupiter.api.Test;
import ru.rostislav.cloudfilestorage.util.PathValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathValidatorTest {

    PathValidator pathValidator = new PathValidator();

    @Test
    void shouldRejectParentDirectory() {
        assertFalse(pathValidator.isValidPath("../file.txt"));
    }

    @Test
    void shouldRejectNestedParentDirectory() {
        assertFalse(pathValidator.isValidPath("documents/../file.txt"));
    }

    @Test
    void shouldRejectCurrentDirectory() {
        assertFalse(pathValidator.isValidPath("./file.txt"));
    }

    @Test
    void shouldRejectNestedCurrentDirectory() {
        assertFalse(pathValidator.isValidPath("documents/./file.txt"));
    }

    @Test
    void shouldRejectBackslash() {
        assertFalse(pathValidator.isValidPath("documents\\file.txt"));
    }

    @Test
    void shouldRejectDoubleSlash() {
        assertFalse(pathValidator.isValidPath("documents//file.txt"));
    }

    @Test
    void shouldRejectPathStartingWithSlash() {
        assertFalse(pathValidator.isValidPath("/documents/file.txt"));
    }

    @Test
    void shouldRejectBlankPath() {
        assertFalse(pathValidator.isValidPath("   "));
    }

    @Test
    void shouldRejectNullPath() {
        assertFalse(pathValidator.isValidPath(null));
    }

    @Test
    void shouldAcceptRootPath() {
        assertTrue(pathValidator.isValidPath(""));
    }

    @Test
    void shouldAcceptFilePath() {
        assertTrue(pathValidator.isValidPath("documents/file.txt"));
    }

    @Test
    void shouldAcceptFolderPath() {
        assertTrue(pathValidator.isValidPath("documents/folder/"));
    }

    @Test
    void shouldAcceptNestedPath() {
        assertTrue(pathValidator.isValidPath("documents/projects/java/file.txt"));
    }

    @Test
    void shouldRecognizeFolderPath() {
        assertTrue(pathValidator.isFolderPath("documents/"));
    }

    @Test
    void shouldRecognizeRootPathAsFolder() {
        assertTrue(pathValidator.isFolderPath(""));
    }

    @Test
    void shouldRecognizeFilePathAsNotFolder() {
        assertFalse(pathValidator.isFolderPath("documents/file.txt"));
    }
}
