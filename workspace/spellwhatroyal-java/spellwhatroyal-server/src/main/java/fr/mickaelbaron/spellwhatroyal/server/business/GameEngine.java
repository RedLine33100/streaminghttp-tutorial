package fr.mickaelbaron.spellwhatroyal.server.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import fr.mickaelbaron.spellwhatroyal.api.NotYetImplementException;
import fr.mickaelbaron.spellwhatroyal.api.model.GameState;
import fr.mickaelbaron.spellwhatroyal.server.cfg.IConfigExecution;
import fr.mickaelbaron.spellwhatroyal.server.entity.SpellWhatElement;

/**
 * @author Mickael BARON (baron.mickael@gmail.com)
 */
@ApplicationScoped
public class GameEngine {

	private Runnable preGameRunnable;

	private Runnable inGameRunnable;

	private Runnable postGameRunnable;

	private ScheduledExecutorService executor;

	private Long startState;

	private GameState state = GameState.PRE_GAME;

	private int spellWhatElementCursor;

	private List<SpellWhatElement> spellWhatElements = new ArrayList<>();

	@Inject
	private IConfigExecution configExecution;

	@Inject
	private PlayerEngine refPlayerEngine;

	public void postConstruct(@Observes @Initialized(ApplicationScoped.class) Object o) {
		System.out.println("Starting game.");
	}
	
	private void initData() {
		SpellWhatElement firstElement = new SpellWhatElement();
		firstElement.setText("maison");
		firstElement.setHelp("6 lettres");
		firstElement.setUri(
				"https://www.ouestfrance-immo.com/photo-vente-maison-beuzeville-27/207/maison-a-vendre-beuzeville-14348128_1_1573585794_30cc8ab9991304b562c1be40b66c3c3c_crop_480-360_.jpg");
		spellWhatElements.add(firstElement);
		
		SpellWhatElement secondElement = new SpellWhatElement();
		secondElement.setText("moto");
		secondElement.setHelp("4 lettres");
		secondElement.setUri("https://cdn130.picsart.com/312329717035201.jpg");
		spellWhatElements.add(secondElement);
		
		SpellWhatElement thirdElement = new SpellWhatElement();
		thirdElement.setText("mickaelbaron");
		thirdElement.setHelp("Le compte Twitter @(Prénom + Nom) sans espace");
		thirdElement.setUri("https://mickael-baron.fr/images/mbaron.jpg");
		spellWhatElements.add(thirdElement);
		
		SpellWhatElement fourthElement = new SpellWhatElement();
		fourthElement.setText("casimir");
		fourthElement.setHelp("L'île aux enfants");
		fourthElement.setUri("http://idata.over-blog.com/0/00/74/35/33/casimir.jpg");
		spellWhatElements.add(fourthElement);
		
		SpellWhatElement fifthElement = new SpellWhatElement();
		fifthElement.setText("ensma");
		fifthElement.setHelp("La soucoupe écrasée");
		fifthElement.setUri("https://i0.wp.com/bde.ensma.fr/wp-content/uploads/2019/05/ensmadehaut.jpg");
		spellWhatElements.add(fifthElement);
		
		initSpellWhatElements();			
	}
	
	@PostConstruct
	public void init() {
		this.initData();
		
		executor = Executors.newScheduledThreadPool(1);
		startState = System.currentTimeMillis(); // In case where ...
		
		preGameRunnable = () -> {
			state = GameState.PRE_GAME;
			startState = System.currentTimeMillis();

			// Prepare the game.
			nextSpellWhatElement();

			executor.schedule(inGameRunnable, configExecution.getPreGameDelay(), TimeUnit.SECONDS);
		};

		inGameRunnable = () -> {
			state = GameState.IN_GAME;
			startState = System.currentTimeMillis();

			executor.schedule(postGameRunnable, configExecution.getInGameDelay(), TimeUnit.SECONDS);
		};

		postGameRunnable = () -> {
			state = GameState.POST_GAME;
			startState = System.currentTimeMillis();

			// Compute scores.
			GameEngine.this.refPlayerEngine.computeScore(this.getCurrentSpellWhatElement().getText());

			// is finished ?
			executor.schedule(preGameRunnable, configExecution.getPostGameDelay(), TimeUnit.SECONDS);
		};

		executor.execute(preGameRunnable);
	}

	public GameState getState() {
		return this.state;
	}

	public SpellWhatElement getCurrentSpellWhatElement() {
		SpellWhatElement spellWhatElement = spellWhatElements.get(spellWhatElementCursor);

		return spellWhatElement;
	}

	private void nextSpellWhatElement() {
		spellWhatElementCursor++;

		if (spellWhatElementCursor >= spellWhatElements.size()) {
			initSpellWhatElements();
		}
	}
	
	private void initSpellWhatElements() {
		spellWhatElementCursor = 0;
		Collections.shuffle(spellWhatElements);
	}

	private Integer getGameDelay() {
		switch (state) {
		case PRE_GAME:
			return configExecution.getPreGameDelay();
		case IN_GAME:
			return configExecution.getInGameDelay();
		case POST_GAME:
			return configExecution.getPostGameDelay();
		default:
			throw new NotYetImplementException();
		}
	}

	public Integer getCounter() {
		Long result = (System.currentTimeMillis() - startState) / 1000;
		return Math.max((int) (getGameDelay() - result), 0);
	}
}
