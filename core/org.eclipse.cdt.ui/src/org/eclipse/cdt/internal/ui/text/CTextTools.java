/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     QNX Software System
 *     Anton Leherbauer (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.text;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.text.ICPartitions;

import org.eclipse.cdt.internal.ui.text.util.CColorManager;


/**
 * This type shares all scanners and the color manager between
 * its clients.
 */
public class CTextTools {
	
	private class PreferenceListener implements IPropertyChangeListener, Preferences.IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			adaptToPreferenceChange(event);
		}
        public void propertyChange(Preferences.PropertyChangeEvent event) {
            adaptToPreferenceChange(new PropertyChangeEvent(event.getSource(), event.getProperty(), event.getOldValue(), event.getNewValue()));
        }
	}

	/** The color manager */
	private CColorManager fColorManager;
	/** The C source code scanner */
	private CCodeScanner fCodeScanner;
	/** The C++ source code scanner */
	private CCodeScanner fCppCodeScanner;
	/** The C partitions scanner */
	private FastCPartitionScanner fPartitionScanner;
	/** The C multiline comment scanner */
	private CCommentScanner fMultilineCommentScanner;
	/** The C singleline comment scanner */
	private CCommentScanner fSinglelineCommentScanner;
	/** The C string scanner */
	private SingleTokenCScanner fStringScanner;
	/** The C preprocessor scanner */
	private CPreprocessorScanner fCPreprocessorScanner;
	/** The C++ preprocessor scanner */
	private CPreprocessorScanner fCppPreprocessorScanner;

	/** The preference store */
	private IPreferenceStore fPreferenceStore;
    /** The core preference store */
    private Preferences fCorePreferenceStore;	
	/** The preference change listener */
	private PreferenceListener fPreferenceListener= new PreferenceListener();
	/** The document partitioning used for the C partitioner */
	private String fDocumentPartitioning = ICPartitions.C_PARTITIONING;
	
	/**
	 * Creates a new C text tools collection and eagerly creates 
	 * and initializes all members of this collection.
	 */
    public CTextTools(IPreferenceStore store) {
        this(store, null, true);
    }
    
	/**
	 * Creates a new C text tools collection and eagerly creates 
	 * and initializes all members of this collection.
	 */
    public CTextTools(IPreferenceStore store, Preferences coreStore) {
        this(store, coreStore, true);
    }
    
    /**
     * Creates a new C text tools collection and eagerly creates 
     * and initializes all members of this collection.
     */
	public CTextTools(IPreferenceStore store, Preferences coreStore, boolean autoDisposeOnDisplayDispose) {
		if(store == null) {
			store = CUIPlugin.getDefault().getPreferenceStore();
		}
		fColorManager= new CColorManager(autoDisposeOnDisplayDispose);
		fCodeScanner= new CCodeScanner(fColorManager, store, GCCLanguage.getDefault());
		fCppCodeScanner= new CCodeScanner(fColorManager, store, GPPLanguage.getDefault());
		fPartitionScanner= new FastCPartitionScanner();
		
		fMultilineCommentScanner= new CCommentScanner(fColorManager, store, coreStore, ICColorConstants.C_MULTI_LINE_COMMENT);
		fSinglelineCommentScanner= new CCommentScanner(fColorManager, store, coreStore, ICColorConstants.C_SINGLE_LINE_COMMENT);
		fStringScanner= new SingleTokenCScanner(fColorManager, store, ICColorConstants.C_STRING);
		fCPreprocessorScanner= new CPreprocessorScanner(fColorManager, store, GCCLanguage.getDefault());
		fCppPreprocessorScanner= new CPreprocessorScanner(fColorManager, store, GPPLanguage.getDefault());

		fPreferenceStore = store;
		fPreferenceStore.addPropertyChangeListener(fPreferenceListener);
        
        fCorePreferenceStore= coreStore;
        if (fCorePreferenceStore != null) {
            fCorePreferenceStore.addPropertyChangeListener(fPreferenceListener);
        }
		
	}
	
	/**
	 * Creates a new C text tools collection and eagerly creates 
	 * and initializes all members of this collection.
	 */
	public CTextTools() {
		this((IPreferenceStore)null);
	}
	
	/**
	 * Disposes all members of this tools collection.
	 */
	public void dispose() {
		
		fCodeScanner= null;
		fPartitionScanner= null;
		
		
		fMultilineCommentScanner= null;
		fSinglelineCommentScanner= null;
		fStringScanner= null;
		
		if (fColorManager != null) {
			fColorManager.dispose();
			fColorManager= null;
		}
		
		if (fPreferenceStore != null) {
			fPreferenceStore.removePropertyChangeListener(fPreferenceListener);
			fPreferenceStore= null;
            
            if (fCorePreferenceStore != null) {
                fCorePreferenceStore.removePropertyChangeListener(fPreferenceListener);
                fCorePreferenceStore= null;
            }
            
			fPreferenceListener= null;
		}
	}
	
	/**
	 * Gets the color manager.
	 */
	public CColorManager getColorManager() {
		return fColorManager;
	}
	
	/**
	 * Gets the code scanner used.
	 */
	public RuleBasedScanner getCCodeScanner() {
		return fCodeScanner;
	}
	
	/**
	 * Gets the code scanner used.
	 */
	public RuleBasedScanner getCppCodeScanner() {
		return fCppCodeScanner;
	}
	
	/**
	 * Returns a scanner which is configured to scan 
	 * C-specific partitions, which are multi-line comments,
	 * and regular C source code.
	 *
	 * @return a C partition scanner
	 */
	public IPartitionTokenScanner getPartitionScanner() {
		return fPartitionScanner;
	}
	
	/**
	 * Gets the document provider used.
	 */
	public IDocumentPartitioner createDocumentPartitioner() {
		
		String[] types= new String[] {
			ICPartitions.C_MULTI_LINE_COMMENT,
			ICPartitions.C_SINGLE_LINE_COMMENT,
			ICPartitions.C_STRING,
			ICPartitions.C_CHARACTER,
			ICPartitions.C_PREPROCESSOR
		};
		
		return new FastCPartitioner(getPartitionScanner(), types);
	}
	
	/**
	 * Returns a scanner which is configured to scan C multiline comments.
	 *
	 * @return a C multiline comment scanner
	 */
	public RuleBasedScanner getMultilineCommentScanner() {
		return fMultilineCommentScanner;
	}

	/**
	 * Returns a scanner which is configured to scan C singleline comments.
	 *
	 * @return a C singleline comment scanner
	 */
	public RuleBasedScanner getSinglelineCommentScanner() {
		return fSinglelineCommentScanner;
	}
	
	/**
	 * Returns a scanner which is configured to scan C strings.
	 *
	 * @return a C string scanner
	 */
	public RuleBasedScanner getStringScanner() {
		return fStringScanner;
	}

	/**
	 * Returns a scanner which is configured to scan C preprocessor directives.
	 *
	 * @return a C preprocessor directives scanner
	 */
	public RuleBasedScanner getCPreprocessorScanner() {
		return fCPreprocessorScanner;
	}

	/**
	 * Returns a scanner which is configured to scan C++ preprocessor directives.
	 *
	 * @return a C++ preprocessor directives scanner
	 */
	public RuleBasedScanner getCppPreprocessorScanner() {
		return fCppPreprocessorScanner;
	}
	
	/**
	 * Determines whether the preference change encoded by the given event
	 * changes the behavior of one its contained components.
	 * 
	 * @param event the event to be investigated
	 * @return <code>true</code> if event causes a behavioral change
	 */
	public boolean affectsBehavior(PropertyChangeEvent event) {
		return  fCodeScanner.affectsBehavior(event) ||
					fCppCodeScanner.affectsBehavior(event) ||
					fMultilineCommentScanner.affectsBehavior(event) ||
					fSinglelineCommentScanner.affectsBehavior(event) ||
					fStringScanner.affectsBehavior(event) ||
					fCPreprocessorScanner.affectsBehavior(event);
	}
	
	/**
	 * Adapts the behavior of the contained components to the change
	 * encoded in the given event.
	 * 
	 * @param event the event to whch to adapt
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (fCodeScanner.affectsBehavior(event))
			fCodeScanner.adaptToPreferenceChange(event);
		if (fCppCodeScanner.affectsBehavior(event))
			fCppCodeScanner.adaptToPreferenceChange(event);
		if (fMultilineCommentScanner.affectsBehavior(event))
			fMultilineCommentScanner.adaptToPreferenceChange(event);
		if (fSinglelineCommentScanner.affectsBehavior(event))
			fSinglelineCommentScanner.adaptToPreferenceChange(event);
		if (fStringScanner.affectsBehavior(event))
			fStringScanner.adaptToPreferenceChange(event);
		if (fCPreprocessorScanner.affectsBehavior(event)) {
			fCPreprocessorScanner.adaptToPreferenceChange(event);
			fCppPreprocessorScanner.adaptToPreferenceChange(event);
		}
	}

	/**
	 * Sets up the document partitioner for the given document for the given partitioning.
	 * 
	 * @param document the document to be set up
	 * @param partitioning the document partitioning
	 * @since 3.0
	 */
	public void setupCDocumentPartitioner(IDocument document, String partitioning) {
		IDocumentPartitioner partitioner= createDocumentPartitioner();
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;
			extension3.setDocumentPartitioner(partitioning, partitioner);
		} else {
			document.setDocumentPartitioner(partitioner);
		}
		partitioner.connect(document);
	}
	
	/**
	 * Sets up the given document for the default partitioning.
	 * 
	 * @param document the document to be set up
	 * @since 3.0
	 */
	public void setupCDocument(IDocument document) {
		setupCDocumentPartitioner(document, fDocumentPartitioning);
	}

	/**
	 * Get the document partitioning used for the C partitioner.
	 * 
	 * @return the document partitioning used for the C partitioner
	 * @since 3.1
	 */
	public String getDocumentPartitioning() {
		return fDocumentPartitioning;
	}

	/**
	 * Set the document partitioning to be used for the C partitioner.
	 * 
	 * @since 3.1
	 */
	public void setDocumentPartitioning(String documentPartitioning) {
		fDocumentPartitioning = documentPartitioning;
	}

}
