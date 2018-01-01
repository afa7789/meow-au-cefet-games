package br.cefetmg.games.minigames;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.ScaledNumericValue;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.codeandweb.physicseditor.PhysicsShapeCache;

import br.cefetmg.games.minigames.util.DifficultyCurve;
import br.cefetmg.games.minigames.util.MiniGameState;
import br.cefetmg.games.minigames.util.MiniGameStateObserver;
import br.cefetmg.games.minigames.util.TimeoutBehavior;
import br.cefetmg.games.screens.BaseScreen;
import br.cefetmg.games.sound.MyMusic;
import br.cefetmg.games.sound.MySound;
import br.cefetmg.games.sound.SoundManager;

/**
 * Classe do jogo AstroCat
 * 
 * @author andrebrait
 *
 */
public class AstroCat extends MiniGame {

    private static final float PIXELS_PER_METER = 32.0f;
    private static final float RATIO_METERS_PER_PIXEL = 1.0f / PIXELS_PER_METER;

    private static final int NUM_ASTEROIDS = 6;
    private static final float GAME_DURATION = 15.0f;

    private static final float STEPS_PER_SECOND = 60.0f;
    private static final float STEP_TIME = 1.0f / STEPS_PER_SECOND;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private static final float DELTA_ASTEROID_START = 50.0f;

    private static final float ROCKET_FORCE = 5.0f * PIXELS_PER_METER;

    private static final float SCALE = 0.4f;
    private static final String MUSIC_PATH = "astrocat/background.mp3";

    private float accumulator = 0.0f;

    private Random rand;

    private Sprite background;
    private MySound gasNoise, impact;

    private World world;

    private double maxAsteroids;
    private double asteroidSpeed;

    private AstroCatCharacter astroCat;
    private Planet planet;
    private Asteroid[] asteroids;
    private Sprite crosshair;

    private boolean crosshairVisible;

    private Body[] walls;
    private Set<Asteroid> asteroidSet;

    private boolean finished;
    private MyMusic backgroundMusic;

    public AstroCat(BaseScreen screen, MiniGameStateObserver observer, float difficulty) {
        super(screen, observer, difficulty, GAME_DURATION, TimeoutBehavior.FAILS_WHEN_MINIGAME_ENDS);
    }

    private class AstroCatContactListener implements ContactListener {

        private static final short CATEGORY_ASTEROID = 1, CATEGORY_PLANET = 4;

        private boolean evaluateContact(Contact contact, short category) {
            return ((contact.getFixtureA().getFilterData().categoryBits & category)
                    | (contact.getFixtureB().getFilterData().categoryBits & category)) != 0;
        }

        @Override
        public void beginContact(Contact contact) {
            if (evaluateContact(contact, CATEGORY_ASTEROID)) {
                impact.play();
            } else if (evaluateContact(contact, CATEGORY_PLANET)) {
                finished = true;
            }
        }

