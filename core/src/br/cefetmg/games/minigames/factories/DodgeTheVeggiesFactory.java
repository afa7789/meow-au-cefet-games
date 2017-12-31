package br.cefetmg.games.minigames.factories;

import br.cefetmg.games.minigames.DodgeTheVeggies;
import br.cefetmg.games.minigames.MiniGame;
import br.cefetmg.games.screens.BaseScreen;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import java.util.HashMap;
import java.util.Map;
import br.cefetmg.games.minigames.util.MiniGameStateObserver;

public class DodgeTheVeggiesFactory implements MiniGameFactory {

    @Override
    public MiniGame createMiniGame(BaseScreen screen,
            MiniGameStateObserver observer, float difficulty) {
        return new DodgeTheVeggies(screen, observer, difficulty);
    }

    @Override
    public Map<String, Class> getAssetsToPreload() {
        return new HashMap<String, Class>() {
            {
                put("dodge-the-veggies/potato.png", Texture.class);
                put("dodge-the-veggies/tomato.png", Texture.class);
                put("dodge-the-veggies/onion.png", Texture.class);
                put("dodge-the-veggies/carrot.png", Texture.class);
                put("dodge-the-veggies/cat-spritesheet.png", Texture.class);
                put("dodge-the-veggies/fainted-cat-texture.png", Texture.class);
                put("dodge-the-veggies/background.png", Texture.class);
                put("dodge-the-veggies/bensound-jazzcomedy.mp3", Music.class);
            }
        };
    }
}
