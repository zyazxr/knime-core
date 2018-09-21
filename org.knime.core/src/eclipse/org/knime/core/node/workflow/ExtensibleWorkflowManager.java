/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Sep 19, 2018 (Noemi_Balassa): created
 */
package org.knime.core.node.workflow;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.node.NodeLogger;

/**
 * Extensible class holding a {@link WorkflowManager} and providing greater access to its nodes and connections.
 *
 * @author Noemi Balassa
 * @since 3.7
 */
public class ExtensibleWorkflowManager {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExtensibleWorkflowManager.class);

    private final WorkflowManager m_workflowManager;

    /**
     * Constructs an {@link ExtensibleWorkflowManager}.
     *
     * @param workflowManager the {@link WorkflowManager} object to wrap.
     */
    protected ExtensibleWorkflowManager(final WorkflowManager workflowManager) {
        m_workflowManager = requireNonNull(workflowManager, "workflowManager");
    }

    /**
     * Gets the connections of this (sub) workflow.
     *
     * @return a collection of all {@link ConnectionContainer} objects within this workflow.
     */
    protected Collection<ConnectionContainer> getConnections() {
        final Collection<Set<ConnectionContainer>> groupedConnections =
            m_workflowManager.getWorkflow().getConnectionsBySourceValues();
        if (groupedConnections.isEmpty()) {
            return emptySet();
        }
        int count = 0;
        for (final Set<ConnectionContainer> connections : groupedConnections) {
            count += connections.size();
        }
        if (count == 0) {
            return emptySet();
        }
        final Set<ConnectionContainer> connections = new HashSet<>(count);
        groupedConnections.forEach(connections::addAll);
        assert connections.size() == count;
        return unmodifiableCollection(connections);
    }

    /**
     * Gets the nodes of this (sub) workflow.
     *
     * @return a collection of all {@link NodeContainer} objects within this workflow.
     */
    protected Collection<NodeContainer> getNodes() {
        return m_workflowManager.getWorkflow().getNodeValues();
    }

    /**
     * Gets the wrapped workflow manager.
     *
     * @return a {@link WorkflowManager} object.
     */
    protected WorkflowManager getWorkflowManager() {
        return m_workflowManager;
    }

}
