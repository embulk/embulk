package org.embulk.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TestSelfUpdate {
    @Test
    public void testVersionUrl() {
        assertEquals("0.8.16", SelfUpdate.guessVersionFromUrlForTesting(
                             "https://bintray.com/embulk/maven/embulk/0.8.16"));
        assertEquals("0.8.27", SelfUpdate.guessVersionFromUrlForTesting(
                             "https://dl.bintray.com/embulk/maven/embulk-0.8.27.jar"));
        assertEquals("0.9.15", SelfUpdate.guessVersionFromUrlForTesting(
                             "https://dl.embulk.org/embulk-0.9.15.jar"));
        assertEquals("0.9.17", SelfUpdate.guessVersionFromUrlForTesting(
                             "https://github.com/embulk/embulk/releases/download/v0.9.17/embulk-0.9.17.jar"));
        assertEquals("0.9.17-ALPHA", SelfUpdate.guessVersionFromUrlForTesting(
                             "https://github.com/embulk/embulk/releases/download/v0.9.17/embulk-0.9.17-ALPHA.jar"));
        assertNull(SelfUpdate.guessVersionFromUrlForTesting(
                           "https://github.com/embulk/embulk/releases/download/v0.9.17/embulk-0.A9.17.jar"));
    }
}
