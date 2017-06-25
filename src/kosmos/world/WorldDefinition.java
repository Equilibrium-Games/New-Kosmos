/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved.
 *
 * This source file is part of New Kosmos.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package kosmos.world;

import flounder.entities.*;
import flounder.events.*;
import flounder.framework.*;
import flounder.helpers.*;
import flounder.logger.*;
import flounder.maths.*;
import flounder.maths.vectors.*;
import flounder.networking.*;
import flounder.noise.*;
import flounder.resources.*;
import flounder.textures.*;
import kosmos.*;
import kosmos.world.chunks.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

/**
 * A object that defines a worlds generation parameters.
 */
public class WorldDefinition {
	private final String name;
	private final int seed;
	private final int worldSize;
	private final float worldNoiseSpread;
	private final float worldNoiseFrequency;
	private final float worldNoiseHeight;
	private final float worldIslandInside;
	private final float worldIslandOutside;
	private final float worldIslandParameter;

	private final float dayNightCycle;
	private final float dayNightRatio;

	private PerlinNoise noise;
	private TextureObject mapTexture;

	private Map<String, Pair<Vector3f, Vector3f>> players;
	private Map<String, Pair<List<Vector3f>, List<Entity>>> chunkData;

	/**
	 * Creates a new world save definition.
	 *
	 * @param name The name to be used in the save.
	 * @param seed The seed used for the world.
	 * @param worldSize The width and height of the world, in tile size.
	 * @param worldNoiseSpread The width of the islands generated by the noise.
	 * @param worldNoiseFrequency The frequency of roughness applied to the noise (lower is smoother).
	 * @param worldNoiseHeight The height multiplier, max world height.
	 * @param worldIslandInside The inside radius of the island shape.
	 * @param worldIslandOutside The outside radius of the island shape.
	 * @param worldIslandParameter The shape parameter (0=circular, 1=rectangular).
	 * @param dayNightCycle The day/night length (seconds).
	 * @param dayNightRatio The percentage of the time that is day.
	 * @param players The other players position and chunk position data.
	 * @param chunkData The data about all modified chunks in the save.
	 */
	public WorldDefinition(String name, int seed, int worldSize, float worldNoiseSpread, float worldNoiseFrequency, float worldNoiseHeight, float worldIslandInside, float worldIslandOutside, float worldIslandParameter, float dayNightCycle, float dayNightRatio,
	                       Map<String, Pair<Vector3f, Vector3f>> players, Map<String, Pair<List<Vector3f>, List<Entity>>> chunkData) {
		this.name = name;
		this.seed = seed;
		this.worldSize = worldSize;
		this.worldNoiseSpread = worldNoiseSpread;
		this.worldNoiseFrequency = worldNoiseFrequency;
		this.worldNoiseHeight = worldNoiseHeight;
		this.worldIslandInside = worldIslandInside;
		this.worldIslandOutside = worldIslandOutside;
		this.worldIslandParameter = worldIslandParameter;

		this.dayNightCycle = dayNightCycle;
		this.dayNightRatio = dayNightRatio;

		this.noise = new PerlinNoise(seed);
		this.mapTexture = null;

		this.players = players;
		this.chunkData = chunkData;
	}

