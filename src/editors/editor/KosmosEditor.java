/*
 * Copyright (C) 2017, Equilibrium Games - All Rights Reserved
 *
 * This source file is part of New Kosmos
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package editors.editor;

import flounder.framework.*;
import flounder.logger.*;
import flounder.profiling.*;

public class KosmosEditor extends Module {
	private IEditorType editorType;

	public KosmosEditor() {
		super(FlounderLogger.class, FlounderProfiler.class);
	}

	@Handler.Function(Handler.FLAG_INIT)
	public void init() {
		this.editorType = null;
	}

	@Handler.Function(Handler.FLAG_UPDATE_POST)
	public void update() {
		// Gets a new editor, if available.
		IEditorType newManager = (IEditorType) getExtension(editorType, IEditorType.class, true);

		// If there is a editor, disable the old one and start to use the new one.
		if (newManager != null) {
			if (editorType != null) {
				editorType.dispose();
				editorType.setInitialized(false);
			}

			if (!newManager.isInitialized()) {
				newManager.init();
				newManager.setInitialized(true);
			}

			editorType = newManager;
		}

		// Runs updates for the editor.
		if (editorType != null) {
			editorType.update();
		}
	}

	/**
	 * Gets the current editor extension.
	 *
	 * @return The current editor.
	 */
	public IEditorType getEditorType() {
		return this.editorType;
	}

	@Handler.Function(Handler.FLAG_PROFILE)
	public void profile() {
		if (editorType != null) {
			editorType.profile();
		}

		FlounderProfiler.get().add(getTab(), "Selected", editorType == null ? "NULL" : editorType.getClass());
	}

	@Handler.Function(Handler.FLAG_DISPOSE)
	public void dispose() {
		// Disposes the editor with the module.
		if (editorType != null) {
			editorType.dispose();
			editorType.setInitialized(false);
		}
	}

	@Module.Instance
	public static KosmosEditor get() {
		return (KosmosEditor) Framework.getInstance(KosmosEditor.class);
	}

	@Module.TabName
	public static String getTab() {
		return "Kosmos Editor";
	}
}
