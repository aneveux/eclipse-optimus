/**
 * Optimus, framework for Model Transformation
 *
 * Copyright (C) 2013 Worldline or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.atos.optimus.m2m.engine.core.adapters;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.atos.optimus.m2m.engine.core.Activator;
import net.atos.optimus.m2m.engine.core.logging.EObjectLabelProvider;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * When applied to an EObject, this adapter will log all the modifications that
 * were applied to an it and its children
 * 
 * @author Maxence Vanbésien (mvaawl@gmail.com)
 * @since 1.0
 * 
 */
public class EObjectTrackerAdapter extends AdapterImpl {

	private static final String HANDLER_EXCEPTION = "The handler creation threw an Exception !";

	/**
	 * Pattern for UNSET message
	 */
	private static final String PATTERN_UNSET = "UNSET %s IN %s\n";

	/**
	 * Pattern for SET message
	 */
	private static final String PATTERN_SET = "SET %s AS %s IN %s\n";

	/**
	 * Pattern for REMOVE message
	 */
	private static final String PATTERN_REMOVAL = "REMOVE %s AS %s FROM %s\n";

	/**
	 * Pattern for ADD message
	 */
	private static final String PATTERN_ADDITION = "ADD %s AS %s INTO %s\n";

	/**
	 * True if logs, false if logger is closed
	 */
	private boolean active = false;

	/**
	 * Simple logger formatter instance
	 * 
	 * @author Maxence Vanbésien (mvaawl@gmail.com)
	 * @since 1.0
	 * 
	 */
	private static final class TrackerFormatter extends Formatter {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
		 */
		@Override
		public String format(LogRecord record) {
			return record.getMessage();
		}

	}

	/**
	 * Logger instance
	 */
	private Logger logger = null;

	/**
	 * Label provider used to name elements (EObjects & others)
	 */
	private EObjectLabelProvider labelProvider = new EObjectLabelProvider();

	/**
	 * Handler describing the file the messages will be written into
	 */
	private FileHandler handler = null;