	public static WorldDefinition load(String name) {
		File saveFile = new File(Framework.get().getRoamingFolder().getPath() + "/saves/" + name + ".save");

		if (!saveFile.exists()) {
			return null;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
			String line;

			String readVersion = "";
			String readName = "";
			int readSeed = 0;
			int readWorldSize = 0;
			float readWorldNoiseSpread = 0.0f;
			float readWorldNoiseFrequency = 0.0f;
			float readWorldNoiseHeight = 0.0f;
			float readWorldIslandInside = 0.0f;
			float readWorldIslandOutside = 0.0f;
			float readWorldIslandParameter = 0.0f;

			float readDayNightCycle = 0.0f;
			float readDayNightRatio = 0.0f;

			Map<String, Pair<Vector3f, Vector3f>> readPlayers = new HashMap<>();
			Map<String, Pair<List<Vector3f>, List<Entity>>> readChunkData = new HashMap<>();

			String section = "null";

			while ((line = br.readLine()) != null) {
				line = line.trim();

				if (line.contains("{")) {
					section = line.replace("{", "").trim();
				} else if (section.equals("save")) {
					line = line.replace(";", "");

					if (line.startsWith("version")) {
						readVersion = line.split("=")[1].trim();
					} else if (line.startsWith("name")) {
						readName = line.split("=")[1].trim();
					} else if (line.startsWith("seed")) {
						readSeed = Integer.parseInt(line.split("=")[1].trim());
					} else if (line.startsWith("worldSize")) {
						readWorldSize = Integer.parseInt(line.split("=")[1].trim());
					} else if (line.startsWith("worldNoiseSpread")) {
						readWorldNoiseSpread = Float.parseFloat(line.split("=")[1].trim());
					} else if (line.startsWith("worldNoiseFrequency")) {
						readWorldNoiseFrequency = Float.parseFloat(line.split("=")[1].trim());
					} else if (line.startsWith("worldNoiseHeight")) {
						readWorldNoiseHeight = Float.parseFloat(line.split("=")[1].trim());
					} else if (line.startsWith("worldIslandInside")) {
						readWorldIslandInside = Float.parseFloat(line.split("=")[1].trim());
					} else if (line.startsWith("worldIslandOutside")) {
						readWorldIslandOutside = Float.parseFloat(line.split("=")[1].trim());
					} else if (line.startsWith("worldIslandParameter")) {
						readWorldIslandParameter = Float.parseFloat(line.split("=")[1].trim());
					} else if (line.startsWith("dayNightCycle")) {
						readDayNightCycle = Float.parseFloat(line.split("=")[1].trim());
					} else if (line.startsWith("dayNightRatio")) {
						readDayNightRatio = Float.parseFloat(line.split("=")[1].trim());
					}
				} else if (section.equals("players")) {
					if (line.contains(";")) {
						line = line.replace(";", "");
						String[] d = line.split(",");

						readPlayers.put(d[0], new Pair<>(
								new Vector3f(Float.parseFloat(d[1]), Float.parseFloat(d[2]), Float.parseFloat(d[3])),
								new Vector3f(Float.parseFloat(d[4]), 0.0f, Float.parseFloat(d[5]))
						));
					}
				} else if (section.equals("chunks")) {
					if (line.contains(";")) {
						line = line.replace(";", "");
						String[] p = line.split("\\]")[0].replace("[", "").replace("]", "").trim().split(",");
						Vector3f position = new Vector3f(Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]));

						String[] r = line.split("\\]")[1].split("\\[")[1].replace("[", "").replace("]", "").trim().split(",");
						List<Vector3f> entitiesRemoved = new ArrayList<>();

						for (int i = 0; i < r.length; i += 3) {
							Vector3f v = new Vector3f(Float.parseFloat(r[i].trim()), Float.parseFloat(r[i + 1].trim()), Float.parseFloat(r[i + 2].trim()));
							entitiesRemoved.add(v);
						}

					//	String[] a = line.split("\\[")[2].split("\\[")[2].replace("[", "").replace("]", "").trim().split(",");
						List<Entity> entitiesAdded = new ArrayList<>();

						if (!readChunkData.containsKey(position)) {
							readChunkData.put(vectorToString(position), new Pair<>(entitiesRemoved, entitiesAdded));
						}
					}
				}
			}

			if (readSeed == 0) {
				FlounderLogger.get().log("Failed to load world: " + name);
				return null;
			}

