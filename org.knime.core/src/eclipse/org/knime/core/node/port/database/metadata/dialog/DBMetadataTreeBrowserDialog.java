/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 3, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.dialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.knime.core.node.port.database.metadata.DBMetadataProvider;
import org.knime.core.node.port.database.metadata.filter.selected.SelectedElementFilter;
import org.knime.core.node.port.database.metadata.filter.selection.SelectionFilter;
import org.knime.core.node.port.database.metadata.model.DBObject;

/**
 * The dialog window for the metadata tree browser.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBMetadataTreeBrowserDialog extends JDialog {

    private static final long serialVersionUID = 3508901287156639856L;

    private final DBMetadataTreeBrowser m_panel;

    private DBObject m_selectedObject = null;

    private boolean m_cancelledJob = false;

    private SelectionFilter m_selectionFilter;

    /**
     * Constructor for the metadata tree browser dialog.
     *
     * @param frame the parent frame
     * @param dbmetaProvider the metadata provider
     * @param label the label for the dialog window
     * @param selectionFilter the selection filter
     * @param selElementFilter the selected element filter
     */
    public DBMetadataTreeBrowserDialog(final Frame frame, final DBMetadataProvider dbmetaProvider, final String label,
        final SelectionFilter selectionFilter, final SelectedElementFilter selElementFilter) {
        super(frame);
        m_selectionFilter = selectionFilter;

        setTitle(label);
        setLocationRelativeTo(frame);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
        setAlwaysOnTop(true);

        m_panel = new DBMetadataTreeBrowser(dbmetaProvider, selectionFilter, selElementFilter);
        DBSearchFilterPanel searchFilter = new DBSearchFilterPanel(m_panel);

        add(searchFilter, BorderLayout.NORTH);
        add(m_panel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.setActionCommand("ok");
        okButton.addActionListener(new ButtonListener());
        buttonPanel.add(okButton, BorderLayout.EAST);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(new ButtonListener());
        buttonPanel.add(cancelButton, BorderLayout.WEST);

        add(buttonPanel, BorderLayout.SOUTH);
        pack();

    }

    /**
     * Open the dialog window and update the selected element filter.
     *
     * @param selElementFilter the new selected element filter
     */
    public void open(final SelectedElementFilter selElementFilter) {
        m_panel.updateSelectedObject(selElementFilter);
        m_selectedObject = m_panel.getSelectedObject();
        setVisible(true);

        dispose();
    }

    /**
     * Get the selected object in the tree browser.
     *
     * @return the selected object
     */
    public DBObject getSelectedObject() {
        return m_selectedObject;
    }

    /**
     * Returns whether the previous fetching job is cancelled.
     *
     * @return true if the window is closed during metadata fetching. Return true if fetching is successful
     */
    public boolean isJobCancelled() {
        return m_cancelledJob;
    }

    private void executeOkCommand() {
        DBObject selectedObj = m_panel.getSelectedObject();
        if (selectedObj != null) {
            m_selectedObject = selectedObj;
            closeDialog();
        } else {
            JOptionPane.showMessageDialog(new JDialog(), m_selectionFilter.getErrorMessage(), "Selection not allowed",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeDialog() {
        m_cancelledJob = !m_panel.closeWorker();
        dispose();
    }

    private class ButtonListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (e.getActionCommand().equals("ok")) {
                executeOkCommand();
            } else if (e.getActionCommand().equals("cancel")) {
                closeDialog();
            }

        }
    }

}