        @Override
        public void endContact(Contact contact) {

        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {

        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {

        }

    }

    private static abstract class AstroCatBody {

        private static final float BODY_SCALE_PER_PIXEL = SCALE * RATIO_METERS_PER_PIXEL;
        protected final Sprite sprite;
        final Body body;

        AstroCatBody(String bodyName, PhysicsShapeCache physCache, Texture texture, World world,
                     Vector2 position, boolean processPosition) {
            body = physCache.createBody(bodyName, world, BODY_SCALE_PER_PIXEL, BODY_SCALE_PER_PIXEL);
            body.setTransform(position.cpy().scl(RATIO_METERS_PER_PIXEL), 0.0f);
            sprite = new Sprite(texture);
            sprite.setOriginCenter();
            sprite.setScale(SCALE);
            // sprite.setSize(sprite.getWidth() * SCALE, sprite.getHeight() * SCALE);
            if (processPosition) {
                updatePosition();
            }
        }

        public void updatePosition() {
            Vector2 bodyCenter = body.getPosition();
            sprite.setCenter(bodyCenter.x * PIXELS_PER_METER, bodyCenter.y * PIXELS_PER_METER);
            sprite.setRotation((float) Math.toDegrees(body.getAngle()));
        }

        public void draw(Batch batch) {
            sprite.draw(batch);
        }

        void deactivate() {
            body.setActive(false);
            sprite.setAlpha(0.0f);
        }

        void activate() {
            body.setActive(true);
            sprite.setAlpha(1.0f);
        }

    }

    private static class Asteroid extends AstroCatBody {

        private final int index;
        private final Random rand;

        Asteroid(int i, PhysicsShapeCache physCache, World world, Texture[] asteroidTextures, int asteroidNum) {
            super("asteroid" + asteroidNum, physCache, asteroidTextures[asteroidNum - 1], world, Vector2.Zero, true);
            index = i;
            rand = new Random();
            deactivate();
        }

        void setNewPositionAndSpeed(Vector2 position, Vector2 speed, float omega) {
            body.setTransform(position.cpy().scl(RATIO_METERS_PER_PIXEL), speed.angleRad());
            body.setLinearVelocity(speed);
            body.setAngularVelocity(omega);
            updatePosition();
        }

        void setNewRandomPositionAndSpeed(Viewport viewport, AstroCatCharacter player, float speed) {
            Vector2 newPosition;
            float angleMultiplier;
            if (rand.nextBoolean()) {
                // O asteróide sairá de uma das laterais da tela
                angleMultiplier = 0.1f;
                float x = (rand.nextBoolean() && rand.nextBoolean()) ? -DELTA_ASTEROID_START
                        : viewport.getWorldWidth() + DELTA_ASTEROID_START;
                float y = rand.nextFloat() * (1.0f + rand.nextFloat()) * viewport.getWorldHeight();
                newPosition = new Vector2(x, y);
            } else {
                // O asteróide sairá de cima ou baixo da tela
                angleMultiplier = 0.2f;
                float x = rand.nextFloat() * (1.0f + rand.nextFloat()) * viewport.getWorldWidth();
                float y = rand.nextBoolean() ? -DELTA_ASTEROID_START : viewport.getWorldHeight() + DELTA_ASTEROID_START;
                newPosition = new Vector2(x, y);
            }
            Vector2 relativePlayerPosition = player.body.getPosition().cpy().scl(PIXELS_PER_METER).sub(newPosition);
            float newSpeedNorm = getRandomWithinRange(rand, speed, 0.2f);
            float newAngleRad = getRandomWithinRange(rand, relativePlayerPosition.angleRad(), angleMultiplier);
            float newOmega = getRandomWithinRange(rand, 2.0f, 1.5f);
            setNewPositionAndSpeed(newPosition, new Vector2(newSpeedNorm, 0.0f).rotateRad(newAngleRad), newOmega);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Asteroid && ((Asteroid) o).index == index;
        }

        @Override
        public int hashCode() {
            return 37 * index;
        }

    }

    private static class AstroCatCharacter extends AstroCatBody {

        private final static float PI = (float) Math.PI;
        private final static float CIRCLE_RAD = PI * 2.0f;
        private final static float NOVENTA_RAD = PI * 0.5f;

        private final static float ROCKET_DIFF_X_SCALE = SCALE * 0.095f;
        private final static float ROCKET_DIFF_Y_SCALE = SCALE * -0.53f;

        private final ParticleEffect rocket;
        private final Vector2 rocketDiff;
        private final float diffToNoventa;
        private final MySound gasNoise;

        private final Vector2 currentForce;

        private boolean isPlayingSound;

        AstroCatCharacter(PhysicsShapeCache physCache, World world, Texture astroCatTexture, Vector2 position,
                ParticleEffect rocketEmitter, MySound gasNoiseSound) {
            super("astrocat", physCache, astroCatTexture, world, position, false);
            rocket = rocketEmitter;
            rocketDiff = new Vector2(sprite.getWidth() * ROCKET_DIFF_X_SCALE, sprite.getHeight() * ROCKET_DIFF_Y_SCALE);
            diffToNoventa = rocketDiff.angleRad() + NOVENTA_RAD;
            currentForce = new Vector2(ROCKET_FORCE, 0);
            gasNoise = gasNoiseSound;
            isPlayingSound = false;
            updatePosition();
        }

        @Override
        public void updatePosition() {
            super.updatePosition();
            float targetAngleRad = body.getAngle();
            float targetAnglePropulsionRad = targetAngleRad - NOVENTA_RAD;
            currentForce.setAngleRad(targetAnglePropulsionRad).scl(-1.0f);
            Vector2 emitterOffset = rocketDiff.cpy().rotateRad(targetAngleRad + diffToNoventa)
                    .add(body.getPosition().cpy().scl(PIXELS_PER_METER));
            rocket.setPosition(emitterOffset.x, emitterOffset.y);
            for (ParticleEmitter emitter : rocket.getEmitters()) {
                setNewCenter(emitter.getAngle(), (float) Math.toDegrees(targetAnglePropulsionRad));
            }
        }

        private void setNewCenter(ScaledNumericValue value, float center) {
            float spanHigh = (value.getHighMax() - value.getHighMin()) * 0.5f;
            float spanLow = (value.getLowMax() - value.getLowMin()) * 0.5f;
            value.setHigh(center - spanHigh, center + spanHigh);
            value.setLow(center - spanLow, center + spanLow);
        }

        @Override
        public void draw(Batch batch) {
            rocket.draw(batch);
            super.draw(batch);
        }

        void accelerate() {
            rocket.start();
            if (!isPlayingSound) {
                gasNoise.loop();
                isPlayingSound = true;
            }
            body.applyForceToCenter(currentForce, true);
        }

        void stopAccelerating() {
            rocket.reset();
            if (isPlayingSound) {
                gasNoise.stop();
                isPlayingSound = false;
            }
            body.applyForceToCenter(Vector2.Zero, false);
        }

        void turnToPoint(Vector2 point) {
            // Lei dos eixos paralelos
            MassData md = body.getMassData();
            float inertiaAtCenterOfMass = body.getInertia() + md.mass * md.center.len2();
            // Momento de inércia na origem + massa vezes dist. dos eixos ao quadrado

            float bodyAngle = body.getAngle() + NOVENTA_RAD;
            float desiredAngle = point.cpy().scl(RATIO_METERS_PER_PIXEL).sub(body.getPosition()).angleRad();
            float nextAngle = bodyAngle + body.getAngularVelocity() * STEP_TIME;
            float totalRotation = desiredAngle - nextAngle;
            while (totalRotation < -PI) {
                totalRotation += CIRCLE_RAD;
            }
            while (totalRotation > PI) {
                totalRotation -= CIRCLE_RAD;
            }
            float desiredAngularVelocity = totalRotation * STEPS_PER_SECOND;
            float torque = inertiaAtCenterOfMass * desiredAngularVelocity * STEPS_PER_SECOND;
            body.applyTorque(torque, true);
        }

    }

    private static class Planet extends AstroCatBody {
        Planet(PhysicsShapeCache physCache, World world, Texture planetTexture, Vector2 position) {
            super("planet", physCache, planetTexture, world, position, true);
        }
    }

    private static float getRandomWithinRange(Random rand, float value, float range) {
        return value + (rand.nextBoolean() ? -1.0f : 1.0f) * rand.nextFloat() * range * value;
    }

    @Override
    protected void onGamePaused(boolean justPaused) {
        if (justPaused) {
            backgroundMusic.pause();
            astroCat.stopAccelerating();
        } else {
            backgroundMusic.play();
        }
    }

    @Override
    protected void onStart() {
        finished = false;
        rand = new Random();

        walls = new Body[4];
        asteroidSet = new HashSet<Asteroid>();

        // Carregando texturas
        Texture[] asteroidTextures = new Texture[NUM_ASTEROIDS];
        for (int i = 0; i < NUM_ASTEROIDS; i++) {
            asteroidTextures[i] = loadTexture("astrocat/asteroid" + (i + 1) + ".png");
        }
        Texture astroCatTexture = loadTexture("astrocat/astrocat.png");
        Texture crosshairTexture = loadTexture("astrocat/crosshair.png");
        Texture backgroundTexture = loadTexture("astrocat/background.png");
        Texture planetTexture = loadTexture("astrocat/planet.png");
        gasNoise = new MySound(assets.get("astrocat/gasnoise.mp3", Sound.class));
        impact = new MySound(assets.get("astrocat/impact.mp3", Sound.class));
        world = new World(new Vector2(0.0f, 0.0f), true);
        PhysicsShapeCache physCache = new PhysicsShapeCache(Gdx.files.internal("astrocat/physics.xml"));

        // Carregando efeito de partículas
        ParticleEffect particleEffect = new ParticleEffect();
        particleEffect.load(Gdx.files.internal("astrocat/rocket.p"), Gdx.files.internal("astrocat"));
        particleEffect.scaleEffect(SCALE);

        // Instanciando Sprites
        float verticalMiddle = viewport.getWorldHeight() * 0.5f;
        astroCat = new AstroCatCharacter(physCache, world, astroCatTexture,
                new Vector2(viewport.getWorldWidth() * 0.1f, getRandomWithinRange(rand, verticalMiddle, 0.7f)),
                particleEffect, gasNoise);
        planet = new Planet(physCache, world, planetTexture,
                new Vector2(viewport.getWorldWidth() * 0.95f, getRandomWithinRange(rand, verticalMiddle, 0.8f)));

        // Instanciando asteróides
        int asteroidInstances = (int) (Math.ceil(maxAsteroids * 1.2f));

        asteroids = new Asteroid[asteroidInstances];
        for (int i = 0; i < asteroidInstances; i++) {
            Asteroid newAsteroid = new Asteroid(i, physCache, world, asteroidTextures, rand.nextInt(NUM_ASTEROIDS) + 1);
            newAsteroid.setNewPositionAndSpeed(Vector2.Zero, Vector2.Zero, 0.0f);
            asteroids[i] = newAsteroid;
        }

        // Inicializando o plano de fundo
        background = new Sprite(backgroundTexture);
        background.setOrigin(0.0f, 0.0f);
        background.setScale(viewport.getWorldWidth() / background.getWidth(),
                viewport.getWorldHeight() / background.getHeight());
        background.setPosition(0.0f, 0.0f);

        // Definindo sprite da mira
        crosshair = new Sprite(crosshairTexture);
        crosshair.setOriginCenter();
        crosshair.setScale(SCALE);
        crosshair.setAlpha(0.0f);
        crosshairVisible = false;

        // Inserindo listener de detecção de colisão com som
        ContactListener contactListener = new AstroCatContactListener();
        world.setContactListener(contactListener);

        createWalls();

        backgroundMusic = new MyMusic(assets.get(MUSIC_PATH, Music.class));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.6f);
        backgroundMusic.play();
    }