			FlounderLogger.get().log("Loaded world from New Kosmos v" + readVersion);
			return new WorldDefinition(readName, readSeed, readWorldSize, readWorldNoiseSpread, readWorldNoiseFrequency, readWorldNoiseHeight, readWorldIslandInside, readWorldIslandOutside, readWorldIslandParameter, readDayNightCycle, readDayNightRatio, readPlayers, readChunkData);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Generates a map for the current seed.
	 */
	public void generateMap() {
		// Account for the null seed.
		if (seed == -1) {
			if (mapTexture != null && mapTexture.isLoaded()) {
				mapTexture.delete();
			}

			return;
		}

		FlounderLogger.get().log("Generating map for seed: " + seed);

		BufferedImage imageBiome = new BufferedImage(worldSize, worldSize, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < worldSize; y++) {
			for (int x = 0; x < worldSize; x++) {
				float worldX = (float) x - ((float) worldSize / 2.0f);
				float worldZ = (float) y - ((float) worldSize / 2.0f);

				Colour colourBiome = KosmosChunks.getBiomeMap(worldX, worldZ).getBiome().getColour();
				imageBiome.setRGB(x, y, (((int) (255.0f * colourBiome.r) << 8) + ((int) (255.0f * colourBiome.g)) << 8) + ((int) (255.0f * colourBiome.b)));
			}
		}

		File directorySave = new File(Framework.get().getRoamingFolder().getPath() + "/saves/");

		if (!directorySave.exists()) {
			System.out.println("Creating directory: " + directorySave);

			try {
				directorySave.mkdir();
			} catch (SecurityException e) {
				System.out.println("Filed to create directory: " + directorySave.getPath() + ".");
				e.printStackTrace();
			}
		}

		String clientServer = FlounderNetwork.get().getSocketServer() != null ? "server" : "client";
		File outputBiome = new File(directorySave.getPath() + "/" + seed + "-biome-" + clientServer + ".png");

		try {
			// Save the map texture.
			ImageIO.write(imageBiome, "png", outputBiome);

			// Remove old map texture.
			if (mapTexture != null && mapTexture.isLoaded()) {
				mapTexture.delete();
			}

			// Load the map texture after a few seconds.
			FlounderEvents.get().addEvent(new EventTime(2.5f, false) {
				@Override
				public void onEvent() {
					mapTexture = TextureFactory.newBuilder().setFile(new MyFile(Framework.get().getRoamingFolder(), "saves", seed + "-biome-" + clientServer + ".png")).create();
				}
			});
		} catch (IOException e) {
			FlounderLogger.get().error("Could not save map image to file: " + outputBiome);
			FlounderLogger.get().exception(e);
		}
	}

	/**
	 * The name of this world/save.
	 *
	 * @return The world/save name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the seed used for the world.
	 *
	 * @return The seed.
	 */
	public int getSeed() {
		return seed;
	}

	/**
	 * Gets the width and height of the world, in tile size.
	 *
	 * @return The world size.
	 */
	public int getWorldSize() {
		return worldSize;
	}

	/**
	 * Gets the width of the islands generated by the noise.
	 *
	 * @return The world noise spread.
	 */
	public float getWorldNoiseSpread() {
		return worldNoiseSpread;
	}

	/**
	 * Gets the frequency of roughness applied to the noise (lower is smoother).
	 *
	 * @return The world noise frequency.
	 */
	public float getWorldNoiseFrequency() {
		return worldNoiseFrequency;
	}

	/**
	 * Gets the height multiplier, max world height.
	 *
	 * @return The world noise height.
	 */
	public float getWorldNoiseHeight() {
		return worldNoiseHeight;
	}

	/**
	 * Gets the inside radius of the island shape.
	 *
	 * @return The world island inside bound.
	 */
	public float getWorldIslandInside() {
		return worldIslandInside;
	}

	/**
	 * Gets the outside radius of the island shape.
	 *
	 * @return The world island outside bound.
	 */
	public float getWorldIslandOutside() {
		return worldIslandOutside;
	}

	/**
	 * Gets the shape parameter (0=circular, 1=rectangular).
	 *
	 * @return The world island circle-square parameter.
	 */
	public float getWorldIslandParameter() {
		return worldIslandParameter;
	}

	/**
	 * Gets the day/night length (seconds).
	 *
	 * @return The day night cycle length.
	 */
	public float getDayNightCycle() {
		return dayNightCycle;
	}

	/**
	 * Gets the percentage of the time that is day.
	 *
	 * @return The day night ratio.
	 */
	public float getDayNightRatio() {
		return dayNightRatio;
	}

