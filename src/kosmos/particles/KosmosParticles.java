/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package kosmos.particles;

import flounder.devices.*;
import flounder.framework.*;
import flounder.helpers.*;
import flounder.loaders.*;
import flounder.logger.*;
import flounder.maths.vectors.*;
import flounder.profiling.*;
import flounder.resources.*;
import flounder.space.*;
import flounder.textures.*;
import kosmos.particles.loading.*;

import java.lang.ref.*;
import java.util.*;

/**
 * A manager that manages particles.
 */
public class KosmosParticles extends Module {
	private static final KosmosParticles INSTANCE = new KosmosParticles();
	public static final String PROFILE_TAB_NAME = "Kosmos Particles";

	public static final MyFile PARTICLES_FOLDER = new MyFile(MyFile.RES_FOLDER, "particles");
	public static final float MAX_ELAPSED_TIME = 5.0f;

	private Map<String, SoftReference<ParticleTemplate>> loaded;

	private List<ParticleSystem> particleSystems;
	private List<StructureBasic<Particle>> particles;
	private List<Particle> deadParticles;

	/**
	 * Creates a new particle systems manager.
	 */
	public KosmosParticles() {
		super(ModuleUpdate.UPDATE_POST, PROFILE_TAB_NAME, FlounderLogger.class, FlounderProfiler.class, FlounderDisplay.class, FlounderLoader.class, FlounderTextures.class);
	}

	@Override
	public void init() {
		this.loaded = new HashMap<>();

		this.particleSystems = new ArrayList<>();
		this.particles = new ArrayList<>();
		this.deadParticles = new ArrayList<>();
	}

	@Override
	public void update() {
	//	if (FlounderGuis.getGuiMaster().isGamePaused()) {
	//		return;
	//	}

		particleSystems.forEach(ParticleSystem::generateParticles);

		if (!particles.isEmpty()) {
			for (StructureBasic<Particle> list : particles) {
				List<Particle> particles = new ArrayList<>(list.getAll());

				for (Particle particle : particles) {
					particle.update();

					if (!particle.isAlive()) {
						list.remove(particle);
						deadParticles.add(particle);
					}
				}
			}
		}

		if (!deadParticles.isEmpty()) {
			Iterator<Particle> deadIterator = deadParticles.iterator();

			while (deadIterator.hasNext()) {
				Particle particle = deadIterator.next();
				particle.update();

				if (particle.getElapsedTime() > MAX_ELAPSED_TIME) {
					deadIterator.remove();
				}
			}

			ArraySorting.heapSort(deadParticles); // Sorts the list old to new.
		}
	}

	@Override
	public void profile() {
		FlounderProfiler.add(PROFILE_TAB_NAME, "Systems", particleSystems.size());
		FlounderProfiler.add(PROFILE_TAB_NAME, "Types", particles.size());
		FlounderProfiler.add(PROFILE_TAB_NAME, "Dead Particles", deadParticles.size());
	}

	/**
	 * Clears all particles from the scene.
	 */
	public static void clear() {
		INSTANCE.particles.clear();
	}

	/**
	 * Adds a particle system to the recalculateRay loop.
	 *
	 * @param system The new system to add.
	 */
	public static void addSystem(ParticleSystem system) {
		INSTANCE.particleSystems.add(system);
	}

	/**
	 * Removes a particle system from the recalculateRay loop.
	 *
	 * @param system The system to remove.
	 */
	public static void removeSystem(ParticleSystem system) {
		INSTANCE.particleSystems.remove(system);
	}

	/**
	 * Gets a list of all particles.
	 *
	 * @return All particles.
	 */
	protected static List<StructureBasic<Particle>> getParticles() {
		return INSTANCE.particles;
	}

	/**
	 * Adds a particle to the recalculateRay loop.
	 *
	 * @param particleTemplate The particle template to build from.
	 * @param position The particles initial position.
	 * @param velocity The particles initial velocity.
	 * @param lifeLength The particles life length.
	 * @param rotation The particles rotation.
	 * @param scale The particles scale.
	 * @param gravityEffect The particles gravity effect.
	 */
	public static void addParticle(ParticleTemplate particleTemplate, Vector3f position, Vector3f velocity, float lifeLength, float rotation, float scale, float gravityEffect) {
		Particle particle;

		if (INSTANCE.deadParticles.size() > 0) {
			particle = INSTANCE.deadParticles.get(0).set(particleTemplate, position, velocity, lifeLength, rotation, scale, gravityEffect);
			INSTANCE.deadParticles.remove(0);
		} else {
			particle = new Particle(particleTemplate, position, velocity, lifeLength, rotation, scale, gravityEffect);
		}

		for (StructureBasic<Particle> list : INSTANCE.particles) {
			if (list.getSize() > 0 && list.get(0).getParticleTemplate().equals(particle.getParticleTemplate())) {
				list.add(particle);
				return;
			}
		}

		StructureBasic<Particle> list = new StructureBasic<>();
		list.add(particle);
		INSTANCE.particles.add(list);
	}

	@Override
	public Module getInstance() {
		return INSTANCE;
	}

	@Override
	public void dispose() {
		loaded.clear();

		particleSystems.clear();
		particles.clear();
		deadParticles.clear();
	}
}