    private Texture loadTexture(String path) {
        Texture tex = assets.get(path, Texture.class);
        tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        return tex;
    }

    private void createWalls() {
        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDefHorizontal = new FixtureDef();
        FixtureDef fixtureDefVertical = new FixtureDef();
        PolygonShape shapeHorizontal = new PolygonShape();
        PolygonShape shapeVertical = new PolygonShape();

        float worldScaledWidth = viewport.getWorldWidth() * RATIO_METERS_PER_PIXEL;
        float worldScaledHeight = viewport.getWorldHeight() * RATIO_METERS_PER_PIXEL;

        bodyDef.type = BodyDef.BodyType.StaticBody;
        fixtureDefHorizontal.filter.maskBits = 2;
        fixtureDefHorizontal.filter.categoryBits = 8;
        fixtureDefVertical.filter.maskBits = 2;
        fixtureDefVertical.filter.categoryBits = 8;
        shapeHorizontal.setAsBox(worldScaledWidth, RATIO_METERS_PER_PIXEL);
        shapeVertical.setAsBox(RATIO_METERS_PER_PIXEL, worldScaledHeight);
        fixtureDefHorizontal.shape = shapeHorizontal;
        fixtureDefVertical.shape = shapeVertical;

        walls[0] = world.createBody(bodyDef);
        walls[0].createFixture(fixtureDefHorizontal);
        walls[0].setTransform(0.0f, 0.0f, 0.0f);

        walls[1] = world.createBody(bodyDef);
        walls[1].createFixture(fixtureDefVertical);
        walls[1].setTransform(0.0f, 0.0f, 0.0f);

        walls[2] = world.createBody(bodyDef);
        walls[2].createFixture(fixtureDefHorizontal);
        walls[2].setTransform(0.0f, worldScaledHeight, 0.0f);

        walls[3] = world.createBody(bodyDef);
        walls[3].createFixture(fixtureDefVertical);
        walls[3].setTransform(worldScaledWidth, 0.0f, 0.0f);

        shapeHorizontal.dispose();
        shapeVertical.dispose();
    }