	public PerlinNoise getNoise() {
		return noise;
	}

	public TextureObject getMapTexture() {
		return mapTexture;
	}

	public Map<String, Pair<Vector3f, Vector3f>> getPlayers() {
		return players;
	}

	public Vector3f getPlayerPosition(String username) {
		if (!players.containsKey(username)) {
			players.put(username, new Pair<>(new Vector3f(), new Vector3f()));
		}

		return players.get(username).getFirst();
	}

	public Vector3f getPlayerChunk(String username) {
		if (!players.containsKey(username)) {
			players.put(username, new Pair<>(new Vector3f(), new Vector3f()));
		}

		return players.get(username).getSecond();
	}

	public Map<String, Pair<List<Vector3f>, List<Entity>>> getChunkData() {
		return chunkData;
	}

	public List<Vector3f> getChunkRemoved(Vector3f position) {
		Pair<List<Vector3f>, List<Entity>> found = chunkData.get(vectorToString(position));

		if (found != null) {
			return found.getFirst();
		}

		return new ArrayList<>();
	}

	public List<Entity> getChunkAdded(Vector3f position) {
		Pair<List<Vector3f>, List<Entity>> found = chunkData.get(vectorToString(position));

		if (found != null) {
			return found.getSecond();
		}

		return new ArrayList<>();
	}

	public void save() {
		if (FlounderNetwork.get().getSocketClient() != null) {
			FlounderLogger.get().log("Cannot save multiplayer world on a client!");
			return;
		}

		FlounderLogger.get().log("Saving world: " + name);

		// Prepares changes in chunks for saving.
		KosmosChunks.get().prepareSave();

		try {
			// The save file and the writers.
			File saveFile = new File(Framework.get().getRoamingFolder().getPath() + "/saves/" + name + ".save");
			saveFile.createNewFile();
			FileWriter fileWriter = new FileWriter(saveFile);
			FileWriterHelper fileWriterHelper = new FileWriterHelper(fileWriter);

			// Date and save info.
			String savedDate = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "." + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "." + Calendar.getInstance().get(Calendar.YEAR) + " - " + Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(Calendar.MINUTE);
			fileWriterHelper.addComment("Automatically generated save file.", "Date generated: " + savedDate);

			// Versioning and seed.
			fileWriterHelper.beginNewSegment("save");
			{
				fileWriterHelper.writeSegmentData("version = " + NewKosmos.VERSION + ";", true);
				fileWriterHelper.writeSegmentData("name = " + name + ";", true);
				fileWriterHelper.writeSegmentData("seed = " + seed + ";", true);
				fileWriterHelper.writeSegmentData("worldSize = " + worldSize + ";", true);
				fileWriterHelper.writeSegmentData("worldNoiseSpread = " + worldNoiseSpread + ";", true);
				fileWriterHelper.writeSegmentData("worldNoiseFrequency = " + worldNoiseFrequency + ";", true);
				fileWriterHelper.writeSegmentData("worldNoiseHeight = " + worldNoiseHeight + ";", true);
				fileWriterHelper.writeSegmentData("worldIslandInside = " + worldIslandInside+ ";", true);
				fileWriterHelper.writeSegmentData("worldIslandOutside = " + worldIslandOutside + ";", true);
				fileWriterHelper.writeSegmentData("worldIslandParameter = " + worldIslandParameter + ";", true);
				fileWriterHelper.writeSegmentData("dayNightCycle = " + dayNightCycle + ";", true);
				fileWriterHelper.writeSegmentData("dayNightRatio = " + dayNightRatio + ";", false);
			}
			fileWriterHelper.endSegment(false);

			// Player data.
			fileWriterHelper.beginNewSegment("players");
			{
				Entity thisPlayer = KosmosWorld.get().getEntityPlayer();
				Chunk thisChunk = KosmosChunks.get().getCurrent();

				if (thisPlayer != null && thisChunk != null) {
					fileWriterHelper.writeSegmentData("this, " + thisPlayer.getPosition().x + ", " + thisPlayer.getPosition().y + ", " + thisPlayer.getPosition().z + ", ");
					fileWriterHelper.writeSegmentData(thisChunk.getPosition().x + ", " + thisChunk.getPosition().z + ";", true);
				}

				for (String username : players.keySet()) {
					if (!username.equals("this")) {
						Pair<Vector3f, Vector3f> data = players.get(username);
						fileWriterHelper.writeSegmentData(username + ", " + data.getFirst().x + ", " + data.getFirst().y + ", " + data.getFirst().z + ", ");
						fileWriterHelper.writeSegmentData(data.getSecond().x + ", " + data.getSecond().z + ";", true);
					}
				}
			}
			fileWriterHelper.endSegment(false);

			// Chunk data.
			fileWriterHelper.beginNewSegment("chunks");
			{
				for (String position : chunkData.keySet()) {
					List<Vector3f> entitiesRemoved = chunkData.get(position).getFirst();
					List<Entity> entitiesAdded = chunkData.get(position).getSecond();

					if (!entitiesRemoved.isEmpty() || !entitiesAdded.isEmpty()) {
						StringBuilder result = new StringBuilder("[" + position + "], [");

						for (Vector3f r : entitiesRemoved) {
							result.append(r.x).append(",").append(r.y).append(",").append(r.z).append(",");
						}

						if (!entitiesRemoved.isEmpty()) {
							result.deleteCharAt(result.length() - 1);
						}

						result.append("], [");

						for (Entity a : entitiesAdded) {
							result.append("\'").append(a.getClass().getName()).append("\', ").append(a.getPosition().x).append(",").append(a.getPosition().y).append(",").append(a.getPosition().z).append(",");
							result.append(a.getRotation().x).append(",").append(a.getRotation().y).append(",").append(a.getRotation().z).append(",");
						}

						if (!entitiesAdded.isEmpty()) {
							result.deleteCharAt(result.length() - 1);
						}

						result.append("];");
						fileWriterHelper.writeSegmentData(result.toString(), true);
					}
				}
			}
			fileWriterHelper.endSegment(true);

			// Closes the file for writing.
			fileWriter.close();
		} catch (IOException e) {
			FlounderLogger.get().exception(e);
		}
	}

