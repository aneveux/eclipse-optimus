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
package net.atos.optimus.m2m.engine.core.exceptions;

import org.eclipse.emf.ecore.EObject;

/**
 * This exception is raised to the caller when a transformation gets uncaught
 * throwable
 * 
 * Information about cause, eObject & transformation id is stored in exception
 * 
 * @author Maxence Vanbésien (mvaawl@gmail.com)
 * @since 1.0
 * 
 */
public class TransformationFailedException extends Exception {

	/**
	 * UID of exception
	 */
	private static final long serialVersionUID = -6726638569277944439L;

	/**
	 * Transformation ID
	 */
	private String transformationID;

	/**
	 * EObject processed when exception was caught
	 */
	private EObject eObject;

	/**
	 * Creates new Exception 
	 */
	public TransformationFailedException(String transformationID, EObject eObject, Throwable cause) {
		this.transformationID = transformationID;
		this.eObject = eObject;
		this.initCause(cause);
	}

	/**
	 * @return transformation id
	 */
	public String getTransformationID() {
		return transformationID;
	}

	/**
	 * @return eObject on which the process failed.
	 * @return
	 */
	public EObject getEObject() {
		return eObject;
	}
}