	/**
	 * Creates adapter that will log in the file which path is provided as
	 * parameter. Note that path provided as to be an absolute one
	 * 
	 * @param path
	 */
	public EObjectTrackerAdapter(IPath path) {
		this.logger = Logger.getLogger(EObjectTrackerAdapter.class.getCanonicalName());
		this.logger.setUseParentHandlers(false);
		this.logger.setLevel(Level.INFO);
		try {
			this.handler = new FileHandler(path.toString());
			handler.setFormatter(new TrackerFormatter());
			this.logger.addHandler(handler);
			this.active = true;
		} catch (SecurityException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, HANDLER_EXCEPTION, e));
		} catch (IOException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, HANDLER_EXCEPTION, e));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.emf.common.notify.impl.AdapterImpl#notifyChanged(org.eclipse
	 * .emf.common.notify.Notification)
	 */
	@Override
	public void notifyChanged(Notification msg) {
		if (!active)
			return;
		try {
			if (!(msg.getFeature() instanceof EStructuralFeature) || !(msg.getNotifier() instanceof EObject))
				return;

			EStructuralFeature feature = (EStructuralFeature) msg.getFeature();
			if (feature.isDerived() || !feature.isChangeable())
				return;

			EObject notifier = (EObject) msg.getNotifier();

			switch (msg.getEventType()) {
			case Notification.ADD:
				this.manageAddition(notifier, feature, msg.getNewValue());
				break;
			case Notification.ADD_MANY:
				this.manageMultipleAddition(notifier, feature, msg.getNewValue());
				break;
			case Notification.MOVE:
				this.manageMove(notifier, feature, msg.getNewValue());
				break;
			case Notification.REMOVE:
				this.manageRemoval(notifier, feature, msg.getOldValue());
				break;
			case Notification.REMOVE_MANY:
				this.manageMultipleRemoval(notifier, feature, msg.getOldValue());
				break;
			case Notification.SET:
				this.manageSet(notifier, feature, msg.getNewValue());
				break;
			case Notification.UNSET:
				this.manageUnset(notifier, feature);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Manages when Object is added in EObject
	 * 
	 * @param notifier
	 * @param feature
	 * @param value
	 */
	private void manageAddition(EObject notifier, EStructuralFeature feature, Object value) {
		this.logger.info(String.format(PATTERN_ADDITION, labelProvider.getText(value), feature.getName(),
				labelProvider.getText(notifier)));
		if (feature instanceof EReference && ((EReference) feature).isContainment())
			this.manageDump((EObject) value);
	}

	/**
	 * Dumps all the attributes and references in EObject. Used to dump object
	 * that is built before being adapted
	 * 
	 * @param eObject
	 */
	private void manageDump(EObject eObject) {
		if (eObject == null)
			return;

		eObject.eAdapters().add(this);

		EClass eClass = eObject.eClass();
		for (EStructuralFeature feature : eClass.getEAllStructuralFeatures()) {
			if (!(feature.isDerived()) && feature.isChangeable()) {
				if (feature.isMany()) {
					this.manageMultipleAddition(eObject, feature, eObject.eGet(feature));
				} else {
					Object value = eObject.eGet(feature);
					if (value != null && !value.equals(feature.getDefaultValue()))
						this.manageSet(eObject, feature, value);
				}
			}

			if (feature instanceof EReference && ((EReference) feature).isContainment()) {
				if (feature.isMany()) {
					EList<?> eList = (EList<?>) eObject.eGet(feature);
					for (Object o : eList)
						this.manageDump((EObject) o);
				} else
					this.manageDump((EObject) eObject.eGet(feature));
			}

		}
	}

	/**
	 * Manages addition of multiple objects in EObject
	 * 
	 * @param notifier
	 * @param feature
	 * @param value
	 */
	private void manageMultipleAddition(EObject notifier, EStructuralFeature feature, Object value) {
		EList<?> eList = (EList<?>) value;
		for (Object o : eList)
			this.manageAddition(notifier, feature, o);

	}

	/**
	 * Manage moves in ELists. TODO
	 * 
	 * @param notifier
	 * @param feature
	 * @param value
	 */
	private void manageMove(EObject notifier, EStructuralFeature feature, Object value) {
		// TODO Auto-generated method stub

	}

	/**
	 * Manages removal of Object from EObject
	 * 
	 * @param notifier
	 * @param feature
	 * @param value
	 */
	private void manageRemoval(EObject notifier, EStructuralFeature feature, Object value) {
		this.logger.info(String.format(PATTERN_REMOVAL, labelProvider.getText(value), feature.getName(),
				labelProvider.getText(notifier)));

	}

	/**
	 * Manages multple removal of Object from EObject
	 * 
	 * @param notifier
	 * @param feature
	 * @param value
	 */
	private void manageMultipleRemoval(EObject notifier, EStructuralFeature feature, Object value) {
		EList<?> eList = (EList<?>) value;
		for (Object o : eList)
			this.manageRemoval(notifier, feature, o);

	}

	/**
	 * Manages set of Object in EObject
	 * 
	 * @param notifier
	 * @param feature
	 * @param value
	 */
	private void manageSet(EObject notifier, EStructuralFeature feature, Object value) {
		this.logger.info(String.format(PATTERN_SET, labelProvider.getText(value), feature.getName(),
				labelProvider.getText(notifier)));

	}

	/**
	 * Manages unset of Object from EObject
	 * 
	 * @param notifier
	 * @param feature
	 */
	private void manageUnset(EObject notifier, EStructuralFeature feature) {
		this.logger.info(String.format(PATTERN_UNSET, feature.getName(), labelProvider.getText(notifier)));
	}

	/**
	 * Flushes & closes the internal logger
	 */
	public void dispose() {
		if (!this.active)
			return;
		this.handler.flush();
		this.handler.close();
		this.active = false;
	}

	/**
	 * Returns true if logger is still active. False if it has been disposed.
	 * 
	 * @return
	 */
	public boolean isActive() {
		return active;
	}
}