	public void dispose() {
		if (mapTexture != null) {
			mapTexture.delete();
		}
	}

	public static String vectorToString(Vector3f vector) {
		return vector.x + "," + vector.y + "," + vector.z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WorldDefinition that = (WorldDefinition) o;

		if (seed != that.seed) return false;
		if (worldSize != that.worldSize) return false;
		if (Float.compare(that.worldNoiseSpread, worldNoiseSpread) != 0) return false;
		if (Float.compare(that.worldNoiseFrequency, worldNoiseFrequency) != 0) return false;
		if (Float.compare(that.worldNoiseHeight, worldNoiseHeight) != 0) return false;
		if (Float.compare(that.worldIslandInside, worldIslandInside) != 0) return false;
		if (Float.compare(that.worldIslandOutside, worldIslandOutside) != 0) return false;
		if (Float.compare(that.worldIslandParameter, worldIslandParameter) != 0) return false;
		if (Float.compare(that.dayNightCycle, dayNightCycle) != 0) return false;
		if (Float.compare(that.dayNightRatio, dayNightRatio) != 0) return false;
		return true;
	}

	@Override
	public String toString() {
		return "WorldDefinition{" +
				"name='" + name + '\'' +
				", seed=" + seed +
				", worldSize=" + worldSize +
				", worldNoiseSpread=" + worldNoiseSpread +
				", worldNoiseFrequency=" + worldNoiseFrequency +
				", worldNoiseHeight=" + worldNoiseHeight +
				", worldIslandInside=" + worldIslandInside +
				", worldIslandOutside=" + worldIslandOutside +
				", worldIslandParameter=" + worldIslandParameter +
				", dayNightCycle=" + dayNightCycle +
				", dayNightRatio=" + dayNightRatio +
				", noise=" + noise +
				", mapTexture=" + mapTexture +
				", players=" + players +
				", chunkData=" + chunkData +
				'}';
	}
}
