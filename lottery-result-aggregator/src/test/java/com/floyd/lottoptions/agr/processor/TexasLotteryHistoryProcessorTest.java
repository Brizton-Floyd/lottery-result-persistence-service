package com.floyd.lottoptions.agr.processor;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;
import com.floyd.lottoptions.agr.documentreaders.CsvFileReader;
import com.floyd.lottoptions.agr.persistence.LotteryGameSerializer;
import com.floyd.lottoptions.agr.processor.TexasLotteryHistoryProcessor.RowLayout;
import com.floyd.persistence.model.LotteryDraw;
import com.floyd.persistence.model.LotteryGame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs real rows from the official Texas CSV exports through the real {@link CsvFileReader},
 * {@link TexasLotteryHistoryProcessor} and {@link LotteryGameSerializer}, then reads the stored game back.
 */
class TexasLotteryHistoryProcessorTest {

    @TempDir
    Path tempDir;

    private LotteryGameSerializer serializer() {
        return new LotteryGameSerializer(tempDir.resolve("store").toString());
    }

    private GameProcessingResult process(String game, String... csvRows) throws Exception {
        Path csv = tempDir.resolve(game.replace(' ', '_') + ".csv");
        Files.write(csv, List.of(csvRows), StandardCharsets.UTF_8);
        LotteryUrlConfig.GameInfo info = new LotteryUrlConfig.GameInfo();
        info.setName(game);
        info.setUrl(csv.toUri().toString());
        return new TexasLotteryHistoryProcessor(new CsvFileReader(), serializer()).processGame("Texas", info);
    }

