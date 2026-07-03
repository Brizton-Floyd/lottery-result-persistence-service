package com.floyd.lottoptions.agr.persistence;

import com.floyd.persistence.model.LotteryGame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LotteryGameSerializerTest {

    @TempDir
    Path tempDir;

    private LotteryGame game(String name, Integer drawPositionCount) {
        LotteryGame g = new LotteryGame();
        g.setFullName(name);
        g.setStateGameBelongsTo("Texas");
        g.setDrawPositionCount(drawPositionCount);
        return g;
    }

    @Test
    void serialize_writesReadableFile_underStateDir() throws Exception {
        LotteryGameSerializer serializer = new LotteryGameSerializer(tempDir.toString());

        boolean result = serializer.serialize(game("Lotto Texas", 6));

        assertThat(result).isTrue();
        Path expected = tempDir.resolve("TEXAS").resolve("Lotto Texas.ser");
        assertThat(Files.isRegularFile(expected)).isTrue();

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(expected))) {
            LotteryGame read = (LotteryGame) in.readObject();
            assertThat(read.getFullName()).isEqualTo("Lotto Texas");
            assertThat(read.getDrawPositionCount()).isEqualTo(6);
        }
    }

    @Test
    void serialize_overwritesInPlace_andLeavesNoTempFiles() throws Exception {
        LotteryGameSerializer serializer = new LotteryGameSerializer(tempDir.toString());

        serializer.serialize(game("Cash Five", 5));
        serializer.serialize(game("Cash Five", 9)); // overwrite

        Path stateDir = tempDir.resolve("TEXAS");
        Path target = stateDir.resolve("Cash Five.ser");

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(target))) {
            LotteryGame read = (LotteryGame) in.readObject();
            assertThat(read.getDrawPositionCount()).isEqualTo(9);
        }

        // The atomic-rename strategy must not leave orphaned temp files behind.
        try (Stream<Path> files = Files.list(stateDir)) {
            List<String> names = files.map(p -> p.getFileName().toString()).toList();
            assertThat(names).containsExactly("Cash Five.ser");
            assertThat(names).noneMatch(n -> n.contains(".tmp"));
        }
    }
}
