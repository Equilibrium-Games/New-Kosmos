/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package kosmos.shadows;

import flounder.camera.*;
import flounder.framework.*;
import flounder.maths.matrices.*;
import flounder.maths.vectors.*;
import flounder.profiling.*;
import flounder.renderer.*;
import kosmos.*;
import kosmos.skybox.*;

public class KosmosShadows extends Module {
	private static final KosmosShadows INSTANCE = new KosmosShadows();
	public static final String PROFILE_TAB_NAME = "Kosmos Shadows";

	private int shadowSize;

	private int shadowPCF;
	private float shadowBias;
	private float shadowDarkness;
	private float shadowTransition;

	private Matrix4f projectionMatrix;
	private Matrix4f lightViewMatrix;
	private Matrix4f projectionViewMatrix;
	private Matrix4f offset;

	private ShadowBox shadowBox;

	public KosmosShadows() {
		super(ModuleUpdate.UPDATE_RENDER, PROFILE_TAB_NAME, FlounderRenderer.class);
	}

	@Override
	public void init() {
		this.shadowSize = KosmosConfigs.SHADOWMAP_SIZE.setReference(() -> shadowSize).getInteger();
		this.shadowPCF = KosmosConfigs.SHADOWMAP_PCF.setReference(() -> shadowPCF).getInteger();
		this.shadowBias = KosmosConfigs.SHADOWMAP_BIAS.setReference(() -> shadowBias).getFloat();
		this.shadowDarkness = KosmosConfigs.SHADOWMAP_DARKNESS.setReference(() -> shadowDarkness).getFloat();
		this.shadowTransition = 11.0f; // TODO: This is a strange setting, but works.

		this.projectionMatrix = new Matrix4f();
		this.lightViewMatrix = new Matrix4f();
		this.projectionViewMatrix = new Matrix4f();
		this.offset = createOffset();

		this.shadowBox = new ShadowBox(lightViewMatrix);
	}

	/**
	 * Creates the orthographic projection matrix.
	 *
	 * @param width Shadow box width.
	 * @param height Shadow box height.
	 * @param length Shadow box length.
	 */
	private void updateOrthographicProjectionMatrix(float width, float height, float length) {
		projectionMatrix.setIdentity();
		projectionMatrix.m00 = 2.0f / width;
		projectionMatrix.m11 = 2.0f / height;
		projectionMatrix.m22 = -2.0f / length;
		projectionMatrix.m33 = 1.0f;
	}

	/**
	 * Updates the "view" matrix of the light. The light itself has no position, so the "view" matrix is centered at the center of the shadow box.
	 *
	 * @param direction The light direct.
	 * @param position The center of the shadow box.
	 */
	private void updateLightViewMatrix(Vector3f direction, Vector3f position) {
		direction.normalize();
		position.negate();

		lightViewMatrix.setIdentity();
		float pitch = (float) Math.acos(new Vector2f(direction.x, direction.z).length());
		Matrix4f.rotate(lightViewMatrix, new Vector3f(1.0f, 0.0f, 0.0f), pitch, lightViewMatrix);
		float yaw = (float) Math.toDegrees(((float) Math.atan(direction.x / direction.z)));
		yaw = direction.z > 0.0f ? yaw - 180.0f : yaw;
		Matrix4f.rotate(lightViewMatrix, new Vector3f(0.0f, 1.0f, 0.0f), (float) -Math.toRadians(yaw), lightViewMatrix);
		Matrix4f.translate(lightViewMatrix, position, lightViewMatrix);
	}

	/**
	 * Create the offset for part of the conversion to shadow map space.
	 *
	 * @return The offset as a matrix.
	 */
	private static Matrix4f createOffset() {
		Matrix4f offset = new Matrix4f();
		Matrix4f.translate(offset, new Vector3f(0.5f, 0.5f, 0.5f), offset);
		Matrix4f.scale(offset, new Vector3f(0.5f, 0.5f, 0.5f), offset);
		return offset;
	}

	@Override
	public void update() {
		shadowBox.update(FlounderCamera.getCamera());
		updateOrthographicProjectionMatrix(shadowBox.getWidth(), shadowBox.getHeight(), shadowBox.getLength());
		updateLightViewMatrix(KosmosSkybox.getLightPosition(), shadowBox.getCenter());
		Matrix4f.multiply(projectionMatrix, lightViewMatrix, projectionViewMatrix);
	}

	@Override
	public void profile() {
		FlounderProfiler.add(PROFILE_TAB_NAME, "Map Size", shadowSize);
		FlounderProfiler.add(PROFILE_TAB_NAME, "PCF Count", shadowPCF);
		FlounderProfiler.add(PROFILE_TAB_NAME, "Surface Bias", shadowBias);
		FlounderProfiler.add(PROFILE_TAB_NAME, "Surface Darkness", shadowDarkness);
	}

	public static int getShadowSize() {
		return INSTANCE.shadowSize;
	}

	public static void setShadowSize(int shadowSize) {
		INSTANCE.shadowSize = shadowSize;
	}

	public static int getShadowPCF() {
		return INSTANCE.shadowPCF;
	}

	public static void setShadowPCF(int shadowPCF) {
		INSTANCE.shadowPCF = shadowPCF;
	}

	public static float getShadowBias() {
		return INSTANCE.shadowBias;
	}

	public static void setShadowBias(float shadowBias) {
		INSTANCE.shadowBias = shadowBias;
	}

	public static float getShadowDarkness() {
		return INSTANCE.shadowDarkness;
	}

	public static void setShadowDarkness(float shadowDarkness) {
		INSTANCE.shadowDarkness = shadowDarkness;
	}

	public static float getShadowDistance() {
		return INSTANCE.shadowBox.getShadowDistance();
	}

	public static float getShadowTransition() {
		return INSTANCE.shadowTransition;
	}

	public static void setShadowTransition(float shadowTransition) {
		INSTANCE.shadowTransition = shadowTransition;
	}

	/**
	 * @return The shadow box, so that it can be used by other class to test if engine.entities are inside the box.
	 */
	public static ShadowBox getShadowBox() {
		return INSTANCE.shadowBox;
	}

	public static Matrix4f getProjectionViewMatrix() {
		return INSTANCE.projectionViewMatrix;
	}

	/**
	 * This biased projection-view matrix is used to convert fragments into "shadow map space" when rendering the main renderObjects pass.
	 *
	 * @return The to-shadow-map-space matrix.
	 */
	public static Matrix4f getToShadowMapSpaceMatrix() {
		return Matrix4f.multiply(INSTANCE.offset, INSTANCE.projectionViewMatrix, null); // TODO
	}

	/**
	 * @return The light's "view" matrix.
	 */
	protected static Matrix4f getLightSpaceTransform() {
		return INSTANCE.lightViewMatrix;
	}

	@Override
	public Module getInstance() {
		return INSTANCE;
	}

	@Override
	public void dispose() {
	}
}
