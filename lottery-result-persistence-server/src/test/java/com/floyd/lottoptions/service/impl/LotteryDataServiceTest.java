package com.floyd.lottoptions.service.impl;

import com.floyd.persistence.model.LotteryDraw;
import com.floyd.persistence.model.LotteryGame;
import com.floyd.persistence.model.LotteryState;
import com.floyd.persistence.model.request.StateGameAnalysisRequest;
import com.floyd.persistence.model.response.AllStateLottoGameResponse;
import com.floyd.persistence.model.response.StateGamesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Exercises the <em>real</em> {@link LotteryDataService} against a temp storage root supplied
 * through the configurable {@code base-dir}. Unlike the previous version, this no longer
 * re-implements the catalog logic in a test double — it writes genuine serialized
 * {@code .ser} files and asserts on production output.
 *
 * <p>FR-020: {@code drawPositionCount} must be copied from the deserialized {@code .ser} game
 * onto the lightweight catalog entry, on both the v1 and v2 paths.
 */
class LotteryDataServiceTest {

    @TempDir
    Path tempDir;

    private LotteryDataService service;

    @BeforeEach
    void setUp() {
        service = new LotteryDataService(tempDir.toString());
    }

    /** Writes a real serialized LotteryGame to {@code tempDir/STATE/game.ser}. */
    private void writeGame(String stateDirUpper, String gameName, Integer drawPositionCount) throws Exception {
        Path stateDir = tempDir.resolve(stateDirUpper);
        Files.createDirectories(stateDir);

        LotteryGame game = new LotteryGame();
        game.setFullName(gameName);
        game.setStateGameBelongsTo(stateDirUpper);
        game.setMinNumber(1);
        game.setMaxNumber(54);
        game.setDrawPositionCount(drawPositionCount);

        LotteryDraw draw = new LotteryDraw();
        draw.setDrawDate(LocalDate.of(2024, 1, 1));
        draw.setDrawResults(new ArrayList<>(List.of(1, 2, 3, 4, 5, 6)));
        game.setLotteryDraws(new ArrayList<>(List.of(draw)));

        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(stateDir.resolve(gameName + ".ser")))) {
            oos.writeObject(game);
        }
    }

    @Test
    @DisplayName("v2 catalog carries drawPositionCount and drops draws")
    void v2_catalog_carriesDrawPositionCount() throws Exception {
        writeGame("TEXAS", "Lotto Texas", 6);

        Optional<AllStateLottoGameResponse> result = service.getAllStateLotteryGamesV2();

        assertThat(result).isPresent();
        List<LotteryState> states = result.get().getLotteryStateGames();
        LotteryState texas = states.stream()
                .filter(s -> s.getStateRegion().equalsIgnoreCase("Texas"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Texas not found in catalog"));

        assertThat(texas.getStateLotteryGames()).hasSize(1);
        LotteryGame catalogGame = texas.getStateLotteryGames().get(0);
        assertThat(catalogGame.getDrawPositionCount()).isEqualTo(6);
        assertThat(catalogGame.getLotteryDraws())
                .as("catalog shell must not carry the full draw history")
                .isNull();
    }

    @Test
    @DisplayName("v1 catalog carries drawPositionCount (shared path)")
    void v1_catalog_carriesDrawPositionCount() throws Exception {
        writeGame("TEXAS", "Cash Five", 5);

        Optional<AllStateLottoGameResponse> result = service.getAllStateLotteryGames();

        assertThat(result).isPresent();
        Map<String, List<LotteryGame>> map = result.get().getAllStateLotteryGames();
        assertThat(map).isNotEmpty();
        List<LotteryGame> texasGames = map.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase("Texas"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Texas games not found"));

        assertThat(texasGames).hasSize(1);
        assertThat(texasGames.get(0).getDrawPositionCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("null drawPositionCount stays null and does not throw")
    void nullDrawPositionCount_staysNull() throws Exception {
        writeGame("LOUISIANA", "Pick 3", null);

        assertThatNoException().isThrownBy(() -> {
            Optional<AllStateLottoGameResponse> result = service.getAllStateLotteryGamesV2();
            assertThat(result).isPresent();
            result.get().getLotteryStateGames().forEach(state ->
                    state.getStateLotteryGames().forEach(g ->
                            assertThat(g.getDrawPositionCount()).isNull()));
        });
    }

    @Test
    @DisplayName("getStateData returns the full game with draws")
    void getStateData_returnsFullGame() throws Exception {
        writeGame("TEXAS", "Lotto Texas", 6);

        StateGameAnalysisRequest request = new StateGameAnalysisRequest();
        request.setStateName("Texas");
        request.setGameName("Lotto Texas");

        Optional<StateGamesResponse> result = service.getStateData(request);

        assertThat(result).isPresent();
        LotteryGame game = result.get().getLotteryGame();
        assertThat(game).isNotNull();
        assertThat(game.getDrawPositionCount()).isEqualTo(6);
        assertThat(game.getLotteryDraws()).hasSize(1);
    }

    @Test
    @DisplayName("getStateData for a missing game returns an empty response, not an error")
    void getStateData_missingGame_returnsEmpty() throws Exception {
        StateGameAnalysisRequest request = new StateGameAnalysisRequest();
        request.setStateName("Texas");
        request.setGameName("Nonexistent");

        Optional<StateGamesResponse> result = service.getStateData(request);

        assertThat(result).isPresent();
        assertThat(result.get().getLotteryGame()).isNull();
    }
}