    private LotteryGame stored(String game) throws Exception {
        Path ser = tempDir.resolve("store").resolve("TEXAS").resolve(game + ".ser");
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(ser))) {
            return (LotteryGame) in.readObject();
        }
    }

    private static LotteryDraw drawOn(LotteryGame game, LocalDate date) {
        return game.getLotteryDraws().stream()
                .filter(d -> d.getDrawDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stored draw on " + date));
    }

    @Test
    void megaMillions_storesMegaBallAsBonus_andKeepsDrawsWithAnEmptyMegaplier() throws Exception {
        GameProcessingResult result = process("Mega Millions",
                "Mega Millions,4,4,2025,69,35,37,28,11,25,2",
                // Since the 2025-04-08 redesign the Megaplier is assigned at purchase, so the column is empty.
                "Mega Millions,4,8,2025,61,10,16,60,50,17,",
                "Mega Millions,10,20,2026,35,13,48,33,28,3,");

        assertThat(result).isEqualTo(new GameProcessingResult(3, 0));
        LotteryGame game = stored("Mega Millions");
        assertThat(game.getDrawHistoryCount()).isEqualTo(3);
        assertThat(game.getDrawPositionCount()).isEqualTo(5);
        assertThat(game.getDateLastUpdated()).isEqualTo(LocalDate.of(2026, 10, 20));

        LotteryDraw withMegaplier = drawOn(game, LocalDate.of(2025, 4, 4));
        assertThat(withMegaplier.getDrawResults()).containsExactly(11, 28, 35, 37, 69);
        assertThat(withMegaplier.getBonusNumber()).isEqualTo(25);
        assertThat(withMegaplier.getMultiplier()).isEqualTo(2);

        LotteryDraw withoutMegaplier = drawOn(game, LocalDate.of(2025, 4, 8));
        assertThat(withoutMegaplier.getDrawResults()).containsExactly(10, 16, 50, 60, 61);
        assertThat(withoutMegaplier.getBonusNumber()).isEqualTo(17);
        assertThat(withoutMegaplier.getMultiplier()).isNull();

        assertThat(drawOn(game, LocalDate.of(2026, 10, 20)).getBonusNumber()).isEqualTo(3);
    }

    @Test
    void powerball_storesPowerballAsBonus_andKeepsDrawsWithAnEmptyPowerPlay() throws Exception {
        GameProcessingResult result = process("Powerball",
                // 2012-01-18 to 2014-01-18: the Power Play column is empty.
                "Powerball,1,18,2012,50,34,29,6,44,28,",
                "Powerball,10,19,2026,31,42,8,32,48,23,4");

        assertThat(result).isEqualTo(new GameProcessingResult(2, 0));
        LotteryGame game = stored("Powerball");
        assertThat(game.getDrawHistoryCount()).isEqualTo(2);

        LotteryDraw withoutPowerPlay = drawOn(game, LocalDate.of(2012, 1, 18));
        assertThat(withoutPowerPlay.getDrawResults()).containsExactly(6, 29, 34, 44, 50);
        assertThat(withoutPowerPlay.getBonusNumber()).isEqualTo(28);
        assertThat(withoutPowerPlay.getMultiplier()).isNull();

        LotteryDraw latest = drawOn(game, LocalDate.of(2026, 10, 19));
        assertThat(latest.getDrawResults()).containsExactly(8, 31, 32, 42, 48);
        assertThat(latest.getBonusNumber()).isEqualTo(23);
        assertThat(latest.getMultiplier()).isEqualTo(4);
    }

    @Test
    void repoll_replacesAPreviouslyStoredWrongBonus_andRestoresDroppedDraws() throws Exception {
        // What the old parser persisted: the Megaplier as bonusNumber, and nothing after 2025-04-04.
        LotteryGame corrupted = new LotteryGame();
        corrupted.setFullName("Mega Millions");
        corrupted.setStateGameBelongsTo("Texas");
        LotteryDraw wrong = new LotteryDraw();
        wrong.setDrawDate(LocalDate.of(2025, 4, 4));
        wrong.setBonusNumber(2);
        wrong.getDrawResults().addAll(List.of(11, 28, 35, 37, 69));
        corrupted.setLotteryDraws(new ArrayList<>(List.of(wrong)));
        corrupted.setDrawHistoryCount(1);
        corrupted.setDateLastUpdated(wrong.getDrawDate());
        serializer().serialize(corrupted);

        process("Mega Millions",
                "Mega Millions,4,4,2025,69,35,37,28,11,25,2",
                "Mega Millions,4,8,2025,61,10,16,60,50,17,");

        LotteryGame game = stored("Mega Millions");
        assertThat(game.getDrawHistoryCount()).isEqualTo(2);
        assertThat(game.getLotteryDraws()).hasSize(2);
        assertThat(game.getDateLastUpdated()).isEqualTo(LocalDate.of(2025, 4, 8));
        assertThat(drawOn(game, LocalDate.of(2025, 4, 4)).getBonusNumber()).isEqualTo(25);
        assertThat(drawOn(game, LocalDate.of(2025, 4, 8)).getBonusNumber()).isEqualTo(17);
    }

    @Test
    void unparseableRow_isRejectedAndCounted_whileValidRowsAreStored() throws Exception {
        GameProcessingResult result = process("Powerball",
                "Powerball,10,17,2026,13,35,32,24,12,11,3",
                "Powerball,10,19,2026,31,42,8,32,48,,4");

        assertThat(result).isEqualTo(new GameProcessingResult(2, 1));
        assertThat(result.hasRejectedRows()).isTrue();
        LotteryGame game = stored("Powerball");
        assertThat(game.getDrawHistoryCount()).isEqualTo(1);
        assertThat(game.getDateLastUpdated()).isEqualTo(LocalDate.of(2026, 10, 17));
    }

    @Test
    void noParseableRows_failsTheGame_andLeavesTheExistingStoreUntouched() throws Exception {
        process("Powerball", "Powerball,10,19,2026,31,42,8,32,48,23,4");

        assertThatThrownBy(() -> process("Powerball", "Powerball,10,21,2026,1,2,3,4,5"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No parseable draws");

        assertThat(stored("Powerball").getDateLastUpdated()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void unsupportedGame_failsInsteadOfBeingSilentlySkipped() {
        assertThatThrownBy(() -> process("Lucky Seven", "Lucky Seven,1,1,2026,7"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lucky Seven");
    }

    @Test
    void multiStateRowWithUnexpectedColumnCount_isRejected() {
        String[] extraColumn = "Powerball,10,19,2026,31,42,8,32,48,23,4,9".split(",", -1);

        assertThatThrownBy(() -> TexasLotteryHistoryProcessor.parseDraw(extraColumn, RowLayout.MULTI_STATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("found 12");
    }

    @Test
    void otherGameLayouts_parseAsBefore() {
        LotteryDraw twoStep = TexasLotteryHistoryProcessor.parseDraw(
                "Texas Two Step,10,19,2026,23,7,12,22,23".split(",", -1), RowLayout.TRAILING_BONUS);
        assertThat(twoStep.getDrawResults()).containsExactly(7, 12, 22, 23);
        assertThat(twoStep.getBonusNumber()).isEqualTo(23);
        assertThat(twoStep.getMultiplier()).isNull();

        LotteryDraw pick3 = TexasLotteryHistoryProcessor.parseDraw(
                "Pick 3 Day,10,21,2026,2,8,3,,9".split(",", -1), RowLayout.FIREBALL);
        assertThat(pick3.getDrawResults()).containsExactly(2, 8, 3);
        assertThat(pick3.getBonusNumber()).isNull();

        LotteryDraw lottoTexas = TexasLotteryHistoryProcessor.parseDraw(
                "Lotto Texas,10,19,2026,37,1,16,49,7,32".split(",", -1), RowLayout.STANDARD);
        assertThat(lottoTexas.getDrawResults()).containsExactly(1, 7, 16, 32, 37, 49);
        assertThat(lottoTexas.getBonusNumber()).isNull();
    }
}