    private void fillAsteroidSet() {
        while (asteroidSet.size() <= maxAsteroids) {
            Asteroid selected;
            do {
                selected = asteroids[rand.nextInt(asteroids.length)];
            } while (!asteroidSet.add(selected));
            selected.setNewRandomPositionAndSpeed(viewport, astroCat, (float) asteroidSpeed);
            selected.activate();
        }
    }

    private void removeOutOfBoundAsteroids() {
        for (Iterator<Asteroid> i = asteroidSet.iterator(); i.hasNext();) {
            Asteroid asteroid = i.next();
            Vector2 pos = asteroid.body.getPosition().cpy().scl(PIXELS_PER_METER);
            if (pos.x <= -DELTA_ASTEROID_START || pos.y <= -DELTA_ASTEROID_START
                    || pos.x >= viewport.getWorldWidth() + DELTA_ASTEROID_START
                    || pos.y >= viewport.getWorldHeight() + DELTA_ASTEROID_START) {
                asteroid.deactivate();
                asteroid.setNewPositionAndSpeed(Vector2.Zero, Vector2.Zero, 0.0f);
                i.remove();
            }
        }
    }

    private void stepWorld(float dt) {
        accumulator += Math.min(dt, 0.25f);
        if (accumulator >= STEP_TIME) {
            accumulator -= STEP_TIME;
            world.step(STEP_TIME, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            updateBodies();
            astroCat.rocket.update(dt);
        }
    }

    private void updateBodies() {
        astroCat.updatePosition();
        removeOutOfBoundAsteroids();
        fillAsteroidSet();
        for (Asteroid asteroid : asteroidSet) {
            asteroid.updatePosition();
        }
        planet.updatePosition();
    }

    @Override
    protected void configureDifficultyParameters(float difficulty) {
        maxAsteroids = Math.ceil(DifficultyCurve.LINEAR.getCurveValueBetween(difficulty, 7.0f, 14.0f));
        asteroidSpeed = DifficultyCurve.LINEAR.getCurveValueBetween(difficulty, 0.16f, 0.32f) * PIXELS_PER_METER;
    }

    @Override
    public void onHandlePlayingInput() {

        if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen)) {
            if (crosshairVisible) {
                crosshair.setAlpha(0.0f);
                crosshairVisible = false;
            }
            if (Gdx.input.isTouched(0)) {
                Vector2 cursor = new Vector2(Gdx.input.getX(0), Gdx.input.getY(0));
                viewport.unproject(cursor);
                astroCat.turnToPoint(cursor);
                astroCat.accelerate();
            } else {
                astroCat.stopAccelerating();
            }
        } else {
            Vector2 cursor = new Vector2(Gdx.input.getX(0), Gdx.input.getY(0));
            viewport.unproject(cursor);
            if (!crosshairVisible) {
                crosshair.setAlpha(0.9f);
                crosshairVisible = true;
            }
            crosshair.setCenter(cursor.x, cursor.y);
            astroCat.turnToPoint(cursor);
            if (Gdx.input.isTouched(0)) {
                astroCat.accelerate();
            } else {
                astroCat.stopAccelerating();
            }
        }

    }

    @Override
    public void onUpdate(float dt) {
        if (finished) {
            super.challengeSolved();
        }
        stepWorld(dt);
    }

    @Override
    public void onDrawGame() {
        background.draw(batch);
        astroCat.draw(batch);
        planet.draw(batch);
        for (Asteroid asteroid : asteroids) {
            asteroid.draw(batch);
        }
        crosshair.draw(batch);
    }

    @Override
    public String getInstructions() {
        return "Guie o gato no espaço até o planeta!";
    }

    @Override
    public boolean shouldHideMousePointer() {
        return true;
    }

    @Override
    protected void onEnd() {
        backgroundMusic.stop();
        gasNoise.stop();
        world.destroyBody(astroCat.body);
        world.destroyBody(planet.body);
        for (Asteroid asteroid : asteroids) {
            world.destroyBody(asteroid.body);
        }
        for (Body wall : walls) {
            world.destroyBody(wall);
        }
    }

}
