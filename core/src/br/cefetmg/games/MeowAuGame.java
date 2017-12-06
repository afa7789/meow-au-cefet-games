package br.cefetmg.games;

import br.cefetmg.games.database.OnlineRanking;
import br.cefetmg.games.database.interfaces.Leaderboard;
import br.cefetmg.games.screens.SplashScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;

/**
 * Classe de inicialização do jogo. Um game, na LibGDX, é um ApplicationListener
 * que possui várias telas e delega para a tela corrente a responsabilidade pelo
 * ciclo de vida da aplicação (create, dispose, render, resize, pause, resume).
 *
 * @author Flávio Coutinho - fegemo <coutinho@decom.cefetmg.br>
 */
public class MeowAuGame extends Game {

	private static Screen loadedScreen = null;

	@Override
	public void create() {
		this.setScreen(new SplashScreen(this, null));
		Gdx.input.setCatchBackKey(true);
	}

	private void handleInput() {
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			Gdx.app.exit();
		}
	}

	@Override
	public void render() {
		handleInput();
		super.render();
	}

	@Override
	public void dispose() {
		if (this.getScreen() != null) {
			this.getScreen().dispose();
		}
	}

	@Override
	public void setScreen(Screen screen) {
		if (loadedScreen == screen) {
			this.screen = screen;
			return;
		}

		super.setScreen(screen);
	}

	public void setLoadedScreen(Screen screen) {
		loadedScreen = screen;
	}

	public MeowAuGame(Leaderboard leaderboard) {
		super();
		OnlineRanking.setLeaderboard(leaderboard);
		OnlineRanking.connect();
	}

}
