package br.cefetmg.games.minigames.factories;

import br.cefetmg.games.minigames.MiniGame;
import br.cefetmg.games.minigames.SpyFish;
import br.cefetmg.games.minigames.util.MiniGameStateObserver;
import br.cefetmg.games.screens.BaseScreen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Luiza Pedro
 */
public class SpyFishFactory implements MiniGameFactory {

    @Override
    public MiniGame createMiniGame(BaseScreen screen,
            MiniGameStateObserver observer, float difficulty) {
        return new SpyFish(screen, observer, difficulty);
    }

    @Override
    public Map<String, Class> getAssetsToPreload() {
        return new HashMap<String, Class>() {
            {
                put("spy-fish/fish.png", Texture.class);
                put("spy-fish/card.png", Texture.class);
                put("spy-fish/ocean.png", Texture.class);
                put("spy-fish/smw_fireball.mp3", Sound.class);
                put("spy-fish/smw_kick.mp3", Sound.class);
            }
        };
    }

}
