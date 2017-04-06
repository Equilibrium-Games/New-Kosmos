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
import flounder.entities.*;
import flounder.fbos.*;
import flounder.helpers.*;
import flounder.maths.matrices.*;
import flounder.maths.vectors.*;
import flounder.physics.*;
import flounder.profiling.*;
import flounder.renderer.*;
import flounder.resources.*;
import flounder.shaders.*;
import kosmos.chunks.*;
import kosmos.entities.components.*;
import org.lwjgl.opengl.*;

public class ShadowRenderer extends Renderer {
	private static final MyFile VERTEX_SHADER = new MyFile(FlounderShaders.SHADERS_LOC, "shadows", "shadowVertex.glsl");
	private static final MyFile FRAGMENT_SHADER = new MyFile(FlounderShaders.SHADERS_LOC, "shadows", "shadowFragment.glsl");

	private FBO shadowFBO;
	private ShaderObject shader;

	private Matrix4f mvpReusableMatrix;

	/**
	 * Creates a new entity renderer.
	 */
	public ShadowRenderer() {
		this.shadowFBO = FBO.newFBO(KosmosShadows.getShadowSize(), KosmosShadows.getShadowSize()).noColourBuffer().disableTextureWrap().depthBuffer(DepthBufferType.TEXTURE).create();
		this.shader = ShaderFactory.newBuilder().setName("shadows").addType(new ShaderType(GL20.GL_VERTEX_SHADER, VERTEX_SHADER)).addType(new ShaderType(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)).create();

		this.mvpReusableMatrix = new Matrix4f();
	}

	@Override
	public void renderObjects(Vector4f clipPlane, Camera camera) {
		if (!shader.isLoaded() || camera == null) {
			return;
		}

		prepareRendering(clipPlane, camera);

		if (FlounderEntities.getEntities() != null) {
			for (Entity entity : FlounderEntities.getEntities().getAll()) {
				renderEntity(entity);
			}
		}

		if (KosmosChunks.getChunks() != null) {
			for (Entity entityc : KosmosChunks.getChunks().getAll()) {
				Chunk chunk = (Chunk) entityc;

				if (chunk.isLoaded()) {
					renderEntity(entityc);
				}
			}
		}

		endRendering();
	}

	private void prepareRendering(Vector4f clipPlane, Camera camera) {
		if (shadowFBO.getWidth() != KosmosShadows.getShadowSize() || shadowFBO.getHeight() != KosmosShadows.getShadowSize()) {
			shadowFBO.setSize(KosmosShadows.getShadowSize(), KosmosShadows.getShadowSize());
		}

		shadowFBO.bindFrameBuffer();
		shader.start();

		OpenGlUtils.prepareNewRenderParse(0.0f, 0.0f, 0.0f);
		OpenGlUtils.antialias(true);
		OpenGlUtils.cullBackFaces(false);
		OpenGlUtils.enableDepthTesting();
	}

	private void renderEntity(Entity entity) {
		if (entity == null) {
			return;
		}

		ComponentModel componentModel = (ComponentModel) entity.getComponent(ComponentModel.class);
		ComponentAnimation componentAnimation = (ComponentAnimation) entity.getComponent(ComponentAnimation.class);
		ComponentSway componentSway = (ComponentSway) entity.getComponent(ComponentSway.class);
		final int vaoLength;

		if (componentModel != null && componentModel.getModel() != null && componentModel.getModel().isLoaded()) {
			OpenGlUtils.bindVAO(componentModel.getModel().getVaoID(), 0);
			shader.getUniformBool("animated").loadBoolean(false);

			if (componentModel.getModelMatrix() != null) {
				Matrix4f.multiply(KosmosShadows.getProjectionViewMatrix(), componentModel.getModelMatrix(), mvpReusableMatrix);
				shader.getUniformMat4("mvpMatrix").loadMat4(mvpReusableMatrix);
			}

			if (componentModel.getModel().getCollider() != null) {
				float height = 0.0f;

				if (componentModel.getModel().getCollider() instanceof AABB) {
					height = ((AABB) componentModel.getModel().getCollider()).getHeight();
				} else if (componentModel.getModel().getCollider() instanceof Sphere) {
					height = 2.0f * ((Sphere) componentModel.getModel().getCollider()).getRadius();
				}

				shader.getUniformFloat("swayHeight").loadFloat(height);
			}

			vaoLength = componentModel.getModel().getVaoLength();
		} else if (componentAnimation != null && componentAnimation.getModel() != null && componentAnimation.getModel().isLoaded()) {
			OpenGlUtils.bindVAO(componentAnimation.getModel().getVaoID(), 0, 4, 5);
			shader.getUniformBool("animated").loadBoolean(true);

			if (componentAnimation.getModelMatrix() != null) {
				Matrix4f.multiply(KosmosShadows.getProjectionViewMatrix(), componentAnimation.getModelMatrix(), mvpReusableMatrix);
				shader.getUniformMat4("mvpMatrix").loadMat4(mvpReusableMatrix);
			}

			// Just stop if you are trying to apply a sway to a animated object, rethink life.
			shader.getUniformFloat("swayHeight").loadFloat(0.0f);
			vaoLength = componentAnimation.getModel().getVaoLength();

			// Loads joint transforms.
			Matrix4f[] jointMatrices = componentAnimation.getJointTransforms();

			for (int i = 0; i < jointMatrices.length; i++) {
				shader.getUniformMat4("jointTransforms[" + i + "]").loadMat4(jointMatrices[i]);
			}
		} else {
			// No model, so no render!
			return;
		}

		if (componentSway != null) {
			shader.getUniformBool("swaying").loadBoolean(true);
			shader.getUniformVec2("swayOffset").loadVec2(componentSway.getSwayOffsetX(), componentSway.getSwayOffsetZ());

			if (componentSway.getTextureSway() != null && componentSway.getTextureSway().isLoaded()) {
				OpenGlUtils.bindTexture(componentSway.getTextureSway(), 1);
			}
		} else {
			shader.getUniformBool("swaying").loadBoolean(false);
		}

		if (vaoLength > 0) {
			OpenGlUtils.renderElements(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, vaoLength);
		}

		OpenGlUtils.unbindVAO(0, 4, 5);
	}

	private void endRendering() {
		shader.stop();
		shadowFBO.unbindFrameBuffer();
	}

	@Override
	public void profile() {
		FlounderProfiler.add("Shadows", "Render Time", super.getRenderTime());
	}

	/**
	 * @return The ID of the shadow map texture.
	 */
	public int getShadowMap() {
		return shadowFBO.getDepthTexture();
	}

	@Override
	public void dispose() {
		shader.delete();
		shadowFBO.delete();
	}
}
